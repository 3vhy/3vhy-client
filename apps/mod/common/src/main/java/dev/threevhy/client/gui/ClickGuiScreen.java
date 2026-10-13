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
 * The mod menu's Panels style, like 67 Client's click GUI: a floating panel for each category and a
 * search bar on top. Left click a mod to switch it on or off, right click to open its settings right
 * under it. Panels are moved by dragging their title and folded with the arrow; where they are and
 * which are folded is saved.
 *
 * <p>Like 67's, it's the same real size whatever Minecraft's GUI scale is: it's laid out in its own
 * units, {@link #UNITS_PER_SCREEN} to the window's height, and drawn with {@link Smooth} shapes so
 * nothing is pixelated. A Themes panel picks the client's colour ({@link Accent}), and a pill at the
 * bottom opens your profiles, like 67's Configs.
 */
public final class ClickGuiScreen extends CompatScreen {
	/** How many layout units tall the window is: 540 makes a unit two pixels on a 1080p screen. */
	private static final float UNITS_PER_SCREEN = 540f;
	private static final int PANEL_W = 108;
	private static final int HEADER_H = 20;
	private static final int ROW_H = 16;
	private static final int RADIUS = 4;
	/** Saved panel spots from before this layout are laid out again. */
	private static final int LAYOUT = 4;
	private static final int TOP = 28;
	private static final int GAP = 8;
	/** Text size in rows and settings, and in panel titles. */
	private static final float TEXT = 0.7f;
	private static final float TITLE = 0.78f;
	private static final int PANEL = 0xF0171A21;
	private static final int ROW_TEXT = 0xFFC9CDD6;
	private static final int MUTED = 0xFF7D838F;
	private static final int BOX_OFF = 0xFF2E333D;
	private static final String CLIENT = "client";
	private static final String THEMES = "themes";
	/** Mods whose extra options (lists, files, status) only fit the Classic page. */
	private static final Set<Module> MORE_OPTIONS = Set.of(Modules.PLAYER_LIST, Modules.NAME_PROTECT, Modules.SKIN_PROTECT,
			Modules.MUSIC, Modules.THEME, Modules.TEXT_HOTKEYS);

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

	/** GUI units per layout unit, and the screen's size in layout units, worked out each frame. */
	private float unit = 1;
	private int vw = 1;
	private int vh = 1;

	public ClickGuiScreen(Screen parent) {
		super(Component.literal("3vhy Mod Menu"));
		this.parent = parent;
	}

	/** Works out the layout unit for this window: the same real size at any GUI scale. */
	private void measure() {
		Minecraft mc = Minecraft.getInstance();
		float guiScale = (float) mc.getWindow().getGuiScale();
		float pixelsPerUnit = Math.max(1f, mc.getWindow().getHeight() / UNITS_PER_SCREEN);
		unit = pixelsPerUnit / guiScale;
		vw = Math.max(1, Math.round(this.width / unit));
		vh = Math.max(1, Math.round(this.height / unit));
	}

	private static List<String> panelKeys() {
		List<String> keys = new ArrayList<>();
		for (Category category : Category.values()) keys.add(category.name().toLowerCase(Locale.ROOT));
		keys.add(CLIENT);
		keys.add(THEMES);
		return keys;
	}

	private static Category category(String key) {
		for (Category category : Category.values()) {
			if (category.name().toLowerCase(Locale.ROOT).equals(key)) return category;
		}
		return null;
	}

	private static String title(String key) {
		if (THEMES.equals(key)) return "THEMES";
		Category category = category(key);
		return category == null ? "CLIENT" : category.label.toUpperCase(Locale.ROOT);
	}

	private static String icon(String key) {
		if (THEMES.equals(key)) return "target";
		Category category = category(key);
		if (category == null) return "spark";
		return switch (category) {
			case HUD -> "grid";
			case PVP -> "target";
			case VISUAL -> "eye";
			case UTILITY -> "sliders";
		};
	}

	/** A panel's saved spot {x, y, folded, layout}; panels without one are laid out first. */
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
		int columns = Math.max(1, Math.min(keys.size(), (vw - 16) / (PANEL_W + GAP)));
		int left = Math.max(8, (vw - columns * (PANEL_W + GAP) + GAP) / 2);
		int[] bottoms = new int[columns];
		java.util.Arrays.fill(bottoms, TOP);
		for (String key : keys) {
			int column = 0;
			for (int i = 1; i < columns; i++) if (bottoms[i] < bottoms[column]) column = i;
			int[] spot = Config.get().panels.get(key);
			if (spot == null || spot.length < 4 || spot[3] != LAYOUT) {
				spot = new int[] {left + column * (PANEL_W + GAP), bottoms[column], 0, LAYOUT};
				Config.get().panels.put(key, spot);
			}
			bottoms[column] = spot[1] + HEADER_H + defaultHeight(key) + GAP;
		}
	}

	private int defaultHeight(String key) {
		if (THEMES.equals(key)) return (Accent.PRESETS.size() + 1) * ROW_H + 8;
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
				new ClientAction("Edit HUD", () -> mc.setScreen(new HubScreen(this).editOnly())),
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
		measure();
		hits.clear();
		boxes.clear();
		d.push();
		d.scale(unit);
		Ui ui = new Ui(d, Math.round(mouseX / unit), Math.round(mouseY / unit), hits).cleanFont(UiFont.clean()).textScale(TEXT);
		float intro = Math.min(1f, (System.currentTimeMillis() - openedAt) / 200f);
		d.translate(0, (1 - (1 - (1 - intro) * (1 - intro))) * -6);
		drawSearch(ui);
		List<String> keys = panelKeys();
		// The panel being dragged is drawn last, on top
		if (dragging != null && keys.remove(dragging)) keys.add(dragging);
		for (String key : keys) drawPanel(ui, key);
		drawProfilesButton(ui);
		d.pop();
	}

	/** A pill at the bottom, like 67's Configs: opens your profiles. */
	private void drawProfilesButton(Ui ui) {
		String label = "PROFILES";
		int w = ui.boldWidth(label) + 26;
		int h = 14;
		int x = (vw - w) / 2;
		int y = vh - h - 8;
		boolean hover = ui.hover(x, y, x + w, y + h);
		Smooth.roundRect(ui.d, x, y, x + w, y + h, h / 2f, hover ? Draw.mix(PANEL, 0xFFFFFFFF, 0.06) : PANEL);
		Smooth.roundOutline(ui.d, x, y, x + w, y + h, h / 2f, 0.5f, hover ? MenuTheme.alpha(accent(), 0xC0) : 0x30FFFFFF);
		Smooth.icon(ui.d, "grid", x + 7, y + 4, 6, accent());
		ui.title(label, x + 17, y + 4, hover ? 0xFFFFFFFF : ROW_TEXT);
		Minecraft mc = Minecraft.getInstance();
		ui.hit(x, y, x + w, y + h, () -> mc.setScreen(new ModMenuScreen(this).openTab(ModMenuScreen.Tab.PROFILES)));
	}

	/** The search bar: a dark pill with a magnifier, like 67's. */
	private void drawSearch(Ui ui) {
		int w = Math.min(150, vw - 20);
		int h = 14;
		int x = (vw - w) / 2;
		int y = 6;
		boolean focused = editingText == null;
		Smooth.roundRect(ui.d, x, y, x + w, y + h, h / 2f, PANEL);
		Smooth.roundOutline(ui.d, x, y, x + w, y + h, h / 2f, 0.5f, focused && !search.isEmpty() ? MenuTheme.alpha(accent(), 0xC0) : 0x30FFFFFF);
		Smooth.icon(ui.d, "search", x + 6, y + 3.5f, 7, search.isEmpty() ? MUTED : accent());
		int tx = x + 17;
		int max = w - 24;
		String shown = search.isEmpty() ? "SEARCH MODULES..." : search.toUpperCase(Locale.ROOT);
		while (!shown.isEmpty() && ui.width(shown) > max) shown = shown.substring(1);
		ui.text(shown, tx, y + 4, search.isEmpty() ? MUTED : 0xFFFFFFFF);
		if (focused && !search.isEmpty() && (System.currentTimeMillis() / 500) % 2 == 0) {
			int cx = tx + ui.width(shown) + 1;
			ui.d.fill(cx, y + 3, cx + 1, y + h - 3, 0xFFFFFFFF);
		}
		ui.hit(x, y, x + w, y + h, () -> editingText = null);
	}

	private void drawPanel(Ui ui, String key) {
		int[] spot = spot(key);
		boolean folded = spot[2] != 0;
		int x = Math.max(0, Math.min(spot[0], vw - PANEL_W));
		int y = Math.max(TOP, Math.min(spot[1], vh - HEADER_H - 8));
		List<Module> modules = modules(key);
		List<ClientAction> actions = CLIENT.equals(key) ? clientActions() : List.of();

		int contentH = 0;
		if (!folded) {
			if (CLIENT.equals(key)) contentH = Math.max(1, actions.size()) * ROW_H;
			else if (THEMES.equals(key)) contentH = (Accent.PRESETS.size() + 1) * ROW_H;
			else {
				for (Module module : modules) contentH += ROW_H + (expanded.contains(module) ? settingsHeight(module) : 0);
				if (modules.isEmpty()) contentH = ROW_H;
			}
			contentH += 8;
		}
		int maxBody = Math.max(ROW_H * 2, vh - y - HEADER_H - 28);
		int bodyH = Math.min(contentH, maxBody);
		int maxScroll = Math.max(0, contentH - bodyH);
		double scroll = Math.max(0, Math.min(scrolls.getOrDefault(key, 0.0), maxScroll));
		scrolls.put(key, scroll);
		int bottom = y + HEADER_H + bodyH;
		boxes.put(key, new int[] {x, y, x + PANEL_W, bottom});

		Smooth.roundRect(ui.d, x, y, x + PANEL_W, bottom, RADIUS, PANEL);
		if (dragging != null && dragging.equals(key)) {
			Smooth.roundOutline(ui.d, x, y, x + PANEL_W, bottom, RADIUS, 1, MenuTheme.alpha(accent(), 0xA0));
		}
		ui.d.fill(x + 6, y + HEADER_H - 1, x + PANEL_W - 6, y + HEADER_H, MenuTheme.alpha(accent(), 0x90));
		Smooth.icon(ui.d, icon(key), x + 7, y + 6, 8, accent());
		ui.textScale(TITLE);
		ui.title(title(key), x + 19, y + 7, 0xFFFFFFFF);
		ui.textScale(TEXT);
		boolean arrowHover = ui.hover(x + PANEL_W - 18, y + 2, x + PANEL_W - 3, y + HEADER_H - 2);
		Smooth.icon(ui.d, folded ? "chevron_right" : "chevron_down", x + PANEL_W - 15, y + 6, 8, arrowHover ? accent() : MUTED);
		ui.hit(x, y, x + PANEL_W, y + HEADER_H, () -> startDrag(key, x, y));
		ui.hit(x + PANEL_W - 18, y + 2, x + PANEL_W - 3, y + HEADER_H - 2, () -> {
			spot[2] = folded ? 0 : 1;
			Config.saveSoon();
		});
		if (folded) return;

		int top = y + HEADER_H + 4;
		ui.clip(x + 1, y + HEADER_H, x + PANEL_W - 1, bottom - 2);
		int ry = top - (int) Math.round(scroll);
		if (THEMES.equals(key)) {
			for (Accent.Preset preset : Accent.PRESETS) {
				themeRow(ui, x, ry, preset);
				ry += ROW_H;
			}
			rainbowRow(ui, x, ry);
		} else if (CLIENT.equals(key)) {
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
			int trackY = y + HEADER_H + 3;
			int trackH = bodyH - 6;
			int thumb = Math.max(12, trackH * trackH / Math.max(1, contentH));
			int thumbY = trackY + (int) Math.round((trackH - thumb) * (scroll / maxScroll));
			Smooth.roundRect(ui.d, x + PANEL_W - 3, thumbY, x + PANEL_W - 1.5f, thumbY + thumb, 0.75f, MenuTheme.alpha(accent(), 0xB0));
		}
	}

	private void emptyRow(Ui ui, int x, int y) {
		ui.text("NO MATCHES", x + 10, y + 4, MUTED);
	}

	/** A colour in the Themes panel: its dot, its name, and a ring round the dot when it's picked. */
	private void themeRow(Ui ui, int x, int y, Accent.Preset preset) {
		int x1 = x + 4;
		int x2 = x + PANEL_W - 4;
		int h = ROW_H - 2;
		boolean picked = !Modules.THEME_RAINBOW.get() && (Modules.THEME_PANEL_COLOR.get() | 0xFF000000) == preset.color();
		boolean hover = ui.hover(x1, y, x2, y + h);
		if (picked || hover) Smooth.roundRect(ui.d, x1, y, x2, y + h, 3, picked ? Draw.mix(PANEL | 0xFF000000, preset.color(), 0.22) : 0x14FFFFFF);
		float cy = y + h / 2f;
		if (picked) Smooth.ring(ui.d, x1 + 8, cy, 4.2f, 0.8f, 0xFFFFFFFF);
		Smooth.circle(ui.d, x1 + 8, cy, 2.6f, preset.color());
		ui.title(preset.name(), x1 + 16, y + 4, picked ? 0xFFFFFFFF : ROW_TEXT);
		ui.hit(x1, y, x2, y + h, () -> {
			Modules.THEME_RAINBOW.set(false);
			Modules.THEME_PANEL_COLOR.set(preset.color());
		});
	}

	private void rainbowRow(Ui ui, int x, int y) {
		int x1 = x + 4;
		int x2 = x + PANEL_W - 4;
		int h = ROW_H - 2;
		boolean picked = Modules.THEME_RAINBOW.get();
		boolean hover = ui.hover(x1, y, x2, y + h);
		if (picked || hover) Smooth.roundRect(ui.d, x1, y, x2, y + h, 3, picked ? Draw.mix(PANEL | 0xFF000000, Accent.get(), 0.22) : 0x14FFFFFF);
		float cy = y + h / 2f;
		if (picked) Smooth.ring(ui.d, x1 + 8, cy, 4.2f, 0.8f, 0xFFFFFFFF);
		Smooth.circle(ui.d, x1 + 8, cy, 2.6f, Accent.rainbow((System.currentTimeMillis() % 3000L) / 3000f));
		ui.title("Rainbow", x1 + 16, y + 4, picked ? 0xFFFFFFFF : ROW_TEXT);
		ui.hit(x1, y, x2, y + h, () -> Modules.THEME_RAINBOW.toggle());
	}

	private void moduleRow(Ui ui, Module module, int x, int y) {
		boolean on = module.enabled();
		boolean toggleable = module.toggleable();
		boolean open = expanded.contains(module);
		int x1 = x + 4;
		int x2 = x + PANEL_W - 4;
		int h = ROW_H - 2;
		boolean hover = ui.hover(x1, y, x2, y + h);
		if (toggleable && on) {
			Smooth.roundRect(ui.d, x1, y, x2, y + h, 3, hover ? Draw.mix(onFill(), 0xFFFFFFFF, 0.06) : onFill());
		} else if (hover || open) {
			Smooth.roundRect(ui.d, x1, y, x2, y + h, 3, 0x14FFFFFF);
		}
		String name = ui.trimTitle(module.name, x2 - x1 - 18);
		ui.title(name, x1 + 6, y + 4, toggleable && on ? onText() : ROW_TEXT);
		float cy = y + h / 2f;
		if (toggleable) {
			if (on) Smooth.circle(ui.d, x2 - 6, cy, 1.7f, onDot());
			else Smooth.circle(ui.d, x2 - 6, cy, 1.3f, 0xFF4A4F5A);
		} else {
			Smooth.icon(ui.d, open ? "chevron_down" : "chevron_right", x2 - 10, cy - 3, 6, MUTED);
		}
		Runnable toggleOpen = () -> {
			if (!expanded.remove(module)) expanded.add(module);
		};
		ui.hit(x1, y, x2, y + h, toggleable ? module::toggle : toggleOpen, toggleOpen);
	}

	/** The client's colour (the Themes panel). */
	private static int accent() {
		return Accent.get();
	}

	/** A switched-on row: the colour, deep and muted against the panel, like 67's. */
	private static int onFill() {
		return Draw.mix(PANEL | 0xFF000000, accent(), 0.34);
	}

	/** Text on a switched-on row: a light tint of the colour. */
	private static int onText() {
		return Draw.mix(accent(), 0xFFFFFFFF, 0.55);
	}

	/** The dot on a switched-on row. */
	private static int onDot() {
		return Draw.mix(accent(), 0xFFFFFFFF, 0.25);
	}

	private void actionRow(Ui ui, int x, int y, String label, Runnable action) {
		int x1 = x + 4;
		int x2 = x + PANEL_W - 4;
		int h = ROW_H - 2;
		boolean hover = ui.hover(x1, y, x2, y + h);
		if (hover) Smooth.roundRect(ui.d, x1, y, x2, y + h, 3, 0x14FFFFFF);
		ui.title(ui.trimTitle(label, x2 - x1 - 18), x1 + 6, y + 4, hover ? 0xFFFFFFFF : ROW_TEXT);
		Smooth.icon(ui.d, "chevron_right", x2 - 10, y + h / 2f - 3, 6, hover ? accent() : MUTED);
		ui.hit(x1, y, x2, y + h, action);
	}

	private boolean hasWidget(Module module) {
		return HudWidgets.ALL.stream().anyMatch(widget -> widget.module == module);
	}

	private static final int LINE_H = 12;

	private int settingHeight(Setting setting) {
		if (setting instanceof Setting.Slider) return 17;
		if (setting instanceof Setting.Choice choice) return choiceFits(choice) ? 19 : LINE_H;
		if (setting instanceof Setting.Color) return 18;
		if (setting instanceof Setting.Text) return 22;
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
		for (String option : choice.options()) total += Math.round(Minecraft.getInstance().font.width(UiFont.semibold(option.toUpperCase(Locale.ROOT))) * TEXT) + 9;
		return total <= PANEL_W - 22;
	}

	private void drawSettings(Ui ui, Module module, int x, int y) {
		int h = settingsHeight(module);
		ui.d.fill(x + 9, y + 1, x + 10, y + h - 3, 0x40FFFFFF);
		int left = x + 14;
		int right = x + PANEL_W - 8;
		int sy = y + 2;
		for (Setting setting : module.settings()) {
			drawSetting(ui, setting, left, right, sy);
			sy += settingHeight(setting);
		}
		Minecraft mc = Minecraft.getInstance();
		if (hasWidget(module)) {
			linkRow(ui, left, right, sy, "Move on screen", () -> mc.setScreen(new HubScreen(this).editOnly()));
			sy += LINE_H;
		}
		if (MORE_OPTIONS.contains(module)) {
			linkRow(ui, left, right, sy, "More options", () -> mc.setScreen(new ModMenuScreen(this).openModule(module)));
			sy += LINE_H;
		}
		if (module.settings().isEmpty() && !hasWidget(module) && !MORE_OPTIONS.contains(module)) {
			ui.text("NO SETTINGS", left, sy + 3, MUTED);
		}
	}

	private void linkRow(Ui ui, int left, int right, int y, String label, Runnable action) {
		boolean hover = ui.hover(left - 2, y, right + 2, y + LINE_H - 1);
		ui.text(ui.trim(label.toUpperCase(Locale.ROOT), right - left - 10), left, y + 3, hover ? accent() : MUTED);
		Smooth.icon(ui.d, "chevron_right", right - 6, y + 3, 6, hover ? accent() : MUTED);
		ui.hit(left - 2, y, right + 2, y + LINE_H - 1, action);
	}

	private void drawSetting(Ui ui, Setting setting, int left, int right, int y) {
		String label = setting.name.toUpperCase(Locale.ROOT);
		if (setting instanceof Setting.Toggle toggle) {
			ui.text(ui.trim(label, right - left - 12), left, y + 3, MUTED);
			checkbox(ui.d, right - 7, y + 2.5f, toggle.get());
			ui.hit(left - 2, y, right + 2, y + LINE_H - 1, toggle::toggle);
		} else if (setting instanceof Setting.Slider slider) {
			String value = slider.format();
			int valueW = ui.width(value);
			ui.text(ui.trim(label, right - left - valueW - 6), left, y + 2, MUTED);
			ui.text(value, right - valueW, y + 2, ROW_TEXT);
			int trackY = y + 11;
			int trackW = right - left;
			Smooth.roundRect(ui.d, left, trackY, right, trackY + 2, 1, BOX_OFF);
			float filled = (float) (trackW * slider.fraction());
			if (filled > 0) Smooth.roundRect(ui.d, left, trackY, left + filled, trackY + 2, 1, 0xFFFFFFFF);
			Smooth.circle(ui.d, left + filled, trackY + 1, 2.4f, 0xFFFFFFFF);
			ui.hit(left - 2, trackY - 4, right + 2, trackY + 6, () -> {
				draggingSlider = slider;
				sliderX = left;
				sliderW = trackW;
				slider.setFraction((ui.mouseX - left) / (double) trackW);
			});
		} else if (setting instanceof Setting.Choice choice) {
			if (choiceFits(choice)) {
				ui.text(ui.trim(label, right - left), left, y + 2, MUTED);
				int ox = left;
				for (String option : choice.options()) {
					String text = option.toUpperCase(Locale.ROOT);
					int ow = ui.width(text) + 6;
					boolean active = choice.is(option);
					boolean hover = ui.hover(ox, y + 9, ox + ow, y + 18);
					Smooth.roundRect(ui.d, ox, y + 9, ox + ow, y + 18, 2, active ? onFill() : hover ? 0x24FFFFFF : 0x12FFFFFF);
					ui.text(text, ox + 3, y + 11, active ? onText() : hover ? 0xFFFFFFFF : MUTED);
					ui.hit(ox, y + 9, ox + ow, y + 18, () -> choice.set(option));
					ox += ow + 2;
				}
			} else {
				String value = choice.get().toUpperCase(Locale.ROOT);
				int valueW = ui.width(value);
				ui.text(ui.trim(label, right - left - valueW - 12), left, y + 3, MUTED);
				boolean hover = ui.hover(right - valueW - 8, y, right + 2, y + LINE_H - 1);
				ui.text(value, right - valueW - 7, y + 3, hover ? accent() : ROW_TEXT);
				Smooth.icon(ui.d, "chevron_right", right - 5, y + 3, 6, hover ? accent() : MUTED);
				ui.hit(left - 2, y, right + 2, y + LINE_H - 1, choice::next, choice::previous);
			}
		} else if (setting instanceof Setting.Color color) {
			ui.text(ui.trim(label, right - left), left, y + 2, MUTED);
			int sx = left;
			for (int swatch : Setting.Color.SWATCHES) {
				boolean selected = (color.get() | 0xFF000000) == swatch;
				if (selected) Smooth.circle(ui.d, sx + 3, y + 13, 4.2f, 0xFFFFFFFF);
				Smooth.circle(ui.d, sx + 3, y + 13, 3, swatch);
				int pick = swatch;
				ui.hit(sx, y + 9, sx + 6, y + 17, () -> color.set(pick));
				sx += 9;
				if (sx + 6 > right) break;
			}
		} else if (setting instanceof Setting.Text text) {
			ui.text(ui.trim(label, right - left), left, y + 2, MUTED);
			boolean focused = editingText == text;
			int fy = y + 10;
			Smooth.roundRect(ui.d, left, fy, right, fy + 10, 2, BOX_OFF);
			if (focused) Smooth.roundOutline(ui.d, left, fy, right, fy + 10, 2, 0.5f, accent());
			String value = text.get();
			String shown = value.isEmpty() && !focused ? "Type here" : value;
			while (!shown.isEmpty() && ui.width(shown) > right - left - 6) shown = shown.substring(1);
			ui.text(shown, left + 3, fy + 3, value.isEmpty() ? MUTED : 0xFFFFFFFF);
			if (focused && (System.currentTimeMillis() / 500) % 2 == 0) {
				int cx = left + 3 + ui.width(value.isEmpty() ? "" : shown) + 1;
				ui.d.fill(cx, fy + 2, cx + 1, fy + 8, 0xFFFFFFFF);
			}
			ui.hit(left, fy, right, fy + 10, () -> editingText = text);
		}
	}

	/** A small check box, like 67's: the client colour with a white tick when on, dark with an x when off. */
	private static void checkbox(Draw d, float x, float y, boolean on) {
		Smooth.roundRect(d, x, y, x + 7, y + 7, 1.6f, on ? accent() : BOX_OFF);
		Smooth.icon(d, on ? "check" : "cross", x, y, 7, on ? 0xFFFFFFFF : MUTED);
	}

	private void startDrag(String key, int x, int y) {
		dragging = key;
		grabX = (int) Math.round(lastMouseX) - x;
		grabY = (int) Math.round(lastMouseY) - y;
	}

	/** Where the mouse was last pressed, in layout units. */
	private double lastMouseX;
	private double lastMouseY;

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mx = event.x() / unit;
		double my = event.y() / unit;
		lastMouseX = mx;
		lastMouseY = my;
		int button = event.button();
		if (button == 0 || button == 1) {
			for (int i = hits.size() - 1; i >= 0; i--) {
				Ui.Hit hit = hits.get(i);
				if (!hit.contains(mx, my)) continue;
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
		double mx = event.x() / unit;
		double my = event.y() / unit;
		if (draggingSlider != null) {
			draggingSlider.setFraction((mx - sliderX) / sliderW);
			return true;
		}
		if (dragging != null) {
			int[] spot = spot(dragging);
			spot[0] = Math.max(0, Math.min(vw - PANEL_W, (int) Math.round(mx) - grabX));
			spot[1] = Math.max(TOP, Math.min(vh - HEADER_H - 8, (int) Math.round(my) - grabY));
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
		double mx = x / unit;
		double my = y / unit;
		for (Map.Entry<String, int[]> entry : boxes.entrySet()) {
			int[] box = entry.getValue();
			if (mx < box[0] || mx >= box[2] || my < box[1] || my >= box[3]) continue;
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
