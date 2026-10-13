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
	private static final int PANEL_W = 160;
	private static final int HEADER_H = 24;
	private static final int ROW_H = 19;
	private static final int RADIUS = 8;
	/** Saved panel spots from before this layout are laid out again, since panels got wider. */
	private static final int LAYOUT = 2;
	private static final int PANEL = 0xF2232529;
	private static final int ROW_TEXT = 0xFFD5D8DE;
	private static final int MUTED = 0xFF8B9099;
	private static final int BOX_OFF = 0xFF3A3D45;
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
		if (saved != null && saved.length >= 4 && saved[3] == LAYOUT) return saved;
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
		java.util.Arrays.fill(bottoms, 32);
		for (String key : keys) {
			int column = 0;
			for (int i = 1; i < columns; i++) if (bottoms[i] < bottoms[column]) column = i;
			int[] spot = Config.get().panels.get(key);
			if (spot == null || spot.length < 4 || spot[3] != LAYOUT) {
				spot = new int[] {left + column * (PANEL_W + 8), bottoms[column], 0, LAYOUT};
				Config.get().panels.put(key, spot);
			}
			bottoms[column] = spot[1] + HEADER_H + defaultHeight(key) + 8;
		}
	}

	private int defaultHeight(String key) {
		return (CLIENT.equals(key) ? clientActions().size() : modules(key).size()) * ROW_H + 8;
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
		float scale = this.height >= 360 ? 1f : 0.7f;
		String shown = ui.trim(hint, (int) ((this.width - 30) / scale));
		int hintW = Math.round(ui.width(shown) * scale);
		int hintH = Math.round(8 * scale);
		int hx = this.width / 2 - hintW / 2;
		ui.d.roundRect(hx - 6, this.height - hintH - 7, hx + hintW + 6, this.height - 2, 5, 0xD0000000);
		ui.small(shown, hx, this.height - hintH - 4, scale, MenuTheme.alpha(ui.t.text(), 0xD0));
	}

	/** The search bar: a dark pill with a magnifier, like the panels. */
	private void drawSearch(Ui ui) {
		int w = Math.min(220, this.width - 20);
		int h = 18;
		int x = (this.width - w) / 2;
		int y = 6;
		boolean focused = editingText == null;
		ui.d.roundRect(x, y, x + w, y + h, h / 2, PANEL);
		ui.d.roundOutline(x, y, x + w, y + h, h / 2, focused && !search.isEmpty() ? MenuTheme.alpha(accent(), 0xC0) : 0x30FFFFFF);
		PixelIcons.draw(ui.d, SEARCH_ICON, x + 8, y + 4, 1, search.isEmpty() ? MUTED : accent());
		int tx = x + 24;
		int max = w - 30;
		String shown = search.isEmpty() ? "SEARCH MODULES..." : search.toUpperCase(Locale.ROOT);
		while (!shown.isEmpty() && ui.width(shown) > max) shown = shown.substring(1);
		ui.text(shown, tx, y + 5, search.isEmpty() ? MUTED : 0xFFFFFFFF);
		if (focused && !search.isEmpty() && (System.currentTimeMillis() / 500) % 2 == 0) {
			int cx = tx + ui.width(shown) + 1;
			ui.d.fill(cx, y + 4, cx + 1, y + h - 4, 0xFFFFFFFF);
		}
		ui.hit(x, y, x + w, y + h, () -> editingText = null);
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
		int y = Math.max(30, Math.min(spot[1], this.height - HEADER_H - 14));
		List<Module> modules = modules(key);
		List<ClientAction> actions = CLIENT.equals(key) ? clientActions() : List.of();

		int contentH = 0;
		if (!folded) {
			if (CLIENT.equals(key)) contentH = Math.max(1, actions.size()) * ROW_H;
			else {
				for (Module module : modules) contentH += ROW_H + (expanded.contains(module) ? settingsHeight(module) : 0);
				if (modules.isEmpty()) contentH = ROW_H;
			}
			contentH += 8;
		}
		int maxBody = Math.max(ROW_H * 2, this.height - y - HEADER_H - 16);
		int bodyH = Math.min(contentH, maxBody);
		int maxScroll = Math.max(0, contentH - bodyH);
		double scroll = Math.max(0, Math.min(scrolls.getOrDefault(key, 0.0), maxScroll));
		scrolls.put(key, scroll);
		int bottom = y + HEADER_H + bodyH;
		boxes.put(key, new int[] {x, y, x + PANEL_W, bottom});

		ui.d.roundRect(x, y, x + PANEL_W, bottom, RADIUS, PANEL);
		if (dragging != null && dragging.equals(key)) ui.d.roundOutline(x, y, x + PANEL_W, bottom, RADIUS, MenuTheme.alpha(accent(), 0xA0));
		ui.d.fill(x + 8, y + HEADER_H - 3, x + PANEL_W - 8, y + HEADER_H - 2, accent());
		PixelIcons.draw(ui.d, icon(key), x + 8, y + 5, 1, accent());
		ui.title(title(key), x + 25, y + 8, 0xFFFFFFFF);
		boolean arrowHover = ui.hover(x + PANEL_W - 21, y + 3, x + PANEL_W - 4, y + HEADER_H - 3);
		chevron(ui.d, x + PANEL_W - 17, y + 9, !folded, arrowHover ? accent() : MUTED);
		ui.hit(x, y, x + PANEL_W, y + HEADER_H, () -> startDrag(key, x, y));
		ui.hit(x + PANEL_W - 21, y + 3, x + PANEL_W - 4, y + HEADER_H - 3, () -> {
			spot[2] = folded ? 0 : 1;
			Config.saveSoon();
		});
		if (folded) return;

		int top = y + HEADER_H + 4;
		ui.clip(x + 1, y + HEADER_H, x + PANEL_W - 1, bottom - 2);
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
		if (maxScroll > 0) {
			int trackY = y + HEADER_H + 2;
			int trackH = bodyH - 6;
			int thumb = Math.max(14, trackH * trackH / Math.max(1, contentH));
			int thumbY = trackY + (int) Math.round((trackH - thumb) * (scroll / maxScroll));
			ui.d.roundRect(x + PANEL_W - 3, thumbY, x + PANEL_W - 1, thumbY + thumb, 1, MenuTheme.alpha(accent(), 0xC0));
		}
	}

	private static void chevron(Draw d, int x, int y, boolean open, int color) {
		if (open) {
			for (int i = 0; i < 4; i++) d.fill(x + i, y + i, x + 7 - i, y + i + 1, color);
		} else {
			for (int i = 0; i < 4; i++) d.fill(x + 2 + i, y - 1 + i, x + 3 + i, y + 6 - i, color);
		}
	}

	private void emptyRow(Ui ui, int x, int y) {
		ui.text("NO MATCHES", x + 14, y + 5, MUTED);
	}

	private void moduleRow(Ui ui, Module module, int x, int y) {
		boolean on = module.enabled();
		boolean toggleable = module.toggleable();
		boolean open = expanded.contains(module);
		int x1 = x + 7;
		int x2 = x + PANEL_W - 7;
		int h = ROW_H - 3;
		boolean hover = ui.hover(x1, y, x2, y + h);
		if (toggleable && on) {
			ui.d.roundRect(x1, y, x2, y + h, 5, hover ? Draw.mix(onFill(), 0xFFFFFFFF, 0.1) : onFill());
		} else if (hover || open) {
			ui.d.roundRect(x1, y, x2, y + h, 5, 0x18FFFFFF);
		}
		String name = ui.trimTitle(module.name, x2 - x1 - 24);
		ui.title(name, x1 + 7, y + (h - 8) / 2 + 1, toggleable && on ? onText() : ROW_TEXT);
		if (toggleable) {
			if (on) ui.dot(x2 - 11, y + h / 2 - 2, 4, 0xF0FFFFFF);
			else ui.dot(x2 - 10, y + h / 2 - 1, 3, 0x55FFFFFF);
		} else {
			chevron(ui.d, x2 - 13, y + (h - 4) / 2, open, MUTED);
		}
		if (hover) status = module.name + ": " + module.description;
		Runnable toggleOpen = () -> {
			if (!expanded.remove(module)) expanded.add(module);
		};
		ui.hit(x1, y, x2, y + h, toggleable ? module::toggle : toggleOpen, toggleOpen);
	}

	/** The colour picked in Theme > Panels colour. */
	private static int accent() {
		return Modules.THEME_PANEL_COLOR.get() | 0xFF000000;
	}

	/** A switched-on row: the colour, a little deeper. */
	private static int onFill() {
		return Draw.mix(accent(), 0xFF000000, 0.18);
	}

	/** Text on a switched-on row: a pale tint of the colour. */
	private static int onText() {
		return Draw.mix(accent(), 0xFFFFFFFF, 0.85);
	}

	private void actionRow(Ui ui, int x, int y, String label, Runnable action) {
		int x1 = x + 7;
		int x2 = x + PANEL_W - 7;
		int h = ROW_H - 3;
		boolean hover = ui.hover(x1, y, x2, y + h);
		if (hover) ui.d.roundRect(x1, y, x2, y + h, 5, 0x18FFFFFF);
		ui.title(ui.trimTitle(label, x2 - x1 - 24), x1 + 7, y + (h - 8) / 2 + 1, hover ? 0xFFFFFFFF : ROW_TEXT);
		chevron(ui.d, x2 - 12, y + (h - 6) / 2, false, hover ? accent() : MUTED);
		ui.hit(x1, y, x2, y + h, action);
	}

	private boolean hasWidget(Module module) {
		return HudWidgets.ALL.stream().anyMatch(widget -> widget.module == module);
	}

	private static final int LINE_H = 16;

	private int settingHeight(Setting setting) {
		if (setting instanceof Setting.Slider) return 23;
		if (setting instanceof Setting.Choice choice) return choiceFits(choice) ? 26 : LINE_H;
		if (setting instanceof Setting.Color) return 23;
		if (setting instanceof Setting.Text) return 29;
		return LINE_H;
	}

	private int settingsHeight(Module module) {
		int h = 4;
		for (Setting setting : module.settings()) h += settingHeight(setting);
		if (hasWidget(module)) h += LINE_H;
		if (MORE_OPTIONS.contains(module)) h += LINE_H;
		if (module.settings().isEmpty() && !hasWidget(module) && !MORE_OPTIONS.contains(module)) h += LINE_H;
		return h + 2;
	}

	private boolean choiceFits(Setting.Choice choice) {
		int total = 0;
		for (String option : choice.options()) total += Minecraft.getInstance().font.width(option.toUpperCase(Locale.ROOT)) + 11;
		return total <= PANEL_W - 30;
	}

	private void drawSettings(Ui ui, Module module, int x, int y) {
		MenuTheme t = ui.t;
		int h = settingsHeight(module);
		ui.d.fill(x + 13, y + 1, x + 14, y + h - 3, 0x50FFFFFF);
		int left = x + 20;
		int right = x + PANEL_W - 11;
		int sy = y + 2;
		for (Setting setting : module.settings()) {
			drawSetting(ui, setting, left, right, sy);
			sy += settingHeight(setting);
		}
		Minecraft mc = Minecraft.getInstance();
		if (hasWidget(module)) {
			linkRow(ui, left, right, sy, "Move on screen", () -> mc.setScreen(new HubScreen(this)));
			sy += LINE_H;
		}
		if (MORE_OPTIONS.contains(module)) {
			linkRow(ui, left, right, sy, "More options", () -> mc.setScreen(new ModMenuScreen(this).openModule(module)));
			sy += LINE_H;
		}
		if (module.settings().isEmpty() && !hasWidget(module) && !MORE_OPTIONS.contains(module)) {
			ui.text("NO SETTINGS", left, sy + 4, MUTED);
		}
	}

	private void linkRow(Ui ui, int left, int right, int y, String label, Runnable action) {
		boolean hover = ui.hover(left - 2, y, right + 2, y + LINE_H - 1);
		ui.text(ui.trim(label.toUpperCase(Locale.ROOT), right - left - 10), left, y + 4, hover ? accent() : ROW_TEXT);
		chevron(ui.d, right - 5, y + 4, false, hover ? accent() : MUTED);
		ui.hit(left - 2, y, right + 2, y + LINE_H - 1, action);
	}

	private void drawSetting(Ui ui, Setting setting, int left, int right, int y) {
		String label = setting.name.toUpperCase(Locale.ROOT);
		boolean rowHover = ui.hover(left - 2, y, right + 2, y + settingHeight(setting));
		if (rowHover) status = setting.name + ": " + setting.description;
		if (setting instanceof Setting.Toggle toggle) {
			ui.text(ui.trim(label, right - left - 16), left, y + 4, ROW_TEXT);
			checkbox(ui.d, right - 10, y + 2, toggle.get());
			ui.hit(left - 2, y, right + 2, y + LINE_H - 1, toggle::toggle);
		} else if (setting instanceof Setting.Slider slider) {
			String value = slider.format();
			int valueW = ui.width(value);
			ui.text(ui.trim(label, right - left - valueW - 6), left, y + 2, ROW_TEXT);
			ui.text(value, right - valueW, y + 2, MUTED);
			int trackY = y + 15;
			int trackW = right - left;
			ui.d.roundRect(left, trackY, right, trackY + 3, 1, BOX_OFF);
			int filled = (int) Math.round(trackW * slider.fraction());
			if (filled > 0) ui.d.roundRect(left, trackY, left + filled, trackY + 3, 1, accent());
			int knob = left + filled - 3;
			ui.d.roundRect(knob, trackY - 2, knob + 7, trackY + 5, 3, 0xFFFFFFFF);
			ui.hit(left - 2, trackY - 4, right + 2, trackY + 7, () -> {
				draggingSlider = slider;
				sliderX = left;
				sliderW = trackW;
				slider.setFraction((ui.mouseX - left) / (double) trackW);
			});
		} else if (setting instanceof Setting.Choice choice) {
			if (choiceFits(choice)) {
				ui.text(ui.trim(label, right - left), left, y + 2, ROW_TEXT);
				int ox = left;
				for (String option : choice.options()) {
					String text = option.toUpperCase(Locale.ROOT);
					int ow = ui.width(text) + 8;
					boolean active = choice.is(option);
					boolean hover = ui.hover(ox, y + 12, ox + ow, y + 24);
					ui.d.roundRect(ox, y + 12, ox + ow, y + 24, 3, active ? onFill() : hover ? 0x24FFFFFF : 0x12FFFFFF);
					ui.text(text, ox + 4, y + 14, active ? onText() : hover ? 0xFFFFFFFF : MUTED);
					ui.hit(ox, y + 12, ox + ow, y + 24, () -> choice.set(option));
					ox += ow + 3;
				}
			} else {
				String value = choice.get().toUpperCase(Locale.ROOT);
				int valueW = ui.width(value);
				ui.text(ui.trim(label, right - left - valueW - 14), left, y + 4, ROW_TEXT);
				boolean hover = ui.hover(right - valueW - 10, y, right + 2, y + LINE_H - 1);
				ui.text(value, right - valueW - 6, y + 4, hover ? accent() : MUTED);
				chevron(ui.d, right - 3, y + 4, false, hover ? accent() : MUTED);
				ui.hit(left - 2, y, right + 2, y + LINE_H - 1, choice::next, choice::previous);
			}
		} else if (setting instanceof Setting.Color color) {
			ui.text(ui.trim(label, right - left), left, y + 2, ROW_TEXT);
			int sx = left;
			for (int swatch : Setting.Color.SWATCHES) {
				boolean selected = (color.get() | 0xFF000000) == swatch;
				if (selected) ui.d.roundRect(sx - 1, y + 12, sx + 9, y + 22, 3, 0xFFFFFFFF);
				ui.d.roundRect(sx, y + 13, sx + 8, y + 21, 2, swatch);
				int pick = swatch;
				ui.hit(sx, y + 13, sx + 8, y + 21, () -> color.set(pick));
				sx += 11;
				if (sx + 8 > right) break;
			}
		} else if (setting instanceof Setting.Text text) {
			ui.text(ui.trim(label, right - left), left, y + 2, ROW_TEXT);
			boolean focused = editingText == text;
			int fy = y + 13;
			ui.d.roundRect(left, fy, right, fy + 14, 3, BOX_OFF);
			if (focused) ui.d.roundOutline(left, fy, right, fy + 14, 3, accent());
			String value = text.get();
			String shown = value.isEmpty() && !focused ? "Type here" : value;
			while (!shown.isEmpty() && ui.width(shown) > right - left - 8) shown = shown.substring(1);
			ui.text(shown, left + 4, fy + 3, value.isEmpty() ? MUTED : 0xFFFFFFFF);
			if (focused && (System.currentTimeMillis() / 500) % 2 == 0) {
				int cx = left + 4 + ui.width(value.isEmpty() ? "" : shown) + 1;
				ui.d.fill(cx, fy + 2, cx + 1, fy + 12, 0xFFFFFFFF);
			}
			ui.hit(left, fy, right, fy + 14, () -> editingText = text);
		}
	}

	/** A small check box: filled with the panel colour and ticked when on, a dark box with an x when off. */
	private static void checkbox(Draw d, int x, int y, boolean on) {
		d.roundRect(x, y, x + 10, y + 10, 3, on ? accent() : BOX_OFF);
		int mark = on ? 0xFF101318 : MUTED;
		if (on) {
			d.fill(x + 2, y + 5, x + 3, y + 6, mark);
			d.fill(x + 3, y + 6, x + 4, y + 7, mark);
			d.fill(x + 4, y + 7, x + 5, y + 8, mark);
			d.fill(x + 5, y + 6, x + 6, y + 7, mark);
			d.fill(x + 6, y + 5, x + 7, y + 6, mark);
			d.fill(x + 7, y + 4, x + 8, y + 5, mark);
			d.fill(x + 8, y + 3, x + 9, y + 4, mark);
		} else {
			for (int i = 0; i < 4; i++) {
				d.fill(x + 3 + i, y + 3 + i, x + 4 + i, y + 4 + i, mark);
				d.fill(x + 6 - i, y + 3 + i, x + 7 - i, y + 4 + i, mark);
			}
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
			spot[1] = Math.max(30, Math.min(this.height - HEADER_H - 14, (int) Math.round(event.y()) - grabY));
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
