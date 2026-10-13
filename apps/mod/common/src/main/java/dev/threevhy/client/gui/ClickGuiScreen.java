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
 * nothing is pixelated.
 */
public final class ClickGuiScreen extends CompatScreen {
	/** How many layout units tall the window is: 540 makes a unit two pixels on a 1080p screen. */
	private static final float UNITS_PER_SCREEN = 540f;
	private static final int PANEL_W = 160;
	private static final int HEADER_H = 26;
	private static final int ROW_H = 19;
	private static final int RADIUS = 8;
	/** Saved panel spots from before this layout are laid out again. */
	private static final int LAYOUT = 3;
	private static final int TOP = 36;
	private static final int PANEL = 0xF224262B;
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

	private static String icon(String key) {
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
		int columns = Math.max(1, Math.min(keys.size(), (vw - 16) / (PANEL_W + 12)));
		int left = Math.max(8, (vw - columns * (PANEL_W + 12) + 12) / 2);
		int[] bottoms = new int[columns];
		java.util.Arrays.fill(bottoms, TOP);
		for (String key : keys) {
			int column = 0;
			for (int i = 1; i < columns; i++) if (bottoms[i] < bottoms[column]) column = i;
			int[] spot = Config.get().panels.get(key);
			if (spot == null || spot.length < 4 || spot[3] != LAYOUT) {
				spot = new int[] {left + column * (PANEL_W + 12), bottoms[column], 0, LAYOUT};
				Config.get().panels.put(key, spot);
			}
			bottoms[column] = spot[1] + HEADER_H + defaultHeight(key) + 12;
		}
	}

	private int defaultHeight(String key) {
		return (CLIENT.equals(key) ? clientActions().size() : modules(key).size()) * ROW_H + 10;
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
		Ui ui = new Ui(d, Math.round(mouseX / unit), Math.round(mouseY / unit), hits).cleanFont(UiFont.clean());
		float intro = Math.min(1f, (System.currentTimeMillis() - openedAt) / 200f);
		d.translate(0, (1 - (1 - (1 - intro) * (1 - intro))) * -6);
		drawSearch(ui);
		List<String> keys = panelKeys();
		// The panel being dragged is drawn last, on top
		if (dragging != null && keys.remove(dragging)) keys.add(dragging);
		for (String key : keys) drawPanel(ui, key);
		d.pop();
	}

	/** The search bar: a dark pill with a magnifier, like 67's. */
	private void drawSearch(Ui ui) {
		int w = Math.min(230, vw - 20);
		int h = 20;
		int x = (vw - w) / 2;
		int y = 8;
		boolean focused = editingText == null;
		Smooth.roundRect(ui.d, x, y, x + w, y + h, h / 2f, PANEL);
		Smooth.roundOutline(ui.d, x, y, x + w, y + h, h / 2f, 1, focused && !search.isEmpty() ? MenuTheme.alpha(accent(), 0xC0) : 0x30FFFFFF);
		Smooth.icon(ui.d, "search", x + 8, y + 5, 10, search.isEmpty() ? MUTED : accent());
		int tx = x + 24;
		int max = w - 32;
		String shown = search.isEmpty() ? "SEARCH MODULES..." : search.toUpperCase(Locale.ROOT);
		while (!shown.isEmpty() && ui.width(shown) > max) shown = shown.substring(1);
		ui.text(shown, tx, y + 6, search.isEmpty() ? MUTED : 0xFFFFFFFF);
		if (focused && !search.isEmpty() && (System.currentTimeMillis() / 500) % 2 == 0) {
			int cx = tx + ui.width(shown) + 1;
			ui.d.fill(cx, y + 5, cx + 1, y + h - 5, 0xFFFFFFFF);
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
			else {
				for (Module module : modules) contentH += ROW_H + (expanded.contains(module) ? settingsHeight(module) : 0);
				if (modules.isEmpty()) contentH = ROW_H;
			}
			contentH += 10;
		}
		int maxBody = Math.max(ROW_H * 2, vh - y - HEADER_H - 8);
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
		ui.d.fill(x + 10, y + HEADER_H - 3, x + PANEL_W - 10, y + HEADER_H - 2, accent());
		Smooth.icon(ui.d, icon(key), x + 10, y + 7, 11, accent());
		ui.title(title(key), x + 27, y + 9, 0xFFFFFFFF);
		boolean arrowHover = ui.hover(x + PANEL_W - 24, y + 3, x + PANEL_W - 4, y + HEADER_H - 4);
		Smooth.icon(ui.d, folded ? "chevron_right" : "chevron_down", x + PANEL_W - 21, y + 7, 11, arrowHover ? accent() : MUTED);
		ui.hit(x, y, x + PANEL_W, y + HEADER_H, () -> startDrag(key, x, y));
		ui.hit(x + PANEL_W - 24, y + 3, x + PANEL_W - 4, y + HEADER_H - 4, () -> {
			spot[2] = folded ? 0 : 1;
			Config.saveSoon();
		});
		if (folded) return;

		int top = y + HEADER_H + 5;
		ui.clip(x + 1, y + HEADER_H, x + PANEL_W - 1, bottom - 3);
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
			int trackY = y + HEADER_H + 3;
			int trackH = bodyH - 8;
			int thumb = Math.max(14, trackH * trackH / Math.max(1, contentH));
			int thumbY = trackY + (int) Math.round((trackH - thumb) * (scroll / maxScroll));
			Smooth.roundRect(ui.d, x + PANEL_W - 4, thumbY, x + PANEL_W - 2, thumbY + thumb, 1, MenuTheme.alpha(accent(), 0xC0));
		}
	}

	private void emptyRow(Ui ui, int x, int y) {
		ui.text("NO MATCHES", x + 16, y + 5, MUTED);
	}

	private void moduleRow(Ui ui, Module module, int x, int y) {
		boolean on = module.enabled();
		boolean toggleable = module.toggleable();
		boolean open = expanded.contains(module);
		int x1 = x + 8;
		int x2 = x + PANEL_W - 8;
		int h = ROW_H - 3;
		boolean hover = ui.hover(x1, y, x2, y + h);
		if (toggleable && on) {
			Smooth.roundRect(ui.d, x1, y, x2, y + h, 5, hover ? Draw.mix(onFill(), 0xFFFFFFFF, 0.1) : onFill());
		} else if (hover || open) {
			Smooth.roundRect(ui.d, x1, y, x2, y + h, 5, 0x18FFFFFF);
		}
		String name = ui.trimTitle(module.name, x2 - x1 - 24);
		ui.title(name, x1 + 8, y + (h - 8) / 2 + 1, toggleable && on ? onText() : ROW_TEXT);
		float cy = y + h / 2f;
		if (toggleable) {
			if (on) Smooth.circle(ui.d, x2 - 10, cy, 2.2f, 0xF0FFFFFF);
			else Smooth.circle(ui.d, x2 - 10, cy, 1.6f, 0x66FFFFFF);
		} else {
			Smooth.icon(ui.d, open ? "chevron_down" : "chevron_right", x2 - 15, cy - 4.5f, 9, MUTED);
		}
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
		int x1 = x + 8;
		int x2 = x + PANEL_W - 8;
		int h = ROW_H - 3;
		boolean hover = ui.hover(x1, y, x2, y + h);
		if (hover) Smooth.roundRect(ui.d, x1, y, x2, y + h, 5, 0x18FFFFFF);
		ui.title(ui.trimTitle(label, x2 - x1 - 24), x1 + 8, y + (h - 8) / 2 + 1, hover ? 0xFFFFFFFF : ROW_TEXT);
		Smooth.icon(ui.d, "chevron_right", x2 - 15, y + h / 2f - 4.5f, 9, hover ? accent() : MUTED);
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
		for (String option : choice.options()) total += Minecraft.getInstance().font.width(UiFont.semibold(option.toUpperCase(Locale.ROOT))) + 11;
		return total <= PANEL_W - 32;
	}

	private void drawSettings(Ui ui, Module module, int x, int y) {
		int h = settingsHeight(module);
		ui.d.fill(x + 14, y + 1, x + 15, y + h - 3, 0x50FFFFFF);
		int left = x + 21;
		int right = x + PANEL_W - 12;
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
			ui.text("NO SETTINGS", left, sy + 4, MUTED);
		}
	}

	private void linkRow(Ui ui, int left, int right, int y, String label, Runnable action) {
		boolean hover = ui.hover(left - 2, y, right + 2, y + LINE_H - 1);
		ui.text(ui.trim(label.toUpperCase(Locale.ROOT), right - left - 12), left, y + 4, hover ? accent() : ROW_TEXT);
		Smooth.icon(ui.d, "chevron_right", right - 8, y + 3.5f, 9, hover ? accent() : MUTED);
		ui.hit(left - 2, y, right + 2, y + LINE_H - 1, action);
	}

	private void drawSetting(Ui ui, Setting setting, int left, int right, int y) {
		String label = setting.name.toUpperCase(Locale.ROOT);
		if (setting instanceof Setting.Toggle toggle) {
			ui.text(ui.trim(label, right - left - 16), left, y + 4, ROW_TEXT);
			checkbox(ui.d, right - 11, y + 2.5f, toggle.get());
			ui.hit(left - 2, y, right + 2, y + LINE_H - 1, toggle::toggle);
		} else if (setting instanceof Setting.Slider slider) {
			String value = slider.format();
			int valueW = ui.width(value);
			ui.text(ui.trim(label, right - left - valueW - 6), left, y + 2, ROW_TEXT);
			ui.text(value, right - valueW, y + 2, MUTED);
			int trackY = y + 15;
			int trackW = right - left;
			Smooth.roundRect(ui.d, left, trackY, right, trackY + 3, 1.5f, BOX_OFF);
			float filled = (float) (trackW * slider.fraction());
			if (filled > 0) Smooth.roundRect(ui.d, left, trackY, left + filled, trackY + 3, 1.5f, accent());
			Smooth.circle(ui.d, left + filled, trackY + 1.5f, 3.5f, 0xFFFFFFFF);
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
					Smooth.roundRect(ui.d, ox, y + 12, ox + ow, y + 24, 3, active ? onFill() : hover ? 0x24FFFFFF : 0x12FFFFFF);
					ui.text(text, ox + 4, y + 14, active ? onText() : hover ? 0xFFFFFFFF : MUTED);
					ui.hit(ox, y + 12, ox + ow, y + 24, () -> choice.set(option));
					ox += ow + 3;
				}
			} else {
				String value = choice.get().toUpperCase(Locale.ROOT);
				int valueW = ui.width(value);
				ui.text(ui.trim(label, right - left - valueW - 14), left, y + 4, ROW_TEXT);
				boolean hover = ui.hover(right - valueW - 10, y, right + 2, y + LINE_H - 1);
				ui.text(value, right - valueW - 9, y + 4, hover ? accent() : MUTED);
				Smooth.icon(ui.d, "chevron_right", right - 7, y + 3.5f, 9, hover ? accent() : MUTED);
				ui.hit(left - 2, y, right + 2, y + LINE_H - 1, choice::next, choice::previous);
			}
		} else if (setting instanceof Setting.Color color) {
			ui.text(ui.trim(label, right - left), left, y + 2, ROW_TEXT);
			int sx = left;
			for (int swatch : Setting.Color.SWATCHES) {
				boolean selected = (color.get() | 0xFF000000) == swatch;
				if (selected) Smooth.circle(ui.d, sx + 4, y + 17, 5.5f, 0xFFFFFFFF);
				Smooth.circle(ui.d, sx + 4, y + 17, 4, swatch);
				int pick = swatch;
				ui.hit(sx, y + 13, sx + 8, y + 21, () -> color.set(pick));
				sx += 12;
				if (sx + 8 > right) break;
			}
		} else if (setting instanceof Setting.Text text) {
			ui.text(ui.trim(label, right - left), left, y + 2, ROW_TEXT);
			boolean focused = editingText == text;
			int fy = y + 13;
			Smooth.roundRect(ui.d, left, fy, right, fy + 14, 3, BOX_OFF);
			if (focused) Smooth.roundOutline(ui.d, left, fy, right, fy + 14, 3, 1, accent());
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

	/** A check box: filled with the panel colour and ticked when on, dark with an x when off. */
	private static void checkbox(Draw d, float x, float y, boolean on) {
		Smooth.roundRect(d, x, y, x + 11, y + 11, 3, on ? accent() : BOX_OFF);
		Smooth.icon(d, on ? "check" : "cross", x, y, 11, on ? 0xFF101318 : MUTED);
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
