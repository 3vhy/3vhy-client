package dev.threevhy.client.gui;

import dev.threevhy.client.Config;
import dev.threevhy.client.Draw;
import dev.threevhy.client.ThreevhyClient;
import dev.threevhy.client.compat.CompatScreen;
import dev.threevhy.client.discord.DiscordPresence;
import dev.threevhy.client.hud.HudWidget;
import dev.threevhy.client.hud.HudWidgets;
import dev.threevhy.client.hud.Theme;
import dev.threevhy.client.module.Module;
import dev.threevhy.client.module.Module.Category;
import dev.threevhy.client.module.Modules;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/**
 * 3vhy Settings, the in-game mod menu (Right Shift). Every feature is a card with its icon and a
 * big enabled/disabled bar; features with extra settings have a gear that opens their page.
 * Category tabs and a search box along the top, the HUD layout editor in the footer.
 */
public final class SettingsScreen extends CompatScreen {
	private static final float[] ZOOM_STEPS = {2f, 3f, 4f, 6f, 8f};
	private static final float[] HUD_SCALES = {0.75f, 1.0f, 1.25f, 1.5f};

	private static final int PANEL = 0xE8081436;
	private static final int FIELD = 0xF0050C26;
	private static final int CARD = 0xD0102360;
	private static final int CARD_HOVER = 0xE81A3480;
	private static final int ON = 0xFF2FBF78;
	private static final int ON_HOVER = 0xFF45D68E;
	private static final int OFF = 0xFFD64560;
	private static final int OFF_HOVER = 0xFFEA5A74;
	private static final int SWITCH_OFF = 0xFF2A3B6E;
	private static final int CARD_H = 74;
	private static final int CARD_MIN_W = 66;
	private static final int GAP = 6;

	private record Hit(int x1, int y1, int x2, int y2, Runnable action) {
		boolean contains(double x, double y) {
			return x >= x1 && x < x2 && y >= y1 && y < y2;
		}
	}

	/** A row on a settings page: a switch, or a value you click to change. */
	private record Option(String name, String description, Supplier<Boolean> on, Supplier<String> value,
			Runnable click) {
	}

	/** A settings page: one module's settings, or 3vhy's general ones. */
	private record Page(String title, ItemStack icon, List<Option> options) {
	}

	private final Screen parent;
	private final List<Hit> hits = new ArrayList<>();
	private final long openedAt = System.currentTimeMillis();
	private Category selected = null; // null = all
	private String search = "";
	private Page page = null;
	private double scroll = 0;
	private double shownScroll = 0;
	private int maxScroll = 0;
	private long lastFrame = 0;
	private String hoverText = null;

	public SettingsScreen(Screen parent) {
		super(Component.literal("3vhy Settings"));
		this.parent = parent;
	}

	// ---------- Pages ----------

	private static boolean hasOptions(Module module) {
		return module == Modules.ZOOM || module == Modules.DISCORD;
	}

	/** Opens a module's settings page. Returns this, so it can be used when creating the screen. */
	public SettingsScreen openOptions(Module module) {
		List<Option> options = new ArrayList<>();
		options.add(new Option("Enabled", module.description, module::enabled, null, module::toggle));
		if (module == Modules.ZOOM) {
			options.add(new Option("Zoom strength", "How far zoom goes in", null,
					() -> trimZero(Config.get().zoomFactor) + "x", () -> {
						Config.get().zoomFactor = next(ZOOM_STEPS, Config.get().zoomFactor);
						Config.saveSoon();
					}));
			options.add(new Option("Smooth camera", "Cinematic camera while zooming",
					() -> Config.get().zoomCinematic, null, () -> {
						Config.get().zoomCinematic = !Config.get().zoomCinematic;
						Config.saveSoon();
					}));
		} else if (module == Modules.DISCORD) {
			options.add(new Option("Show server address", "Your server's address on your Discord profile",
					() -> Config.get().discordShowServer, null, () -> {
						Config.get().discordShowServer = !Config.get().discordShowServer;
						Config.saveSoon();
					}));
		}
		page = new Page(module.name, ModuleIcons.of(module), options);
		return this;
	}

	/** Opens the general settings page (the gear next to the search box). */
	public SettingsScreen openGeneral() {
		List<Option> options = new ArrayList<>();
		options.add(new Option("HUD size", "Makes every HUD widget bigger or smaller", null,
				() -> Math.round(Config.get().hudScale * 100) + "%", () -> {
					Config.get().hudScale = next(HUD_SCALES, Config.get().hudScale);
					Config.saveSoon();
				}));
		options.add(new Option("Reset HUD layout", "Puts every HUD widget back in its spot", null,
				() -> "Reset", () -> {
					for (HudWidget widget : HudWidgets.ALL) widget.resetPosition();
				}));
		options.add(new Option("3vhy main menu", Modules.MAIN_MENU.description, Modules.MAIN_MENU::enabled, null,
				Modules.MAIN_MENU::toggle));
		page = new Page("3vhy Settings", null, options);
		return this;
	}

	private static float next(float[] steps, float current) {
		for (float step : steps) {
			if (step > current + 0.01f) return step;
		}
		return steps[0];
	}

	private static String trimZero(float value) {
		return value == Math.floor(value) ? Integer.toString((int) value) : Float.toString(value);
	}

	private List<Module> shownModules() {
		String query = search.trim().toLowerCase(Locale.ROOT);
		List<Module> shown = new ArrayList<>();
		for (Module module : Modules.ALL) {
			if (selected != null && module.category != selected) continue;
			if (!query.isEmpty() && !module.name.toLowerCase(Locale.ROOT).contains(query)
					&& !module.description.toLowerCase(Locale.ROOT).contains(query)) {
				continue;
			}
			shown.add(module);
		}
		return shown;
	}

	// ---------- Drawing ----------

	@Override
	protected void drawBackdrop(Draw d) {
		if (Minecraft.getInstance().level == null) {
			MainMenuScreen.drawBackdrop(d, this.width, this.height);
			d.fill(0, 0, this.width, this.height, 0x70040A20);
		} else {
			d.fill(0, 0, this.width, this.height, 0x60040A20);
		}
	}

	@Override
	protected void draw(Draw d, int mouseX, int mouseY, float delta) {
		Minecraft mc = Minecraft.getInstance();
		hits.clear();
		hoverText = null;
		long now = System.nanoTime();
		double seconds = lastFrame == 0 ? 0 : Math.min(0.1, (now - lastFrame) / 1_000_000_000.0);
		lastFrame = now;
		shownScroll += (scroll - shownScroll) * Math.min(1.0, seconds * 16.0);
		if (Math.abs(scroll - shownScroll) < 0.5) shownScroll = scroll;

		int pw = Math.min(this.width - 16, 500);
		int ph = Math.min(this.height - 16, 300);
		int px = (this.width - pw) / 2;
		int py = (this.height - ph) / 2;

		float intro = Math.min(1f, (System.currentTimeMillis() - openedAt) / 180f);
		float slide = (1 - easeOut(intro)) * 8;
		d.push();
		d.translate(0, slide);

		// Panel with a soft shadow
		d.roundRect(px - 2, py + 1, px + pw + 2, py + ph + 4, 8, 0x40000000);
		d.roundRect(px, py, px + pw, py + ph, 6, PANEL);
		d.roundOutline(px, py, px + pw, py + ph, 6, 0x385BC8E6);

		drawTopBar(d, mc, px, py, pw, mouseX, mouseY);
		d.fill(px + 10, py + 32, px + pw - 10, py + 33, 0x205BC8E6);

		int cx = px + 10;
		int cy = py + 39;
		int cw = pw - 20;
		int ch = ph - 39 - 30;
		if (page != null) {
			drawPage(d, mc, cx, cy, cw, ch, mouseX, mouseY);
		} else {
			drawGrid(d, mc, cx, cy, cw, ch, mouseX, mouseY);
		}

		drawFooter(d, mc, px, py + ph - 26, pw, mouseX, mouseY);
		d.pop();
	}

	private void drawTopBar(Draw d, Minecraft mc, int px, int py, int pw, int mx, int my) {
		int y = py + 9;
		int x = px + 11;
		Wordmark.draw(d, x, y, 1, Theme.ICE, 0xFF061D8C);
		x += Wordmark.COLUMNS + 7;

		int searchW = Math.max(74, Math.min(124, pw / 4));
		int searchX = px + pw - 10 - searchW;
		int gearX = searchX - 5 - 16;

		List<String> labels = new ArrayList<>();
		labels.add("ALL");
		for (Category category : Category.values()) labels.add(category.label.toUpperCase(Locale.ROOT));
		int tabsW = -3;
		for (String label : labels) tabsW += mc.font.width(label) + 14 + 3;
		String client = "CLIENT";
		int clientW = mc.font.width(client) + 10;
		if (x + clientW + tabsW <= gearX - 8) {
			d.text(client, x, y + 4, Theme.CYAN, false);
			x += clientW;
		}

		for (int i = 0; i < labels.size(); i++) {
			Category category = i == 0 ? null : Category.values()[i - 1];
			String label = labels.get(i);
			int tw = mc.font.width(label) + 14;
			boolean active = page == null && selected == category;
			boolean hover = inside(mx, my, x, y, x + tw, y + 16);
			if (active) {
				d.roundRect(x, y, x + tw, y + 16, 4, Theme.CYAN);
			} else if (hover) {
				d.roundRect(x, y, x + tw, y + 16, 4, 0x305BC8E6);
			}
			d.text(label, x + 7, y + 4, active ? Theme.NIGHT : hover ? Theme.ICE : Theme.SKY, false);
			hits.add(new Hit(x, y, x + tw, y + 16, () -> select(category)));
			x += tw + 3;
		}

		// General settings
		boolean generalOpen = page != null && page.icon() == null;
		boolean gearHover = inside(mx, my, gearX, y, gearX + 16, y + 16);
		d.roundRect(gearX, y, gearX + 16, y + 16, 4, generalOpen ? Theme.CYAN : gearHover ? 0x305BC8E6 : FIELD);
		Icons.draw(d, Icons.GEAR, gearX + 5, y + 5 - 1, generalOpen ? Theme.NIGHT : gearHover ? Theme.ICE : Theme.SKY);
		hits.add(new Hit(gearX, y, gearX + 16, y + 16, this::openGeneral));
		if (gearHover) hoverText = "3vhy settings: HUD size, layout and main menu";

		// Search: just start typing
		d.roundRect(searchX, y, searchX + searchW, y + 16, 4, FIELD);
		d.roundOutline(searchX, y, searchX + searchW, y + 16, 4, search.isEmpty() ? 0x285BC8E6 : 0x905BC8E6);
		Icons.draw(d, Icons.SEARCH, searchX + 5, y + 4, search.isEmpty() ? Theme.MUTED : Theme.CYAN);
		int textX = searchX + 16;
		int textMax = searchW - 16 - 14;
		if (search.isEmpty()) {
			d.text("Search mods", textX, y + 4, Theme.MUTED, false);
		} else {
			String shown = search;
			while (!shown.isEmpty() && mc.font.width(shown) > textMax) shown = shown.substring(1);
			d.text(shown, textX, y + 4, Theme.ICE, false);
			if ((System.currentTimeMillis() / 500) % 2 == 0) {
				int caret = textX + mc.font.width(shown) + 1;
				d.fill(caret, y + 3, caret + 1, y + 13, Theme.ICE);
			}
			int clearX = searchX + searchW - 12;
			boolean clearHover = inside(mx, my, clearX - 2, y, clearX + 9, y + 16);
			Icons.draw(d, Icons.CLOSE, clearX, y + 4, clearHover ? Theme.ICE : Theme.MUTED);
			hits.add(new Hit(clearX - 2, y, clearX + 9, y + 16, () -> setSearch("")));
		}
	}

	private void drawGrid(Draw d, Minecraft mc, int cx, int cy, int cw, int ch, int mx, int my) {
		List<Module> shown = shownModules();
		int gridW = cw - 6;
		int columns = Math.max(2, (gridW + GAP) / (CARD_MIN_W + GAP));
		int cardW = (gridW - GAP * (columns - 1)) / columns;
		int rows = (shown.size() + columns - 1) / columns;
		int contentH = Math.max(0, rows * (CARD_H + GAP) - GAP);
		maxScroll = Math.max(0, contentH - ch);
		scroll = clamp(scroll, 0, maxScroll);
		shownScroll = clamp(shownScroll, 0, maxScroll);

		if (shown.isEmpty()) {
			String none = "No mods match \"" + search + "\"";
			d.text(none, cx + (cw - mc.font.width(none)) / 2, cy + ch / 2 - 4, Theme.MUTED, false);
			return;
		}

		d.enableScissor(cx, cy, cx + cw, cy + ch);
		for (int i = 0; i < shown.size(); i++) {
			int x = cx + (i % columns) * (cardW + GAP);
			int y = cy + (i / columns) * (CARD_H + GAP) - (int) Math.round(shownScroll);
			if (y + CARD_H < cy || y > cy + ch) continue;
			card(d, mc, shown.get(i), x, y, cardW, mx, my, cy, cy + ch);
		}
		d.disableScissor();

		if (maxScroll > 0) {
			int trackX = cx + cw - 3;
			d.roundRect(trackX, cy, trackX + 3, cy + ch, 1, 0x205BC8E6);
			int thumbH = Math.max(16, ch * ch / Math.max(1, contentH));
			int thumbY = cy + (int) Math.round((ch - thumbH) * (shownScroll / maxScroll));
			d.roundRect(trackX, thumbY, trackX + 3, thumbY + thumbH, 1, 0xA05BC8E6);
		}
	}

	private void card(Draw d, Minecraft mc, Module module, int x, int y, int w, int mx, int my, int clipTop,
			int clipBottom) {
		int h = CARD_H;
		boolean visible = my >= clipTop && my < clipBottom;
		boolean hover = visible && inside(mx, my, x, y, x + w, y + h);
		boolean on = module.enabled();
		if (hover) hoverText = module.name + ": " + module.description;

		d.roundRect(x, y, x + w, y + h, 4, hover ? CARD_HOVER : CARD);
		d.roundOutline(x, y, x + w, y + h, 4, hover ? 0x705BC8E6 : 0x185BC8E6);

		// Icon, 1.5x
		d.push();
		d.translate(x + (w - 24) / 2f, y + 8);
		d.scale(1.5f);
		d.item(ModuleIcons.of(module), 0, 0);
		d.pop();

		drawName(d, mc, module.name, x + w / 2, y + 36, w - 8);

		// The big enabled / disabled bar
		int bx1 = x + 5;
		int bx2 = x + w - 5;
		int by1 = y + h - 19;
		int by2 = y + h - 5;
		boolean barHover = hover && my >= by1 - 2;
		d.roundRect(bx1, by1, bx2, by2, 3, on ? (barHover ? ON_HOVER : ON) : (barHover ? OFF_HOVER : OFF));
		String state = on ? "ENABLED" : "DISABLED";
		d.text(state, x + (w - mc.font.width(state)) / 2, by1 + 3, 0xFFFFFFFF, false);

		int top = Math.max(y, clipTop);
		int bottom = Math.min(y + h, clipBottom);
		if (hasOptions(module)) {
			int gx = x + w - 15;
			int gy = y + 4;
			boolean gearHover = visible && inside(mx, my, gx, gy, gx + 11, gy + 11);
			if (gearHover) d.roundRect(gx, gy, gx + 11, gy + 11, 3, 0x405BC8E6);
			Icons.draw(d, Icons.GEAR, gx + 2, gy + 2, gearHover ? Theme.ICE : Theme.MUTED);
			if (gearHover) hoverText = module.name + " settings";
			if (gy + 11 > top && gy < bottom) {
				hits.add(new Hit(gx, Math.max(gy, top), gx + 11, Math.min(gy + 11, bottom), () -> openOptions(module)));
			}
		}
		if (bottom > top) hits.add(new Hit(x, top, x + w, bottom, module::toggle));
	}

	/** A card's name: one line if it fits, otherwise two lines, smaller if needed. */
	private static void drawName(Draw d, Minecraft mc, String name, int centerX, int y, int maxW) {
		if (mc.font.width(name) <= maxW) {
			d.text(name, centerX - mc.font.width(name) / 2, y + 4, Theme.ICE, false);
			return;
		}
		int space = name.indexOf(' ');
		if (space < 0) {
			drawScaled(d, mc, name, centerX, y + 5, maxW);
			return;
		}
		// Split at the space that keeps both lines shortest
		String first = name.substring(0, space);
		String second = name.substring(space + 1);
		int best = Math.max(mc.font.width(first), mc.font.width(second));
		for (int i = space + 1; i < name.length(); i++) {
			if (name.charAt(i) != ' ') continue;
			String a = name.substring(0, i);
			String b = name.substring(i + 1);
			int width = Math.max(mc.font.width(a), mc.font.width(b));
			if (width < best) {
				best = width;
				first = a;
				second = b;
			}
		}
		if (best <= maxW) {
			d.text(first, centerX - mc.font.width(first) / 2, y, Theme.ICE, false);
			d.text(second, centerX - mc.font.width(second) / 2, y + 9, Theme.ICE, false);
		} else {
			drawScaled(d, mc, first, centerX, y + 1, maxW);
			drawScaled(d, mc, second, centerX, y + 9, maxW);
		}
	}

	private static void drawScaled(Draw d, Minecraft mc, String text, int centerX, int y, int maxW) {
		float scale = Math.max(0.6f, Math.min(1f, maxW / (float) mc.font.width(text)));
		d.push();
		d.translate(centerX - mc.font.width(text) * scale / 2f, y);
		d.scale(scale);
		d.text(text, 0, 0, Theme.ICE, false);
		d.pop();
	}

	private void drawPage(Draw d, Minecraft mc, int cx, int cy, int cw, int ch, int mx, int my) {
		maxScroll = 0;
		// Back button and title
		int backW = mc.font.width("Back") + 22;
		boolean backHover = inside(mx, my, cx, cy, cx + backW, cy + 16);
		d.roundRect(cx, cy, cx + backW, cy + 16, 4, backHover ? 0x405BC8E6 : FIELD);
		Icons.draw(d, Icons.BACK, cx + 6, cy + 4, backHover ? Theme.ICE : Theme.SKY);
		d.text("Back", cx + 16, cy + 4, backHover ? Theme.ICE : Theme.SKY, false);
		hits.add(new Hit(cx, cy, cx + backW, cy + 16, this::closePage));

		int tx = cx + backW + 8;
		if (page.icon() != null) {
			d.item(page.icon(), tx, cy);
			tx += 20;
		}
		d.text(page.title(), tx, cy + 4, Theme.ICE, false);

		int y = cy + 24;
		int rowH = 30;
		for (Option option : page.options()) {
			if (y + rowH > cy + ch) break;
			boolean hover = inside(mx, my, cx, y, cx + cw, y + rowH);
			d.roundRect(cx, y, cx + cw, y + rowH, 4, hover ? CARD_HOVER : CARD);
			d.roundOutline(cx, y, cx + cw, y + rowH, 4, hover ? 0x705BC8E6 : 0x185BC8E6);
			d.text(option.name(), cx + 10, y + 6, Theme.ICE, false);
			d.push();
			d.translate(cx + 10, y + 18);
			d.scale(0.8f);
			d.text(trim(mc, option.description(), Math.round((cw - 90) / 0.8f)), 0, 0, Theme.MUTED, false);
			d.pop();

			if (option.value() != null) {
				String value = option.value().get();
				int vw = mc.font.width(value) + 22;
				int vx = cx + cw - 10 - vw;
				int vy = y + 7;
				d.roundRect(vx, vy, vx + vw, vy + 16, 4, hover ? 0xFF2A4C9E : 0xFF1C3A82);
				d.text(value, vx + 8, vy + 4, Theme.ICE, false);
				Icons.draw(d, new String[] {"#..", ".#.", "..#", ".#.", "#.."}, vx + vw - 10, vy + 5, Theme.CYAN);
			} else {
				boolean on = option.on().get();
				int sw = 26;
				int sx = cx + cw - 10 - sw;
				int sy = y + 9;
				d.roundRect(sx, sy, sx + sw, sy + 12, 6, on ? ON : SWITCH_OFF);
				int knob = on ? sx + sw - 11 : sx + 1;
				d.roundRect(knob, sy + 1, knob + 10, sy + 11, 5, on ? 0xFFFFFFFF : Theme.SKY);
			}
			hits.add(new Hit(cx, y, cx + cw, y + rowH, option.click()));
			y += rowH + 5;
		}
	}

	private void drawFooter(Draw d, Minecraft mc, int px, int y, int pw, int mx, int my) {
		int doneW = 62;
		int doneX = px + pw - 10 - doneW;
		button(d, mc, doneX, y, doneW, 18, "Done", true, mx, my, this::onClose);
		int editW = mc.font.width("Edit HUD layout") + 26;
		int editX = doneX - 6 - editW;
		button(d, mc, editX, y, editW, 18, "Edit HUD layout", false, mx, my,
				() -> Minecraft.getInstance().setScreen(new HudEditorScreen(this)));
		Icons.draw(d, Icons.LAYOUT, editX + 7, y + 6 - 1, Theme.SKY);

		String text = hoverText;
		int color = Theme.SKY;
		if (text == null) {
			text = DiscordPresence.statusLine();
			color = Theme.MUTED;
		}
		if (text == null) {
			text = "3vhy Client " + ThreevhyClient.version() + "  ·  Right Shift opens this menu";
			color = Theme.MUTED;
		}
		d.text(trim(mc, text, editX - 10 - (px + 11)), px + 11, y + 5, color, false);
	}

	private void button(Draw d, Minecraft mc, int x, int y, int w, int h, String label, boolean primary, int mx,
			int my, Runnable action) {
		boolean hover = inside(mx, my, x, y, x + w, y + h);
		int bg = primary ? (hover ? 0xFF8AE0F5 : Theme.CYAN) : (hover ? 0xFF22438F : 0xFF16306E);
		d.roundRect(x, y, x + w, y + h, 4, bg);
		if (!primary) d.roundOutline(x, y, x + w, y + h, 4, hover ? 0x905BC8E6 : 0x305BC8E6);
		int tw = mc.font.width(label);
		int textX = primary ? x + (w - tw) / 2 : x + w - 8 - tw;
		d.text(label, textX, y + (h - 8) / 2, primary ? Theme.NIGHT : Theme.ICE, false);
		hits.add(new Hit(x, y, x + w, y + h, action));
	}

	private static String trim(Minecraft mc, String text, int maxWidth) {
		if (maxWidth <= 0) return "";
		if (mc.font.width(text) <= maxWidth) return text;
		String ellipsis = "...";
		String t = text;
		while (!t.isEmpty() && mc.font.width(t + ellipsis) > maxWidth) {
			t = t.substring(0, t.length() - 1);
		}
		return t + ellipsis;
	}

	private static boolean inside(double mx, double my, int x1, int y1, int x2, int y2) {
		return mx >= x1 && mx < x2 && my >= y1 && my < y2;
	}

	private static double clamp(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}

	private static float easeOut(float t) {
		return 1 - (1 - t) * (1 - t) * (1 - t);
	}

	// ---------- State ----------

	private void select(Category category) {
		selected = category;
		page = null;
		scroll = 0;
		shownScroll = 0;
	}

	private void closePage() {
		page = null;
	}

	private void setSearch(String value) {
		search = value;
		page = null;
		scroll = 0;
		shownScroll = 0;
	}

	// ---------- Input ----------

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == 0) {
			for (Hit hit : List.copyOf(hits)) {
				if (hit.contains(event.x(), event.y())) {
					Minecraft.getInstance().getSoundManager().play(
							SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
					hit.action().run();
					return true;
				}
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
		scroll = clamp(scroll - vertical * 28, 0, maxScroll);
		return true;
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		String typed = event.codepointAsString();
		if (typed.isEmpty() || typed.codePointAt(0) < 32 || typed.codePointAt(0) == 127) return false;
		if (search.length() < 32) setSearch(search + typed);
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int key = event.key();
		if (key == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
			setSearch(search.substring(0, search.length() - 1));
			return true;
		}
		if (key == GLFW.GLFW_KEY_ESCAPE) {
			if (page != null) {
				closePage();
				return true;
			}
			if (!search.isEmpty()) {
				setSearch("");
				return true;
			}
		}
		// The menu key closes the menu again, unless the player is typing a search
		if (search.isEmpty() && ThreevhyClient.openSettingsKey != null && ThreevhyClient.openSettingsKey.matches(event)) {
			onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void onClose() {
		Config.save();
		Minecraft.getInstance().setScreen(parent);
	}
}
