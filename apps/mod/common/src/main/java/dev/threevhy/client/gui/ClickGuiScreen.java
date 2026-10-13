package dev.threevhy.client.gui;

import dev.threevhy.client.Config;
import dev.threevhy.client.Draw;
import dev.threevhy.client.ThreevhyClient;
import dev.threevhy.client.compat.CompatScreen;
import dev.threevhy.client.hud.HudWidgets;
import dev.threevhy.client.module.Module;
import dev.threevhy.client.module.Module.Category;
import dev.threevhy.client.module.Modules;
import dev.threevhy.client.module.Setting;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

/**
 * The mod menu's Panels style, like a PvP client's click GUI: a floating panel for each category and
 * a search bar on top. Left click a mod to switch it on or off, right click to open its settings
 * right under it. Panels are moved by dragging their title and folded with the arrow; where they
 * are and which are folded is saved.
 */
public final class ClickGuiScreen extends CompatScreen {
	private static final int PANEL_W = 116;
	private static final int HEADER_H = 18;
	private static final int ROW_H = 14;
	private static final int RADIUS = 5;
	private static final String CLIENT = "client";
	/** Mods whose extra options (lists, files, status) only fit the Classic page. */
	private static final Set<Module> MORE_OPTIONS = Set.of(Modules.PLAYER_LIST, Modules.NAME_PROTECT, Modules.SKIN_PROTECT,
			Modules.MUSIC, Modules.THEME);

	private record ClientAction(String label, Runnable action) {
	}

	private final Screen parent;
	private final List<Ui.Hit> hits = Ui.newHits();
	private final Snow snow = new Snow();
	private final long openedAt = System.currentTimeMillis();
	private final Set<Module> expanded = new HashSet<>();
	private final Map<String, Double> scrolls = new HashMap<>();
	/** Each panel's box this frame, {x1, y1, x2, y2}, for scrolling and dragging. */
	private final Map<String, int[]> boxes = new HashMap<>();

	private String search = "";
	private Setting.Text editingText = null;
	private String dragging = null;
	private int grabX;
	private int grabY;
	private Setting.Slider draggingSlider = null;
	private int sliderX;
	private int sliderW = 1;
	private String status = null;

	public ClickGuiScreen(Screen parent) {
		super(Component.literal("3vhy Mod Menu"));
		this.parent = parent;
	}

	private static List<String> panelKeys() {
		List<String> keys = new ArrayList<>();
		for (Category category : Category.values()) keys.add(category.name().toLowerCase(Locale.ROOT));
		keys.add(CLIENT);
		return keys;
	}

	private static Category category(String key) {
		for (Category category : Category.values()) {
			if (category.name().toLowerCase(Locale.ROOT).equals(key)) return category;
		}
		return null;
	}

	private static String title(String key) {
		Category category = category(key);
		return category == null ? "CLIENT" : category.label.toUpperCase(Locale.ROOT);
	}

	private static String[] icon(String key) {
		Category category = category(key);
		if (category == null) return PixelIcons.SNOWFLAKE;
		return switch (category) {
			case HUD -> PixelIcons.LAYOUT;
			case PVP -> PixelIcons.forModule(Modules.CRYSTAL_OPTIMIZER);
			case VISUAL -> PixelIcons.EYE;
			case UTILITY -> PixelIcons.GEAR;
		};
	}

	/** A panel's saved spot {x, y, folded}; panels without one are laid out first. */
	private int[] spot(String key) {
		int[] saved = Config.get().panels.get(key);
		if (saved != null) return saved;
		layOutNewPanels();
		return Config.get().panels.get(key);
	}

	/**
	 * Gives every panel without a spot one: as many columns as fit, centred, each panel going into
	 * the column that's shortest so far, so short panels stack under each other.
	 */
	private void layOutNewPanels() {
		List<String> keys = panelKeys();
		int columns = Math.max(1, Math.min(keys.size(), (this.width - 8) / (PANEL_W + 8)));
		int left = Math.max(4, (this.width - columns * (PANEL_W + 8) + 8) / 2);
		int[] bottoms = new int[columns];
		java.util.Arrays.fill(bottoms, 30);
		for (String key : keys) {
			int column = 0;
			for (int i = 1; i < columns; i++) if (bottoms[i] < bottoms[column]) column = i;
			int[] spot = Config.get().panels.get(key);
			if (spot == null) {
				spot = new int[] {left + column * (PANEL_W + 8), bottoms[column], 0};
				Config.get().panels.put(key, spot);
			}
			bottoms[column] = spot[1] + HEADER_H + Math.min(defaultHeight(key), 200) + 8;
		}
	}

	private int defaultHeight(String key) {
		return (CLIENT.equals(key) ? clientActions().size() : modules(key).size()) * ROW_H + 6;
	}

	private boolean matches(Module module) {
		String query = search.trim().toLowerCase(Locale.ROOT);
		return query.isEmpty() || module.name.toLowerCase(Locale.ROOT).contains(query)
				|| module.description.toLowerCase(Locale.ROOT).contains(query);
	}

	private List<Module> modules(String key) {
		Category category = category(key);
		List<Module> list = new ArrayList<>();
		if (category == null) return list;
		for (Module module : Modules.ALL) {
			if (module.category == category && matches(module)) list.add(module);
		}
		return list;
	}

	private List<ClientAction> clientActions() {
		Minecraft mc = Minecraft.getInstance();
		List<ClientAction> actions = new ArrayList<>(List.of(
				new ClientAction("Edit HUD", () -> mc.setScreen(new HubScreen(this))),
				new ClientAction("Profiles", () -> mc.setScreen(new ModMenuScreen(this).openTab(ModMenuScreen.Tab.PROFILES))),
				new ClientAction("Waypoints", () -> mc.setScreen(new ModMenuScreen(this).openTab(ModMenuScreen.Tab.WAYPOINTS))),
				new ClientAction("Classic menu", () -> {
					Modules.THEME_MENU_STYLE.set("Classic");
					mc.setScreen(new ModMenuScreen(parent));
				}),
				new ClientAction("Minecraft options", () -> mc.setScreen(dev.threevhy.client.compat.Compat.optionsScreen(this)))));
		String query = search.trim().toLowerCase(Locale.ROOT);
		if (!query.isEmpty()) actions.removeIf(each -> !each.label().toLowerCase(Locale.ROOT).contains(query));
		return actions;
	}

	@Override
	protected void drawBackdrop(Draw d) {
		MenuTheme t = MenuTheme.current();
		if (Minecraft.getInstance().level == null) MainMenuScreen.drawBackdrop(d, this.width, this.height);
		d.fill(0, 0, this.width, this.height, t.tint());
		snow.draw(d, this.width, this.height);
	}

	@Override
	protected void draw(Draw d, int mouseX, int mouseY, float delta) {
		hits.clear();
		boxes.clear();
		status = null;
		Ui ui = new Ui(d, mouseX, mouseY, hits);
		float intro = Math.min(1f, (System.currentTimeMillis() - openedAt) / 200f);
		d.push();
		d.translate(0, (1 - (1 - (1 - intro) * (1 - intro))) * -6);
		drawSearch(ui);
		List<String> keys = panelKeys();
		// The panel being dragged is drawn last, on top
		if (dragging != null && keys.remove(dragging)) keys.add(dragging);
		for (String key : keys) drawPanel(ui, key);
		d.pop();

		String hint = status != null ? status : "LEFT CLICK: ON / OFF  ·  RIGHT CLICK: SETTINGS  ·  DRAG A TITLE TO MOVE IT";
		float scale = 0.7f;
		String shown = ui.trim(hint, (int) ((this.width - 30) / scale));
		int hintW = Math.round(ui.width(shown) * scale);
		int hx = this.width / 2 - hintW / 2;
		ui.d.roundRect(hx - 6, this.height - 13, hx + hintW + 6, this.height - 2, 5, 0xD0000000);
		ui.small(shown, hx, this.height - 10, scale, MenuTheme.alpha(ui.t.text(), 0xD0));
	}

	private void drawSearch(Ui ui) {
		int w = Math.min(180, this.width - 20);
		int x = (this.width - w) / 2;
		int y = 7;
		ui.field(x, y, w, 15, search, "Search modules...", editingText == null, SEARCH_ICON);
		ui.hit(x, y, x + w, y + 15, () -> editingText = null);
	}

	private static final String[] SEARCH_ICON = {
			"..####......",
			".#....#.....",
			"#......#....",
			"#......#....",
			"#......#....",
			".#....#.....",
			"..####.#....",
			".......##...",
			"........##..",
	};

	private void drawPanel(Ui ui, String key) {
		MenuTheme t = ui.t;
		int[] spot = spot(key);
		boolean folded = spot[2] != 0;
		int x = Math.max(0, Math.min(spot[0], this.width - PANEL_W));
		int y = Math.max(26, Math.min(spot[1], this.height - HEADER_H - 14));
		List<Module> modules = modules(key);
		List<ClientAction> actions = CLIENT.equals(key) ? clientActions() : List.of();

		int contentH = 0;
		if (!folded) {
			if (CLIENT.equals(key)) contentH = Math.max(1, actions.size()) * ROW_H;
			else {
				for (Module module : modules) contentH += ROW_H + (expanded.contains(module) ? settingsHeight(module) : 0);
				if (modules.isEmpty()) contentH = ROW_H;
			}
			contentH += 6;
		}
		int maxBody = Math.max(ROW_H * 2, this.height - y - HEADER_H - 16);
		int bodyH = Math.min(contentH, maxBody);
		int maxScroll = Math.max(0, contentH - bodyH);
		double scroll = Math.max(0, Math.min(scrolls.getOrDefault(key, 0.0), maxScroll));
		scrolls.put(key, scroll);
		int bottom = y + HEADER_H + bodyH;
		boxes.put(key, new int[] {x, y, x + PANEL_W, bottom});

		ui.d.roundRect(x, y, x + PANEL_W, bottom, RADIUS, t.panel());
		ui.d.roundOutline(x, y, x + PANEL_W, bottom, RADIUS, dragging != null && dragging.equals(key) ? t.edgeStrong() : t.edge());
		ui.d.roundRect(x + 1, y + 1, x + PANEL_W - 1, y + HEADER_H, RADIUS - 1, MenuTheme.alpha(0x000000, 0x50));
		if (!folded) ui.d.fill(x + 1, y + HEADER_H, x + PANEL_W - 1, y + HEADER_H + 1, t.edge());
		PixelIcons.draw(ui.d, icon(key), x + 5, y + 3, 1, t.accent());
		ui.title(title(key), x + 21, y + 5, t.text());
		boolean arrowHover = ui.hover(x + PANEL_W - 17, y + 2, x + PANEL_W - 3, y + HEADER_H - 2);
		chevron(ui.d, x + PANEL_W - 13, y + 7, !folded, arrowHover ? t.accent() : t.muted());
		ui.hit(x, y, x + PANEL_W, y + HEADER_H, () -> startDrag(key, x, y));
		ui.hit(x + PANEL_W - 17, y + 2, x + PANEL_W - 3, y + HEADER_H - 2, () -> {
			spot[2] = folded ? 0 : 1;
			Config.saveSoon();
		});
		if (folded) return;

		int top = y + HEADER_H + 3;
		ui.clip(x + 1, y + HEADER_H + 1, x + PANEL_W - 1, bottom - 1);
		int ry = top - (int) Math.round(scroll);
		if (CLIENT.equals(key)) {
			for (ClientAction action : actions) {
				actionRow(ui, x, ry, action.label(), action.action());
				ry += ROW_H;
			}
			if (actions.isEmpty()) emptyRow(ui, x, ry);
		} else {
			for (Module module : modules) {
				moduleRow(ui, module, x, ry);
				ry += ROW_H;
				if (expanded.contains(module)) {
					drawSettings(ui, module, x, ry);
					ry += settingsHeight(module);
				}
			}
			if (modules.isEmpty()) emptyRow(ui, x, ry);
		}
		ui.endClip();
		if (maxScroll > 0) ui.scrollbar(x + PANEL_W - 3, y + HEADER_H + 2, bodyH - 4, scroll, maxScroll, contentH);
	}

	private static void chevron(Draw d, int x, int y, boolean open, int color) {
		if (open) {
			for (int i = 0; i < 4; i++) d.fill(x + i, y + i, x + 7 - i, y + i + 1, color);
		} else {
			for (int i = 0; i < 4; i++) d.fill(x + 2 + i, y - 1 + i, x + 3 + i, y + 6 - i, color);
		}
	}

	private void emptyRow(Ui ui, int x, int y) {
		ui.small("NO MATCHES", x + 8, y + 4, 0.75f, ui.t.muted());
	}

	private void moduleRow(Ui ui, Module module, int x, int y) {
		MenuTheme t = ui.t;
		boolean on = module.enabled();
		boolean toggleable = module.toggleable();
		boolean open = expanded.contains(module);
		int x1 = x + 3;
		int x2 = x + PANEL_W - 3;
		boolean hover = ui.hover(x1, y, x2, y + ROW_H - 1);
		if (toggleable && on) {
			ui.d.roundRect(x1, y, x2, y + ROW_H - 1, 3, hover ? Draw.mix(onFill(t), 0xFFFFFFFF, 0.1) : onFill(t));
		} else if (hover || open) {
			ui.d.roundRect(x1, y, x2, y + ROW_H - 1, 3, t.cardHover());
		}
		String name = module.name.toUpperCase(Locale.ROOT);
		int nameColor = !toggleable || on ? 0xFFFFFFFF : MenuTheme.alpha(t.text(), 0xB0);
		ui.small(ui.trim(name, (int) ((PANEL_W - 26) / 0.75f)), x1 + 5, y + 4, 0.75f, nameColor);
		if (toggleable) {
			ui.dot(x2 - 9, y + 4, 5, on ? t.accent() : MenuTheme.alpha(t.muted(), 0x70));
		} else {
			chevron(ui.d, x2 - 10, y + 4, open, t.muted());
		}
		if (hover) status = module.name + ": " + module.description;
		Runnable toggleOpen = () -> {
			if (!expanded.remove(module)) expanded.add(module);
		};
		ui.hit(x1, y, x2, y + ROW_H - 1, toggleable ? module::toggle : toggleOpen, toggleOpen);
	}

	/** Rows that are switched on: the accent, darkened so white text stays readable. */
	private static int onFill(MenuTheme t) {
		return MenuTheme.alpha(Draw.mix(t.accent(), 0xFF0A1846, 0.45), 0xE0);
	}

	private void actionRow(Ui ui, int x, int y, String label, Runnable action) {
		MenuTheme t = ui.t;
		int x1 = x + 3;
		int x2 = x + PANEL_W - 3;
		boolean hover = ui.hover(x1, y, x2, y + ROW_H - 1);
		if (hover) ui.d.roundRect(x1, y, x2, y + ROW_H - 1, 3, t.cardHover());
		ui.small(label.toUpperCase(Locale.ROOT), x1 + 5, y + 4, 0.75f, hover ? 0xFFFFFFFF : MenuTheme.alpha(t.text(), 0xD0));
		chevron(ui.d, x2 - 9, y + 4, false, hover ? t.accent() : t.muted());
		ui.hit(x1, y, x2, y + ROW_H - 1, action);
	}

	private boolean hasWidget(Module module) {
		return HudWidgets.ALL.stream().anyMatch(widget -> widget.module == module);
	}

	private int settingHeight(Setting setting) {
		if (setting instanceof Setting.Slider) return 18;
		if (setting instanceof Setting.Choice choice) return choiceFits(choice) ? 19 : 13;
		if (setting instanceof Setting.Color) return 19;
		if (setting instanceof Setting.Text) return 24;
		return 13;
	}

	private int settingsHeight(Module module) {
		int h = 4;
		for (Setting setting : module.settings()) h += settingHeight(setting);
		if (hasWidget(module)) h += 13;
		if (MORE_OPTIONS.contains(module)) h += 13;
		if (module.settings().isEmpty() && !hasWidget(module) && !MORE_OPTIONS.contains(module)) h += 13;
		return h + 2;
	}

	private boolean choiceFits(Setting.Choice choice) {
		int total = 0;
		for (String option : choice.options()) total += Math.round(Minecraft.getInstance().font.width(option.toUpperCase(Locale.ROOT)) * 0.7f) + 6;
		return total <= PANEL_W - 16;
	}

	private void drawSettings(Ui ui, Module module, int x, int y) {
		MenuTheme t = ui.t;
		int h = settingsHeight(module);
		ui.d.fill(x + 5, y, x + 6, y + h - 2, MenuTheme.alpha(t.accent(), 0x90));
		int left = x + 10;
		int right = x + PANEL_W - 7;
		int sy = y + 2;
		for (Setting setting : module.settings()) {
			drawSetting(ui, setting, left, right, sy);
			sy += settingHeight(setting);
		}
		Minecraft mc = Minecraft.getInstance();
		if (hasWidget(module)) {
			linkRow(ui, left, right, sy, "Move on screen", () -> mc.setScreen(new HubScreen(this)));
			sy += 13;
		}
		if (MORE_OPTIONS.contains(module)) {
			linkRow(ui, left, right, sy, "More options", () -> mc.setScreen(new ModMenuScreen(this).openModule(module)));
			sy += 13;
		}
		if (module.settings().isEmpty() && !hasWidget(module) && !MORE_OPTIONS.contains(module)) {
			ui.small("NO SETTINGS", left, sy + 3, 0.7f, t.muted());
		}
	}

	private void linkRow(Ui ui, int left, int right, int y, String label, Runnable action) {
		boolean hover = ui.hover(left - 2, y, right + 2, y + 12);
		ui.small(label.toUpperCase(Locale.ROOT), left, y + 3, 0.7f, hover ? ui.t.accent() : ui.t.text());
		chevron(ui.d, right - 5, y + 3, false, hover ? ui.t.accent() : ui.t.muted());
		ui.hit(left - 2, y, right + 2, y + 12, action);
	}

	private void drawSetting(Ui ui, Setting setting, int left, int right, int y) {
		MenuTheme t = ui.t;
		float scale = 0.7f;
		String label = setting.name.toUpperCase(Locale.ROOT);
		boolean rowHover = ui.hover(left - 2, y, right + 2, y + settingHeight(setting));
		if (rowHover) status = setting.name + ": " + setting.description;
		if (setting instanceof Setting.Toggle toggle) {
			ui.small(ui.trim(label, (int) ((right - left - 14) / scale)), left, y + 3, scale, t.text());
			int bx = right - 8;
			boolean on = toggle.get();
			ui.d.roundRect(bx, y + 2, bx + 8, y + 10, 2, on ? t.accent() : MenuTheme.alpha(0x000000, 0x60));
			ui.d.roundOutline(bx, y + 2, bx + 8, y + 10, 2, on ? t.accent() : t.edgeStrong());
			if (on) {
				ui.d.fill(bx + 2, y + 6, bx + 3, y + 7, t.onAccent());
				ui.d.fill(bx + 3, y + 7, bx + 4, y + 8, t.onAccent());
				ui.d.fill(bx + 4, y + 6, bx + 5, y + 7, t.onAccent());
				ui.d.fill(bx + 5, y + 5, bx + 6, y + 6, t.onAccent());
			}
			ui.hit(left - 2, y, right + 2, y + 12, toggle::toggle);
		} else if (setting instanceof Setting.Slider slider) {
			String value = slider.format();
			int valueW = Math.round(ui.width(value) * scale);
			ui.small(ui.trim(label, (int) ((right - left - valueW - 4) / scale)), left, y + 2, scale, t.text());
			ui.small(value, right - valueW, y + 2, scale, t.muted());
			int trackY = y + 11;
			int trackW = right - left;
			ui.d.roundRect(left, trackY, right, trackY + 3, 1, MenuTheme.alpha(t.muted(), 0x50));
			int filled = (int) Math.round(trackW * slider.fraction());
			ui.d.roundRect(left, trackY, left + filled, trackY + 3, 1, t.accent());
			int knob = left + filled - 2;
			ui.d.roundRect(knob, trackY - 2, knob + 5, trackY + 5, 2, 0xFFFFFFFF);
			ui.hit(left - 2, trackY - 4, right + 2, trackY + 7, () -> {
				draggingSlider = slider;
				sliderX = left;
				sliderW = trackW;
				slider.setFraction((ui.mouseX - left) / (double) trackW);
			});
		} else if (setting instanceof Setting.Choice choice) {
			if (choiceFits(choice)) {
				ui.small(ui.trim(label, (int) ((right - left) / scale)), left, y + 2, scale, t.text());
				int ox = left;
				for (String option : choice.options()) {
					String text = option.toUpperCase(Locale.ROOT);
					int ow = Math.round(ui.width(text) * scale) + 6;
					boolean active = choice.is(option);
					boolean hover = ui.hover(ox, y + 10, ox + ow, y + 18);
					if (active) ui.d.roundRect(ox, y + 10, ox + ow, y + 18, 2, onFill(t));
					ui.small(text, ox + 3, y + 11, scale, active ? 0xFFFFFFFF : hover ? t.text() : t.muted());
					ui.hit(ox, y + 10, ox + ow, y + 18, () -> choice.set(option));
					ox += ow + 2;
				}
			} else {
				String value = choice.get().toUpperCase(Locale.ROOT);
				int valueW = Math.round(ui.width(value) * scale);
				ui.small(ui.trim(label, (int) ((right - left - valueW - 12) / scale)), left, y + 3, scale, t.text());
				boolean hover = ui.hover(right - valueW - 10, y, right + 2, y + 12);
				ui.small(value, right - valueW - 4, y + 3, scale, hover ? t.accent() : t.muted());
				chevron(ui.d, right - 3, y + 3, false, hover ? t.accent() : t.muted());
				ui.hit(left - 2, y, right + 2, y + 12, choice::next, choice::previous);
			}
		} else if (setting instanceof Setting.Color color) {
			ui.small(ui.trim(label, (int) ((right - left) / scale)), left, y + 2, scale, t.text());
			int sx = left;
			for (int swatch : Setting.Color.SWATCHES) {
				boolean selected = (color.get() | 0xFF000000) == swatch;
				if (selected) ui.d.roundRect(sx - 1, y + 10, sx + 8, y + 19, 2, 0xFFFFFFFF);
				ui.d.roundRect(sx, y + 11, sx + 7, y + 18, 2, swatch);
				int pick = swatch;
				ui.hit(sx, y + 11, sx + 7, y + 18, () -> color.set(pick));
				sx += 9;
				if (sx + 7 > right) break;
			}
		} else if (setting instanceof Setting.Text text) {
			ui.small(ui.trim(label, (int) ((right - left) / scale)), left, y + 2, scale, t.text());
			boolean focused = editingText == text;
			ui.field(left, y + 10, right - left, 12, text.get(), "Type here", focused, null);
			ui.hit(left, y + 10, right, y + 22, () -> editingText = text);
		}
	}

	private void startDrag(String key, int x, int y) {
		dragging = key;
		grabX = (int) Math.round(lastMouseX) - x;
		grabY = (int) Math.round(lastMouseY) - y;
	}

	private double lastMouseX;
	private double lastMouseY;

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		lastMouseX = event.x();
		lastMouseY = event.y();
		int button = event.button();
		if (button == 0 || button == 1) {
			for (int i = hits.size() - 1; i >= 0; i--) {
				Ui.Hit hit = hits.get(i);
				if (!hit.contains(event.x(), event.y())) continue;
				Runnable action = button == 0 ? hit.action() : hit.rightAction();
				if (action == null) continue;
				// Clicking anywhere lets go of a text field; clicking a field takes the keyboard again
				editingText = null;
				action.run();
				if (dragging == null) {
					Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
				}
				return true;
			}
		}
		editingText = null;
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (draggingSlider != null) {
			draggingSlider.setFraction((event.x() - sliderX) / sliderW);
			return true;
		}
		if (dragging != null) {
			int[] spot = spot(dragging);
			spot[0] = Math.max(0, Math.min(this.width - PANEL_W, (int) Math.round(event.x()) - grabX));
			spot[1] = Math.max(26, Math.min(this.height - HEADER_H - 14, (int) Math.round(event.y()) - grabY));
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (draggingSlider != null) {
			draggingSlider = null;
			return true;
		}
		if (dragging != null) {
			dragging = null;
			Config.saveSoon();
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
		for (Map.Entry<String, int[]> entry : boxes.entrySet()) {
			int[] box = entry.getValue();
			if (x < box[0] || x >= box[2] || y < box[1] || y >= box[3]) continue;
			scrolls.put(entry.getKey(), Math.max(0, scrolls.getOrDefault(entry.getKey(), 0.0) - vertical * ROW_H * 2));
			return true;
		}
		return super.mouseScrolled(x, y, horizontal, vertical);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		String typed = event.codepointAsString();
		if (typed.isEmpty() || typed.codePointAt(0) < 32 || typed.codePointAt(0) == 127) return false;
		if (editingText != null) {
			editingText.set(editingText.get() + typed);
		} else if (search.length() < 32) {
			search += typed;
			scrolls.clear();
		}
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int key = event.key();
		if (key == GLFW.GLFW_KEY_BACKSPACE) {
			if (editingText != null) {
				String value = editingText.get();
				if (!value.isEmpty()) editingText.set(value.substring(0, value.length() - 1));
			} else if (!search.isEmpty()) {
				search = search.substring(0, search.length() - 1);
				scrolls.clear();
			}
			return true;
		}
		if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
			editingText = null;
			return true;
		}
		if (key == GLFW.GLFW_KEY_ESCAPE) {
			if (editingText != null) {
				editingText = null;
				return true;
			}
			if (!search.isEmpty()) {
				search = "";
				scrolls.clear();
				return true;
			}
		}
		if (editingText == null && search.isEmpty() && ThreevhyClient.openSettingsKey != null
				&& ThreevhyClient.openSettingsKey.matches(event)) {
			onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	/** Searches the panels, as if typed (for the in-game test). */
	public ClickGuiScreen search(String text) {
		search = text;
		return this;
	}

	/** Opens a mod's settings under it (for the in-game test). */
	public ClickGuiScreen expand(Module module) {
		expanded.add(module);
		return this;
	}

	@Override
	public void onClose() {
		Config.save();
		Minecraft.getInstance().setScreen(parent);
	}
}
