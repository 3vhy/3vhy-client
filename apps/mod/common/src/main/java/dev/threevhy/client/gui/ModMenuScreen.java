package dev.threevhy.client.gui;

import dev.threevhy.client.Config;
import dev.threevhy.client.Config.Waypoint;
import dev.threevhy.client.Draw;
import dev.threevhy.client.ThreevhyClient;
import dev.threevhy.client.compat.CompatScreen;
import dev.threevhy.client.hud.HudRenderer;
import dev.threevhy.client.hud.HudWidget;
import dev.threevhy.client.hud.HudWidgets;
import dev.threevhy.client.module.Module;
import dev.threevhy.client.module.Module.Category;
import dev.threevhy.client.module.Modules;
import dev.threevhy.client.module.CustomSkins;
import dev.threevhy.client.module.PlayerList;
import dev.threevhy.client.module.TextHotkeys;
import dev.threevhy.client.module.TextRules;
import dev.threevhy.client.module.Setting;
import dev.threevhy.client.module.Waypoints;
import dev.threevhy.client.music.MusicBridge;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.UUIDUtil;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

/**
 * The 3vhy mod menu in its Classic style, laid out like Lunar's: the 3vhy name and MODS / SETTINGS /
 * WAYPOINTS tabs along the top, your profiles down the left with Save as new profile and Edit HUD
 * layout, filter chips and a search box, and a grid of three mod cards a row, each with an OPTIONS bar
 * and an ENABLED / DISABLED bar. The other style, Panels, is {@link ClickGuiScreen}.
 *
 * <p>Everything is measured off Lunar's menu and laid out in layout units, 0.6 of a GUI pixel at 100%
 * HUD size, so the menu is Lunar's size next to the HUD (329 by 202 GUI pixels) and grows and shrinks
 * with it; it only gets smaller to fit a small window. Round shapes are drawn by {@link Smooth}, so
 * they stay clean at any scale.
 */
public final class ModMenuScreen extends CompatScreen {
	public enum Tab {
		MODS,
		SETTINGS,
		WAYPOINTS,
		/** The mods tab with the new profile name ready to type (profiles live in the sidebar). */
		PROFILES
	}

	private static final Tab[] SHOWN_TABS = {Tab.MODS, Tab.SETTINGS, Tab.WAYPOINTS};
	/** GUI pixels per layout unit at 100% HUD size. */
	private static final float UNIT = 0.6f;
	private static final int PANEL_W = 548;
	private static final int PANEL_H = 337;
	private static final int HEADER_H = 36;
	private static final int SIDEBAR_W = 117;
	/** Where the chips (or a page) start, down from the panel's top, and the content's left edge. */
	private static final int BODY_TOP = 48;
	private static final int CONTENT_X = 123;
	private static final int CONTENT_W = 401;
	private static final int CHIP_H = 16;
	private static final int CARDS_TOP = 71;
	private static final int CARD_GAP_X = 10;
	private static final int CARD_GAP_Y = 8;
	/** The sizes of Lunar's labels: capitals 6 units tall, with room between the letters. */
	private static final float LABEL = 0.9f;
	private static final float LABEL_SPACING = 2.2f;
	private static final int BLUE_GREY = 0xFF3B4F71;
	private static final int OLIVE = 0xFF4B6B34;
	private static final int ON_GREEN = 0xFF2FA068;
	private static final int OFF_RED = 0xFFA52D51;
	private static final int LOCKED = 0xFF8A8F99;

	/** Lunar's colours for the dark theme, and the light theme's own. */
	private record Look(int panel, int edge, int line, int sidebar, int row, int rowOn, int card, int cardHover, int cardEdge,
			int bar, int barHover, int text, int label, int muted, int chipText) {
		static Look of(MenuTheme t) {
			if (t.isLight()) {
				return new Look(t.panel(), t.edge(), t.edge(), MenuTheme.alpha(0x1A4FBA, 0x10), MenuTheme.alpha(0xFFFFFF, 0x80),
						t.cardOn(), t.card(), t.cardHover(), t.edge(), MenuTheme.alpha(0x1A4FBA, 0x14), MenuTheme.alpha(0x1A4FBA, 0x26),
						t.text(), t.text(), t.muted(), 0xFFFFFFFF);
			}
			return new Look(0xF2191C23, 0x2EFFFFFF, 0x1AFFFFFF, 0x24000000, 0x0FFFFFFF, 0x24FFFFFF, 0x12FFFFFF, 0x1CFFFFFF,
					0x1FFFFFFF, 0x17FFFFFF, 0x2BFFFFFF, 0xFFDCE0E6, 0xFFC4C9D1, 0xFF8E949E, 0xFFBACBE0);
		}
	}

	private enum Focus {
		SEARCH,
		PROFILE_NAME,
		WAYPOINT_NAME,
		SETTING_TEXT,
		PLAYER_NAME,
		RULE_FIND,
		RULE_REPLACE,
		HOTKEY_TEXT,
		/** Waiting for the key (or extra mouse button) a new Text Hotkey goes on. */
		HOTKEY_KEY
	}

	private static final int CARD_W = 127;
	private static final int CARD_H = 126;
	private static final int ROW_H = 26;

	private final Screen parent;
	private final List<Ui.Hit> hits = Ui.newHits();
	private final Snow snow = new Snow();
	private final long openedAt = System.currentTimeMillis();

	private Tab tab = Tab.MODS;
	private Category category = null;
	private boolean newOnly = false;
	private Module page = null;
	private Focus focus = Focus.SEARCH;
	private String search = "";
	private String profileName = "";
	private Waypoint renaming = null;
	private String waypointName = "";
	private Setting.Text editingText = null;
	private String playerName = "";
	private String ruleFind = "";
	private String ruleReplace = "";
	private String hotkeyText = "";
	private int hotkeyKey = -1;
	private boolean hotkeyMouse = false;
	private long hotkeyPickedAt = 0;
	private String notice = null;
	private long noticeAt = 0;

	private double scroll = 0;
	private double shownScroll = 0;
	private int maxScroll = 0;
	private long lastFrame = 0;

	private Setting.Slider draggingSlider = null;
	private int sliderX = 0;
	private int sliderW = 1;
	/** GUI pixels per layout unit, worked out each frame. */
	private float unit = UNIT;

	public ModMenuScreen(Screen parent) {
		super(Component.literal("3vhy Mod Menu"));
		this.parent = parent;
	}

	/** The mod menu in the style picked in Theme: Classic cards or floating Panels. */
	public static Screen create(Screen parent) {
		return Modules.THEME_MENU_STYLE.is("Panels") ? new ClickGuiScreen(parent) : new ModMenuScreen(parent);
	}

	/** Opens a mod's settings page. Returns this, for use when creating the screen. */
	public ModMenuScreen openModule(Module module) {
		if (module == Modules.PROFILES) return openTab(Tab.PROFILES);
		tab = Tab.MODS;
		page = module;
		resetScroll();
		return this;
	}

	public ModMenuScreen openTab(Tab next) {
		tab = next == Tab.PROFILES ? Tab.MODS : next;
		page = next == Tab.SETTINGS ? Modules.THEME : null;
		renaming = null;
		focus = next == Tab.PROFILES ? Focus.PROFILE_NAME : Focus.SEARCH;
		resetScroll();
		return this;
	}

	/** Back from a mod's page to the cards (the Settings tab has no cards, so it goes to the Mods tab). */
	private void leavePage() {
		if (tab == Tab.SETTINGS) {
			openTab(Tab.MODS);
			return;
		}
		page = null;
		resetScroll();
	}

	private void resetScroll() {
		scroll = 0;
		shownScroll = 0;
	}

	private void notice(String text) {
		notice = text;
		noticeAt = System.currentTimeMillis();
	}

	// Drawing

	@Override
	protected boolean blurBackground() {
		return false;
	}

	@Override
	protected boolean panoramaBackground() {
		return !TitleScreenStyle.drawsBackground();
	}

	@Override
	protected void drawBackdrop(Draw d) {
		HubScreen.drawLunarBackdrop(d, this.width, this.height, snow);
	}

	/** Lunar's size at the HUD size setting, made smaller only if the window can't fit it. */
	private void measure() {
		float fit = Math.min((this.width - 12) / (float) PANEL_W, (this.height - 12) / (float) PANEL_H);
		unit = Math.max(0.25f, Math.min(UNIT * HudRenderer.scale(), fit));
	}

	@Override
	protected void draw(Draw d, int mouseX, int mouseY, float delta) {
		hits.clear();
		long now = System.nanoTime();
		double seconds = lastFrame == 0 ? 0 : Math.min(0.1, (now - lastFrame) / 1_000_000_000.0);
		lastFrame = now;
		shownScroll += (scroll - shownScroll) * Math.min(1.0, seconds * 16.0);
		if (Math.abs(scroll - shownScroll) < 0.5) shownScroll = scroll;

		measure();
		int vw = Math.round(this.width / unit);
		int vh = Math.round(this.height / unit);
		d.push();
		d.scale(unit);
		Ui ui = new Ui(d, Math.round(mouseX / unit), Math.round(mouseY / unit), hits).cleanFont(UiFont.clean());
		Look look = Look.of(ui.t);
		int x0 = (vw - PANEL_W) / 2;
		int y0 = (vh - PANEL_H) / 2;

		float intro = Math.min(1f, (System.currentTimeMillis() - openedAt) / 200f);
		d.translate(0, (1 - (1 - (1 - intro) * (1 - intro))) * 10);

		ui.round(x0, y0, x0 + PANEL_W, y0 + PANEL_H, 5, look.panel());
		ui.ring(x0, y0, x0 + PANEL_W, y0 + PANEL_H, 5, look.edge());
		drawHeader(ui, look, x0, y0);
		d.fill(x0 + 1, y0 + HEADER_H, x0 + PANEL_W - 1, y0 + HEADER_H + 1, look.line());

		drawSidebar(ui, look, x0, y0 + HEADER_H + 1, PANEL_H - HEADER_H - 1);
		int cx = x0 + CONTENT_X;
		int cy = y0 + BODY_TOP;
		int cw = CONTENT_W + 8;
		int ch = PANEL_H - BODY_TOP - 6;
		switch (tab) {
			case MODS, PROFILES -> {
				if (page != null) drawPage(ui, cx, cy, cw, ch);
				else drawGrid(ui, look, cx, cy, CONTENT_W, ch);
			}
			case SETTINGS -> drawPage(ui, cx, cy, cw, ch);
			case WAYPOINTS -> drawWaypoints(ui, cx, cy, cw, ch);
		}
		drawNotice(ui, cx, y0 + PANEL_H - 22, cw);
		d.pop();
	}

	/** The 3vhy emblem and name, the tabs in the middle, and the theme switch and close button, measured off Lunar's. */
	private void drawHeader(Ui ui, Look look, int px, int y0) {
		Draw d = ui.d;
		float k = Smooth.density(d);
		int cell = Math.max(1, Math.round(19 * k / 16));
		int size = 16 * cell;
		d.push();
		d.scale(1f / k);
		HubScreen.drawEmblem(d, Math.round((px + 12) * k), Math.round((y0 + HEADER_H / 2f) * k - size / 2f), size, cell, 255, look.text());
		d.pop();
		float nameScale = 10 / (UiFont.clean() ? 6.8f : 7f);
		int brandW = Math.round(ui.boldWidth("3VHY") * nameScale);
		d.push();
		d.translate(px + 36, y0 + 13);
		d.scale(nameScale);
		d.text(UiFont.clean() ? UiFont.bold("3VHY") : Ui.bold("3VHY"), 0, 0, look.text(), false);
		d.pop();
		ui.small(" CLIENT", px + 36 + brandW, y0 + 13, nameScale, look.text());

		int tabH = 18;
		int pad = 9;
		int gap = 9;
		int total = -gap;
		for (Tab each : SHOWN_TABS) total += Math.round(ui.spacedWidth(each.name(), LABEL, LABEL_SPACING, true)) + 2 * pad + gap;
		int x = px + (PANEL_W - total) / 2;
		int y = y0 + 9;
		for (Tab each : SHOWN_TABS) {
			String label = each.name();
			int tw = Math.round(ui.spacedWidth(label, LABEL, LABEL_SPACING, true)) + 2 * pad;
			boolean active = tab == each || (each == Tab.MODS && tab == Tab.PROFILES);
			boolean hover = ui.hover(x, y, x + tw, y + tabH);
			if (active || hover) ui.round(x, y, x + tw, y + tabH, 2.5f, active ? look.rowOn() : look.row());
			ui.ring(x, y, x + tw, y + tabH, 2.5f, active ? look.edge() : look.line());
			ui.spaced(label, x + pad, y + (tabH - 6) / 2f, LABEL, LABEL_SPACING, true, hover ? look.text() : active ? look.label() : look.muted());
			ui.hit(x, y, x + tw, y + tabH, () -> openTab(each));
			x += tw + gap;
		}

		int right = px + PANEL_W - 10;
		int size2 = 18;
		boolean closeHover = ui.hover(right - size2, y, right, y + size2);
		ui.round(right - size2, y, right, y + size2, 2.5f, closeHover ? MenuTheme.alpha(ui.t.bad(), 0xB0) : look.bar());
		ui.ring(right - size2, y, right, y + size2, 2.5f, closeHover ? ui.t.bad() : look.line());
		Smooth.icon(d, "cross", right - size2 + 3, y + 3, size2 - 6, closeHover ? 0xFFFFFFFF : look.label());
		ui.hit(right - size2, y, right, y + size2, this::onClose);

		int themeX = right - size2 - 6 - size2;
		boolean light = ui.t.isLight();
		boolean hover = ui.hover(themeX, y, themeX + size2, y + size2);
		if (hover) ui.round(themeX, y, themeX + size2, y + size2, 2.5f, look.bar());
		ui.ring(themeX, y, themeX + size2, y + size2, 2.5f, look.line());
		PixelIcons.draw(d, light ? PixelIcons.MOON : PixelIcons.SUN, themeX + 3, y + 3, 1, hover ? ui.t.accent() : look.label());
		ui.hit(themeX, y, themeX + size2, y + size2, () -> Modules.THEME_MODE.next());
		if (hover) ui.tooltip(light ? "Dark theme" : "Light theme", themeX + size2 / 2, y + size2 + 3);
	}

	/**
	 * Profiles down the left, like Lunar's: a list of rows, the one in use lit up; click another to
	 * switch to it. The profile name box, Save as new profile and Edit HUD layout sit at the bottom.
	 */
	private void drawSidebar(Ui ui, Look look, int x0, int y, int h) {
		MenuTheme t = ui.t;
		int x = x0 + 1;
		int w = SIDEBAR_W;
		ui.d.fill(x, y, x + w, y + h - 5, look.sidebar());
		Config config = Config.get();
		int listTop = y + 10;
		int listBottom = y + h - 80;
		int rowH = 18;
		ui.clip(x, listTop, x + w, listBottom);
		int ry = listTop;
		for (String name : config.profileNames()) {
			boolean active = name.equals(config.profile);
			boolean hover = ui.hover(x, ry, x + w, ry + rowH);
			ui.d.fill(x, ry, x + w, ry + rowH, active ? look.rowOn() : hover ? look.barHover() : look.row());
			ui.d.fill(x, ry + rowH - 1, x + w, ry + rowH, MenuTheme.alpha(0x000000, t.isLight() ? 0x14 : 0x38));
			if (active) ui.d.fill(x, ry, x + 2, ry + rowH - 1, Accent.get());
			float scale = 0.82f;
			String shown = name;
			while (shown.length() > 1 && ui.spacedWidth(shown, scale, 1.2f, true) > w - 40) shown = shown.substring(0, shown.length() - 1);
			if (!shown.equals(name)) shown = shown.stripTrailing() + "...";
			ui.spaced(shown, x + 9, ry + (rowH - 5.6f) / 2, scale, 1.2f, true, active ? look.text() : look.label());
			if (!active) {
				ui.hit(x, ry, x + w, ry + rowH, () -> {
					config.loadProfile(name);
					notice("Switched to " + name);
				});
				if (hover && !name.equals(Config.DEFAULT_PROFILE)) {
					int dx = x + w - 16;
					boolean delHover = ui.hover(dx, ry + 3, dx + 13, ry + 15);
					PixelIcons.draw(ui.d, PixelIcons.TRASH, dx, ry + 3, 1, delHover ? t.bad() : look.muted());
					ui.hit(dx, ry + 3, dx + 13, ry + 15, () -> {
						if (config.deleteProfile(name)) notice("Deleted " + name);
					});
				}
			} else {
				ui.spaced("ACTIVE", x + w - 6 - ui.spacedWidth("ACTIVE", 0.55f, 0.8f, true), ry + (rowH - 3.7f) / 2, 0.55f, 0.8f, true,
						Accent.get());
			}
			ry += rowH;
		}
		ui.endClip();

		int bx = x0 + 7;
		int bw = 103;
		int by = y + h - 70;
		ui.field(bx, by, bw, 14, profileName, "Profile name", focus == Focus.PROFILE_NAME, null);
		ui.hit(bx, by, bx + bw, by + 14, () -> focus = Focus.PROFILE_NAME);
		by += 18;
		sideButton(ui, look, bx, by, bw, 13, "Save as new profile", false, this::createProfile);
		sideButton(ui, look, x0 + 9, y + h - 19, 99, 15, "Edit HUD layout", true, () -> minecraft().setScreen(new HubScreen(this).editOnly()));
	}

	/** A sidebar button with its label in small spaced capitals, like Lunar's: outlined, or filled in the theme colour. */
	private static void sideButton(Ui ui, Look look, int x, int y, int w, int h, String label, boolean primary, Runnable action) {
		boolean hover = ui.hover(x, y, x + w, y + h);
		int accent = Accent.get();
		if (primary) {
			ui.round(x, y, x + w, y + h, 2.5f, hover ? Draw.mix(accent, 0xFFFFFFFF, 0.15) : accent);
		} else {
			ui.round(x, y, x + w, y + h, 2.5f, hover ? look.barHover() : look.row());
			ui.ring(x, y, x + w, y + h, 2.5f, hover ? look.edge() : look.line());
		}
		float scale = 0.6f;
		float spacing = 1.4f;
		String text = label.toUpperCase(Locale.ROOT);
		float textW = ui.spacedWidth(text, scale, spacing, true);
		ui.spaced(text, x + (w - textW) / 2f, y + (h - 4.1f) / 2f, scale, spacing, true,
				primary ? 0xFFFFFFFF : hover ? look.text() : look.muted());
		ui.hit(x, y, x + w, y + h, action);
	}

	private List<Module> shownModules() {
		String query = search.trim().toLowerCase(Locale.ROOT);
		List<Module> shown = new ArrayList<>();
		for (Module module : Modules.ALL) {
			if (category != null && module.category != category) continue;
			if (newOnly && !module.isNew()) continue;
			if (!query.isEmpty() && !module.name.toLowerCase(Locale.ROOT).contains(query)
					&& !module.description.toLowerCase(Locale.ROOT).contains(query)) {
				continue;
			}
			shown.add(module);
		}
		return shown;
	}

	/** Lunar's chips (ALL, NEW, then a category each), the search box on the right, and the cards under them. */
	private void drawGrid(Ui ui, Look look, int cx, int cy, int cw, int ch) {
		int x = cx;
		record Chip(String label, Category category, boolean newOnly) {
		}
		List<Chip> chips = new ArrayList<>();
		chips.add(new Chip("ALL", null, false));
		chips.add(new Chip("NEW", null, true));
		for (Category each : Category.values()) chips.add(new Chip(each.label.toUpperCase(Locale.ROOT), each, false));
		for (Chip chip : chips) {
			int tw = Math.round(ui.spacedWidth(chip.label(), LABEL, 2f, true)) + 16;
			boolean active = category == chip.category() && newOnly == chip.newOnly();
			boolean hover = ui.hover(x, cy, x + tw, cy + CHIP_H);
			int fill = active ? Accent.get() : chip.newOnly() ? OLIVE : BLUE_GREY;
			if (ui.t.isLight() && !active) fill = MenuTheme.alpha(fill, 0xB0);
			ui.round(x, cy, x + tw, cy + CHIP_H, 2.5f, hover && !active ? Draw.mix(fill, 0xFFFFFFFF, 0.12) : fill);
			int textColor = active ? 0xFFFFFFFF : chip.newOnly() ? 0xFFBECFAA : look.chipText();
			ui.spaced(chip.label(), x + 8, cy + (CHIP_H - 6) / 2f, LABEL, 2f, true, textColor);
			ui.hit(x, cy, x + tw, cy + CHIP_H, () -> {
				category = chip.category();
				newOnly = chip.newOnly();
				resetScroll();
			});
			x += tw + 4;
		}
		int searchW = Math.max(60, Math.min(128, cx + cw + 8 - x - 8));
		int searchX = cx + cw + 8 - searchW;
		boolean searching = focus == Focus.SEARCH;
		ui.round(searchX, cy, searchX + searchW, cy + CHIP_H, 2.5f, look.bar());
		ui.ring(searchX, cy, searchX + searchW, cy + CHIP_H, 2.5f, !search.isEmpty() ? look.edge() : look.line());
		Smooth.icon(ui.d, "search", searchX + 5, cy + 4, 8, look.muted());
		int tx = searchX + 17;
		String shownSearch = search;
		while (!shownSearch.isEmpty() && ui.width(shownSearch) * 0.85f > searchW - 22) shownSearch = shownSearch.substring(1);
		ui.small(shownSearch, tx, cy + 4.6f, 0.85f, look.text());
		if (searching && (System.currentTimeMillis() / 500) % 2 == 0) {
			int caret = tx + Math.round(ui.width(shownSearch) * 0.85f) + 1;
			ui.d.fill(caret, cy + 4, caret + 1, cy + CHIP_H - 4, look.label());
		}
		ui.hit(searchX, cy, searchX + searchW, cy + CHIP_H, () -> focus = Focus.SEARCH);

		int gy = cy + (CARDS_TOP - BODY_TOP);
		int gh = ch - (CARDS_TOP - BODY_TOP);
		List<Module> shown = shownModules();
		int cols = Math.max(2, (cw + CARD_GAP_X) / (CARD_W - 7 + CARD_GAP_X));
		int cardW = (cw - (cols - 1) * CARD_GAP_X) / cols;
		int rows = (shown.size() + cols - 1) / cols;
		int contentH = Math.max(0, rows * (CARD_H + CARD_GAP_Y) - CARD_GAP_Y);
		maxScroll = Math.max(0, contentH - gh);
		scroll = Math.max(0, Math.min(scroll, maxScroll));
		shownScroll = Math.max(0, Math.min(shownScroll, maxScroll));

		if (shown.isEmpty()) {
			String none = "No mods match \"" + search + "\"";
			ui.text(none, cx + (cw - ui.width(none)) / 2, gy + gh / 2 - 4, look.muted());
			return;
		}
		ui.clip(cx - 1, gy - 1, cx + cw + 1, gy + gh);
		for (int i = 0; i < shown.size(); i++) {
			int x1 = cx + (i % cols) * (cardW + CARD_GAP_X);
			int y1 = gy + (i / cols) * (CARD_H + CARD_GAP_Y) - (int) Math.round(shownScroll);
			if (y1 + CARD_H < gy || y1 > gy + gh) continue;
			card(ui, look, shown.get(i), x1, y1, cardW);
		}
		ui.endClip();
		if (maxScroll > 0) {
			int barX = cx + cw + 11;
			int thumb = Math.max(18, gh * gh / Math.max(1, contentH));
			int ty = gy + (int) Math.round((gh - thumb) * (shownScroll / maxScroll));
			ui.round(barX, gy, barX + 3, gy + gh, 1.5f, look.row());
			ui.round(barX, ty, barX + 3, ty + thumb, 1.5f, MenuTheme.alpha(look.label(), 0x80));
		}
	}

	static boolean hasPage(Module module) {
		return !module.settings().isEmpty() || !module.toggleable()
				|| HudWidgets.ALL.stream().anyMatch(widget -> widget.module == module);
	}

	/**
	 * A Lunar-style card: the mod's icon and name, then an OPTIONS bar with a gear in its own cell and an
	 * ENABLED / DISABLED bar across the bottom that switches the mod. New mods get a small red NEW.
	 */
	private void card(Ui ui, Look look, Module module, int x, int y, int w) {
		MenuTheme t = ui.t;
		boolean on = module.enabled();
		boolean toggleable = module.toggleable();
		boolean hasPage = hasPage(module);
		boolean hover = ui.hover(x, y, x + w, y + CARD_H);
		float radius = 4.4f;
		ui.round(x, y, x + w, y + CARD_H, radius, hover ? look.cardHover() : look.card());
		Runnable openPage = () -> openModule(module);
		ui.hit(x, y, x + w, y + CARD_H, hasPage ? openPage : module::toggle, openPage);

		int iconColor = !toggleable || on ? look.text() : MenuTheme.alpha(look.text(), 0xB0);
		float k = Smooth.density(ui.d);
		int pixel = Math.max(1, Math.round(21 * k / PixelIcons.SIZE));
		int iconSize = PixelIcons.SIZE * pixel;
		ui.d.push();
		ui.d.scale(1f / k);
		PixelIcons.draw(ui.d, PixelIcons.forModule(module), Math.round((x + w / 2f) * k - iconSize / 2f), Math.round((y + 31) * k - iconSize / 2f),
				pixel, iconColor);
		ui.d.pop();
		float nameScale = 8.6f / (UiFont.clean() ? 6.8f : 7f);
		String name = module.name;
		while (name.length() > 1 && ui.width(name) * nameScale > w - 10) name = name.substring(0, name.length() - 1);
		if (!name.equals(module.name)) name = name.stripTrailing() + "...";
		float nameW = ui.width(name) * nameScale;
		float nameX = x + (w - nameW) / 2f;
		ui.small(name, nameX, y + 66, nameScale, hover ? look.text() : MenuTheme.alpha(look.label(), on || !toggleable ? 0xE0 : 0xA8));
		if (module.isNew()) ui.spaced("NEW", nameX + nameW - ui.spacedWidth("NEW", 0.5f, 0.5f, true) + 4, y + 60, 0.5f, 0.5f, true, 0xFFE0344F);

		int optionsY = y + 82;
		int barH = 21;
		if (hasPage) {
			int gearW = 22;
			boolean optionsHover = ui.hover(x, optionsY, x + w, optionsY + barH);
			ui.d.fill(x, optionsY, x + w, optionsY + barH, optionsHover ? look.barHover() : look.bar());
			ui.d.fill(x + w - gearW - 1, optionsY, x + w - gearW, optionsY + barH, MenuTheme.alpha(0x000000, t.isLight() ? 0x18 : 0x40));
			float textW = ui.spacedWidth("OPTIONS", LABEL, LABEL_SPACING + 0.6f, true);
			ui.spaced("OPTIONS", x + (w - gearW - textW) / 2f, optionsY + (barH - 6) / 2f, LABEL, LABEL_SPACING + 0.6f, true,
					optionsHover ? look.text() : look.label());
			int gear = Math.max(1, Math.round(10 * k / Icons.SIZE));
			ui.d.push();
			ui.d.scale(1f / k);
			Icons.draw(ui.d, Icons.GEAR, gear, Math.round((x + w - gearW / 2f) * k - Icons.SIZE * gear / 2f),
					Math.round((optionsY + barH / 2f) * k - Icons.SIZE * gear / 2f), optionsHover ? look.text() : look.label());
			ui.d.pop();
			ui.hit(x, optionsY, x + w, optionsY + barH, openPage);
		}
		int barY = y + CARD_H - barH - 1;
		String label = !toggleable ? "ALWAYS ON" : on ? "ENABLED" : "DISABLED";
		int fill = !toggleable ? LOCKED : on ? ON_GREEN : OFF_RED;
		boolean barHover = ui.hover(x, barY, x + w, y + CARD_H);
		ui.d.push();
		ui.d.enableScissor(x, barY, x + w, y + CARD_H);
		ui.round(x, barY - radius, x + w, y + CARD_H, radius, barHover && toggleable ? Draw.mix(fill, 0xFFFFFFFF, 0.12) : fill);
		ui.d.disableScissor();
		ui.d.pop();
		float labelW = ui.spacedWidth(label, LABEL, LABEL_SPACING + 0.6f, true);
		ui.spaced(label, x + (w - labelW) / 2f, barY + (y + CARD_H - barY - 6) / 2f, LABEL, LABEL_SPACING + 0.6f, true, 0xFFFFFFFF);
		ui.hit(x, barY, x + w, y + CARD_H, toggleable ? module::toggle : openPage, openPage);
		ui.ring(x, y, x + w, y + CARD_H, radius, hover ? look.edge() : look.cardEdge());
	}

	private void drawPage(Ui ui, int cx, int cy, int cw, int ch) {
		MenuTheme t = ui.t;
		Module module = page;
		// Back, icon, name and description
		boolean backHover = ui.hover(cx, cy, cx + 16, cy + 16);
		ui.round(cx, cy, cx + 16, cy + 16, 8, backHover ? t.cardHover() : t.card());
		ui.ring(cx, cy, cx + 16, cy + 16, 8, backHover ? t.edgeStrong() : t.edge());
		Icons.draw(ui.d, Icons.BACK, cx + 4, cy + 4, t.text());
		ui.hit(cx, cy, cx + 16, cy + 16, this::leavePage);
		PixelIcons.draw(ui.d, PixelIcons.forModule(module), cx + 22, cy + 2, 1, t.text());
		ui.title(module.name, cx + 38, cy + 1, t.text());
		ui.small(ui.trim(module.description, (int) ((cw - 80) / 0.75f)), cx + 38, cy + 11, 0.75f, t.muted());
		if (module.toggleable()) {
			int tx = cx + cw - 26;
			ui.toggle(tx, cy + 3, module.enabled());
			ui.hit(tx - 4, cy, tx + 26, cy + 16, module::toggle);
		}

		// Rows
		int listY = cy + 24;
		int listH = ch - 24;
		List<Runnable> rows = new ArrayList<>();
		List<Setting> settings = module.settings();
		HudWidget widget = HudWidgets.ALL.stream().filter(each -> each.module == module).findFirst().orElse(null);
		int count = settings.size() + (widget != null ? 1 : 0) + (module == Modules.THEME ? 1 : 0)
				+ (module == Modules.MUSIC ? 1 : 0);
		int contentH = count * (ROW_H + 4) + (module == Modules.PLAYER_LIST ? playersHeight() : 0)
				+ (module == Modules.SKIN_PROTECT ? 2 * (ROW_H + 4) : 0)
				+ (module == Modules.NAME_PROTECT ? rulesHeight() : 0)
				+ (module == Modules.TEXT_HOTKEYS ? hotkeysHeight() : 0);
		maxScroll = Math.max(0, contentH - listH);
		scroll = Math.max(0, Math.min(scroll, maxScroll));
		shownScroll = Math.max(0, Math.min(shownScroll, maxScroll));

		ui.clip(cx, listY, cx + cw, listY + listH);
		int y = listY - (int) Math.round(shownScroll);
		int rowW = cw - 6;
		if (widget != null) {
			row(ui, cx, y, rowW, "Position", "Drag it anywhere on screen in the hub (Right Shift)");
			int moveW = ui.titleWidth("Move on screen") + 16;
			ui.button(cx + rowW - 8 - moveW, y + 6, moveW, 14, "Move on screen", false,
					() -> minecraft().setScreen(new HubScreen(this).editOnly()));
			y += ROW_H + 4;
		}
		for (Setting setting : settings) {
			settingRow(ui, setting, cx, y, rowW);
			y += ROW_H + 4;
		}
		if (module == Modules.THEME) {
			row(ui, cx, y, rowW, "HUD layout", "Put every HUD widget back where it started");
			int resetW = ui.titleWidth("Reset layout") + 16;
			ui.button(cx + rowW - 8 - resetW, y + 6, resetW, 14, "Reset layout", false, () -> {
				for (HudWidget each : HudWidgets.ALL) each.resetPosition();
				notice("HUD layout reset");
			});
		}
		if (module == Modules.MUSIC) {
			row(ui, cx, y, rowW, "Status", "Reads what's playing from Windows' media controls");
			String status = MusicBridge.status();
			boolean connected = status.equals("Connected");
			int sw = ui.width(status);
			ui.text(status, cx + rowW - 8 - sw, y + 9, connected ? t.good() : t.muted());
			ui.dot(cx + rowW - 8 - sw - 9, y + 10, 5, connected ? t.good() : MenuTheme.alpha(t.muted(), 0x90));
			y += ROW_H + 4;
		}
		if (module == Modules.PLAYER_LIST) drawPlayers(ui, cx, y, rowW);
		if (module == Modules.SKIN_PROTECT) drawSkinFile(ui, cx, y, rowW);
		if (module == Modules.NAME_PROTECT) drawRules(ui, cx, y, rowW);
		if (module == Modules.TEXT_HOTKEYS) drawHotkeys(ui, cx, y, rowW);
		ui.endClip();
		ui.scrollbar(cx + cw - 2, listY, listH, shownScroll, maxScroll, contentH);
	}

	private void row(Ui ui, int x, int y, int w, String name, String description) {
		row(ui, x, y, w, name, description, 142);
	}

	/** A settings row whose controls take {@code reserved} pixels on the right; rows are split by thin lines, like Lunar's. */
	private void row(Ui ui, int x, int y, int w, String name, String description, int reserved) {
		boolean hover = ui.hover(x, y, x + w, y + ROW_H);
		if (hover) ui.round(x, y, x + w, y + ROW_H, Ui.R_CARD, ui.t.cardHover());
		ui.d.fill(x + 4, y + ROW_H + 1, x + w - 4, y + ROW_H + 2, ui.t.edge());
		ui.text(ui.trim(name, w - reserved - 16), x + 8, y + 5, ui.t.text());
		ui.small(ui.trim(description, (int) ((w - reserved - 16) / 0.75f)), x + 8, y + 16, 0.75f, ui.t.muted());
	}

	private void settingRow(Ui ui, Setting setting, int x, int y, int w) {
		row(ui, x, y, w, setting.name, setting.description);
		MenuTheme t = ui.t;
		int right = x + w - 8;
		if (setting instanceof Setting.Toggle toggle) {
			ui.toggle(right - 22, y + 8, toggle.get());
			ui.hit(x, y, x + w, y + ROW_H, toggle::toggle);
		} else if (setting instanceof Setting.Slider slider) {
			String value = slider.format();
			int valueW = ui.width("000.0x");
			int trackW = 80;
			int trackX = right - valueW - 6 - trackW;
			ui.slider(trackX, y + 9, trackW, slider.fraction());
			ui.text(value, right - ui.width(value), y + 9, t.text());
			ui.hit(trackX - 4, y + 4, trackX + trackW + 4, y + ROW_H - 4, () -> {
				draggingSlider = slider;
				sliderX = trackX;
				sliderW = trackW;
				slider.setFraction((ui.mouseX - trackX) / (double) trackW);
			});
		} else if (setting instanceof Setting.Choice choice) {
			int widest = 0;
			for (String option : choice.options()) widest = Math.max(widest, ui.width(option.toUpperCase(Locale.ROOT)));
			int boxW = Math.max(92, widest + 30);
			int bx = right - boxW;
			boolean hover = ui.hover(bx, y + 6, bx + boxW, y + 20);
			ui.round(bx, y + 6, bx + boxW, y + 20, 7, hover ? t.cardHover() : t.field());
			ui.ring(bx, y + 6, bx + boxW, y + 20, 7, hover ? t.edgeStrong() : t.edge());
			ui.text("<", bx + 4, y + 9, t.muted());
			ui.text(">", bx + boxW - 9, y + 9, t.muted());
			String value = choice.get().toUpperCase(Locale.ROOT);
			ui.text(value, bx + (boxW - ui.width(value)) / 2, y + 9, t.text());
			ui.hit(bx, y + 6, bx + boxW / 2, y + 20, choice::previous);
			ui.hit(bx + boxW / 2, y + 6, bx + boxW, y + 20, choice::next);
		} else if (setting instanceof Setting.Text text) {
			int fieldW = 110;
			int fx = right - fieldW;
			boolean focused = focus == Focus.SETTING_TEXT && editingText == text;
			ui.field(fx, y + 5, fieldW, 16, text.get(), "Type here", focused, PixelIcons.PENCIL);
			ui.hit(fx, y + 5, fx + fieldW, y + 21, () -> {
				editingText = text;
				focus = Focus.SETTING_TEXT;
			});
		} else if (setting instanceof Setting.Color color) {
			int[] swatches = Setting.Color.SWATCHES;
			int size = 10;
			int sx = right - swatches.length * (size + 2) + 2;
			for (int swatch : swatches) {
				boolean selected = (color.get() | 0xFF000000) == swatch;
				if (selected) ui.round(sx - 2, y + 6, sx + size + 2, y + 10 + size, 4, t.accent());
				ui.round(sx, y + 8, sx + size, y + 8 + size, 3, swatch);
				int pick = swatch;
				ui.hit(sx, y + 8, sx + size, y + 8 + size, () -> color.set(pick));
				sx += size + 2;
			}
		}
	}

	private static final int PLAYER_ROW = 22;

	private int playersHeight() {
		return 14 + 22 + (PlayerList.suggestions(minecraft(), playerName, 6).isEmpty() ? 0 : 18)
				+ Math.max(1, PlayerList.names().size()) * (PLAYER_ROW + 4);
	}

	/** The Player List page: add a name (with names from the server as suggestions), then everyone on the list. */
	private void drawPlayers(Ui ui, int x, int y, int w) {
		MenuTheme t = ui.t;
		Minecraft mc = minecraft();
		ui.small("PLAYERS ON YOUR LIST", x + 2, y + 3, 0.75f, t.muted());
		y += 14;
		int fieldW = w - 70;
		ui.field(x, y, fieldW, 16, playerName, "Add a player: type their name", focus == Focus.PLAYER_NAME, PixelIcons.PLUS);
		ui.hit(x, y, x + fieldW, y + 16, () -> focus = Focus.PLAYER_NAME);
		ui.button(x + fieldW + 6, y, 64, 16, "+ Add", true, this::addPlayer);
		y += 22;

		List<String> suggestions = PlayerList.suggestions(mc, playerName, 6);
		if (!suggestions.isEmpty()) {
			int sx = x;
			for (String name : suggestions) {
				int pw = ui.pillWidth(name) + 10;
				if (sx + pw > x + w) break;
				boolean hover = ui.hover(sx, y, sx + pw, y + 12);
				ui.round(sx, y, sx + pw, y + 12, 6, hover ? t.cardHover() : t.card());
				ui.ring(sx, y, sx + pw, y + 12, 6, hover ? t.edgeStrong() : t.edge());
				ui.text("+ " + name, sx + 5, y + 2, hover ? t.accent() : t.text());
				ui.hit(sx, y, sx + pw, y + 12, () -> {
					if (PlayerList.add(name)) notice("Added " + name);
				});
				sx += pw + 4;
			}
			y += 18;
		}

		List<String> names = new ArrayList<>(PlayerList.names());
		if (names.isEmpty()) {
			String hint = "Nobody yet. Add friends, rivals or staff to see when they're on.";
			ui.text(ui.trim(hint, w - 8), x + 4, y + 7, t.muted());
			return;
		}
		for (String name : names) {
			boolean online = PlayerList.isOnline(name);
			PlayerInfo info = online ? PlayerList.info(mc, name) : null;
			boolean hover = ui.hover(x, y, x + w, y + PLAYER_ROW);
			ui.card(x, y, x + w, y + PLAYER_ROW, hover ? t.cardHover() : t.card(), t.edge());
			int fx = x + 6;
			int fy = y + 5;
			if (info != null) ui.d.face(info.getSkin(), fx, fy, 12);
			else ui.d.face(DefaultPlayerSkin.get(UUIDUtil.createOfflinePlayerUUID(name)), fx, fy, 12);
			ui.text(name, x + 24, y + 7, online ? t.text() : t.muted());
			int right = x + w - 8;
			int dx = right - 16;
			boolean delHover = ui.hover(dx, y + 3, dx + 16, y + 19);
			if (delHover) ui.round(dx, y + 3, dx + 16, y + 19, 4, MenuTheme.alpha(t.bad(), 0x50));
			PixelIcons.draw(ui.d, PixelIcons.TRASH, dx + 2, y + 5, 1, delHover ? t.bad() : t.muted());
			ui.hit(dx, y + 3, dx + 16, y + 19, () -> {
				PlayerList.remove(name);
				notice("Removed " + name);
			});
			String state = online ? "ONLINE" : "OFFLINE";
			int pw = ui.pillWidth(state);
			ui.pill(dx - 6 - pw, y + 5, state, online ? MenuTheme.alpha(t.good(), 0x40) : MenuTheme.alpha(t.muted(), 0x30),
					online ? t.good() : t.muted());
			y += PLAYER_ROW + 4;
		}
	}

	/** Skin Protect: which skin file to wear (from the skins folder), and how loading the chosen skin went. */
	private void drawSkinFile(Ui ui, int x, int y, int w) {
		MenuTheme t = ui.t;
		int right = x + w - 8;
		int openW = ui.titleWidth("Open folder") + 16;
		row(ui, x, y, w, "Skin file", "PNG skins in the 3vhy/skins folder", openW + 126 + 8);
		ui.button(right - openW, y + 6, openW, 14, "Open folder", false, CustomSkins::openFolder);
		List<String> files = CustomSkins.files();
		int boxW = 120;
		int bx = right - openW - 6 - boxW;
		boolean hover = ui.hover(bx, y + 6, bx + boxW, y + 20);
		ui.round(bx, y + 6, bx + boxW, y + 20, 7, hover ? t.cardHover() : t.field());
		ui.ring(bx, y + 6, bx + boxW, y + 20, 7, hover ? t.edgeStrong() : t.edge());
		String current = Config.get().skinFile;
		String shown = files.isEmpty() ? "No files yet" : current.isBlank() || !files.contains(current) ? "Pick one" : current;
		shown = ui.trim(shown, boxW - 24);
		ui.text(shown, bx + (boxW - ui.width(shown)) / 2, y + 9, files.isEmpty() ? t.muted() : t.text());
		if (!files.isEmpty()) {
			ui.text("<", bx + 4, y + 9, t.muted());
			ui.text(">", bx + boxW - 9, y + 9, t.muted());
			ui.hit(bx, y + 6, bx + boxW / 2, y + 20, () -> pickSkinFile(files, -1));
			ui.hit(bx + boxW / 2, y + 6, bx + boxW, y + 20, () -> pickSkinFile(files, 1));
		}
		y += ROW_H + 4;
		row(ui, x, y, w, "Status", "What Skin Protect is showing right now");
		String status = ui.trim(CustomSkins.status(), w / 2);
		ui.text(status, right - ui.width(status), y + 9, t.muted());
	}

	private void pickSkinFile(List<String> files, int step) {
		int index = files.indexOf(Config.get().skinFile);
		index = index < 0 ? (step > 0 ? 0 : files.size() - 1) : Math.floorMod(index + step, files.size());
		Config.get().skinFile = files.get(index);
		Config.saveSoon();
		if (!Modules.SKIN_PROTECT_SOURCE.is(CustomSkins.SOURCE_FILE)) {
			while (!Modules.SKIN_PROTECT_SOURCE.is(CustomSkins.SOURCE_FILE)) Modules.SKIN_PROTECT_SOURCE.next();
		}
	}

	private static final int RULE_ROW = 22;

	private int rulesHeight() {
		return 14 + 22 + 12 + Math.max(1, Config.get().textRules.size()) * (RULE_ROW + 4);
	}

	/** Name Protect: change any text on screen, like the stats on a server's scoreboard. */
	private void drawRules(Ui ui, int x, int y, int w) {
		MenuTheme t = ui.t;
		ui.small("CHANGE TEXT ON SCREEN", x + 2, y + 3, 0.75f, t.muted());
		y += 14;
		int addW = 56;
		int fieldW = (w - addW - 12) / 2;
		ui.field(x, y, fieldW, 16, ruleFind, "Find, like Money: #", focus == Focus.RULE_FIND, PixelIcons.PENCIL);
		ui.hit(x, y, x + fieldW, y + 16, () -> focus = Focus.RULE_FIND);
		int rx = x + fieldW + 6;
		ui.field(rx, y, fieldW, 16, ruleReplace, "Show instead, like Money: 1T", focus == Focus.RULE_REPLACE, null);
		ui.hit(rx, y, rx + fieldW, y + 16, () -> focus = Focus.RULE_REPLACE);
		ui.button(x + w - addW, y, addW, 16, "+ Add", true, this::addRule);
		y += 22;
		String hint = "# matches any number and * any word. Works on scoreboards, chat, tab and nametags.";
		ui.small(ui.trim(hint, (int) ((w - 4) / 0.75f)), x + 2, y, 0.75f, t.muted());
		y += 12;
		List<Config.TextRule> rules = new ArrayList<>(Config.get().textRules);
		if (rules.isEmpty()) {
			ui.text(ui.trim("No changes yet. Try Kills: # -> Kills: 9999", w - 8), x + 4, y + 7, t.muted());
			return;
		}
		for (Config.TextRule rule : rules) {
			boolean hover = ui.hover(x, y, x + w, y + RULE_ROW);
			ui.card(x, y, x + w, y + RULE_ROW, hover ? t.cardHover() : t.card(), t.edge());
			int half = (w - 40) / 2;
			ui.text(ui.trim(rule.find, half - 10), x + 8, y + 7, t.text());
			ui.text("->", x + 8 + half - 6, y + 7, t.accent());
			ui.text(ui.trim(rule.replace.isEmpty() ? "(nothing)" : rule.replace, half - 10), x + 8 + half + 10, y + 7,
					rule.replace.isEmpty() ? t.muted() : t.text());
			int dx = x + w - 24;
			boolean delHover = ui.hover(dx, y + 3, dx + 16, y + 19);
			if (delHover) ui.round(dx, y + 3, dx + 16, y + 19, 4, MenuTheme.alpha(t.bad(), 0x50));
			PixelIcons.draw(ui.d, PixelIcons.TRASH, dx + 2, y + 5, 1, delHover ? t.bad() : t.muted());
			ui.hit(dx, y + 3, dx + 16, y + 19, () -> {
				TextRules.remove(rule);
				notice("Removed " + rule.find);
			});
			y += RULE_ROW + 4;
		}
	}

	private void addRule() {
		if (ruleFind.isBlank()) {
			notice("Type what to find first");
			focus = Focus.RULE_FIND;
			return;
		}
		TextRules.add(ruleFind, ruleReplace);
		notice("Changing " + ruleFind.trim());
		ruleFind = "";
		ruleReplace = "";
		focus = Focus.RULE_FIND;
	}

	private int hotkeysHeight() {
		return 14 + 22 + 12 + Math.max(1, Config.get().textHotkeys.size()) * (RULE_ROW + 4);
	}

	/** Text Hotkeys: pick a key, type a message or /command, then every hotkey with its key. */
	private void drawHotkeys(Ui ui, int x, int y, int w) {
		MenuTheme t = ui.t;
		ui.small("YOUR HOTKEYS", x + 2, y + 3, 0.75f, t.muted());
		y += 14;
		int keyW = 86;
		int addW = 56;
		boolean picking = focus == Focus.HOTKEY_KEY;
		String keyLabel = picking ? "Press a key..." : hotkeyKey < 0 ? "Pick a key" : "Key: " + TextHotkeys.keyName(hotkeyKey, hotkeyMouse);
		ui.button(x, y, keyW, 16, ui.trim(keyLabel, keyW - 10), picking, () -> focus = Focus.HOTKEY_KEY);
		int fx = x + keyW + 6;
		int fieldW = w - keyW - addW - 12;
		ui.field(fx, y, fieldW, 16, hotkeyText, "Message or /command", focus == Focus.HOTKEY_TEXT, PixelIcons.PENCIL);
		ui.hit(fx, y, fx + fieldW, y + 16, () -> focus = Focus.HOTKEY_TEXT);
		ui.button(x + w - addW, y, addW, 16, "+ Add", true, this::addHotkey);
		y += 22;
		String hint = "Start with / to run a command. Never fires while you type.";
		ui.small(ui.trim(hint, (int) ((w - 4) / 0.75f)), x + 2, y, 0.75f, t.muted());
		y += 12;
		List<Config.TextHotkey> hotkeys = new ArrayList<>(Config.get().textHotkeys);
		if (hotkeys.isEmpty()) {
			ui.text(ui.trim("No hotkeys yet. Try F6 -> /spawn or G -> gg", w - 8), x + 4, y + 7, t.muted());
			return;
		}
		for (Config.TextHotkey hotkey : hotkeys) {
			boolean hover = ui.hover(x, y, x + w, y + RULE_ROW);
			ui.card(x, y, x + w, y + RULE_ROW, hover ? t.cardHover() : t.card(), t.edge());
			String key = TextHotkeys.keyName(hotkey).toUpperCase(Locale.ROOT);
			int pw = ui.pillWidth(key);
			ui.pill(x + 6, y + 5, key, MenuTheme.alpha(t.accent(), 0x40), t.accent());
			boolean command = hotkey.text.startsWith("/");
			int textX = x + 6 + pw + 8;
			ui.text(ui.trim(hotkey.text, x + w - 32 - textX), textX, y + 7, command ? t.accent() : t.text());
			int dx = x + w - 24;
			boolean delHover = ui.hover(dx, y + 3, dx + 16, y + 19);
			if (delHover) ui.round(dx, y + 3, dx + 16, y + 19, 4, MenuTheme.alpha(t.bad(), 0x50));
			PixelIcons.draw(ui.d, PixelIcons.TRASH, dx + 2, y + 5, 1, delHover ? t.bad() : t.muted());
			ui.hit(dx, y + 3, dx + 16, y + 19, () -> {
				TextHotkeys.remove(hotkey);
				notice("Removed the " + TextHotkeys.keyName(hotkey) + " hotkey");
			});
			y += RULE_ROW + 4;
		}
	}

	private void addHotkey() {
		if (hotkeyKey < 0) {
			notice("Pick a key first");
			focus = Focus.HOTKEY_KEY;
			return;
		}
		if (hotkeyText.isBlank()) {
			notice("Type a message or command first");
			focus = Focus.HOTKEY_TEXT;
			return;
		}
		TextHotkeys.add(hotkeyKey, hotkeyMouse, hotkeyText);
		notice(TextHotkeys.keyName(hotkeyKey, hotkeyMouse) + " sends " + hotkeyText.trim());
		hotkeyText = "";
		hotkeyKey = -1;
		hotkeyMouse = false;
		focus = Focus.SEARCH;
	}

	/** Takes the key (or extra mouse button) for a new Text Hotkey; Escape cancels. */
	private void pickHotkey(int key, boolean mouse) {
		if (key >= 0) {
			hotkeyKey = key;
			hotkeyMouse = mouse;
			hotkeyPickedAt = System.currentTimeMillis();
		}
		focus = Focus.HOTKEY_TEXT;
	}

	private void addPlayer() {
		String name = playerName.trim();
		if (name.isEmpty()) {
			notice("Type a name first");
		} else if (!PlayerList.isValidName(name)) {
			notice("That isn't a Minecraft name");
		} else if (!PlayerList.add(name)) {
			notice(name + " is already on the list");
		} else {
			notice("Added " + name);
			playerName = "";
		}
	}

	private void createProfile() {
		if (Config.get().createProfile(profileName)) {
			notice("Created " + profileName.trim());
			profileName = "";
		} else {
			notice(profileName.isBlank() ? "Type a name first" : "That name is taken");
		}
	}

	private void drawWaypoints(Ui ui, int cx, int cy, int cw, int ch) {
		MenuTheme t = ui.t;
		Minecraft mc = minecraft();
		boolean inWorld = Waypoints.worldKey(mc) != null && mc.player != null;
		if (inWorld) {
			ui.button(cx, cy, 104, 16, "+ Add here", true, () -> {
				Waypoint added = Waypoints.addHere(mc);
				if (added != null) notice("Added " + added.name);
			});
			String where = Waypoints.worldKey(mc);
			where = where.substring(where.indexOf(':') + 1);
			ui.text(ui.trim("Waypoints for " + where, cw - 116), cx + 112, cy + 4, t.muted());
		} else {
			ui.text("Join a world or server to add waypoints.", cx, cy + 4, t.muted());
		}

		List<Waypoint> list = new ArrayList<>(Waypoints.current(mc));
		int listY = cy + 24;
		int listH = ch - 24;
		int contentH = list.size() * 28;
		maxScroll = Math.max(0, contentH - listH);
		scroll = Math.max(0, Math.min(scroll, maxScroll));
		shownScroll = Math.max(0, Math.min(shownScroll, maxScroll));
		if (list.isEmpty() && inWorld) {
			String hint = "No waypoints yet. Stand somewhere and press + ADD HERE.";
			ui.text(hint, cx + (cw - ui.width(hint)) / 2, listY + listH / 2 - 4, t.muted());
		}
		ui.clip(cx, listY, cx + cw, listY + listH);
		int y = listY - (int) Math.round(shownScroll);
		for (Waypoint waypoint : list) {
			boolean hover = ui.hover(cx, y, cx + cw - 6, y + 24);
			ui.card(cx, y, cx + cw - 6, y + 24, hover ? t.cardHover() : t.card(), t.edge());
			// Colour: click to change
			ui.round(cx + 6, y + 7, cx + 16, y + 17, 3, waypoint.color | 0xFF000000);
			ui.hit(cx + 6, y + 7, cx + 16, y + 17, () -> {
				int[] swatches = Setting.Color.SWATCHES;
				int index = 0;
				for (int i = 0; i < swatches.length; i++) if (swatches[i] == (waypoint.color | 0xFF000000)) index = i;
				waypoint.color = swatches[(index + 1) % swatches.length];
				Config.saveSoon();
			});
			// Name: click to rename
			boolean editing = waypoint == renaming && focus == Focus.WAYPOINT_NAME;
			if (editing) {
				ui.field(cx + 22, y + 4, 110, 16, waypointName, "Name", true, null);
			} else {
				ui.title(ui.trimTitle(waypoint.name, 110), cx + 22, y + 4, t.text());
				ui.hit(cx + 22, y + 2, cx + 132, y + 13, () -> {
					renaming = waypoint;
					waypointName = waypoint.name;
					focus = Focus.WAYPOINT_NAME;
				});
			}
			String place = waypoint.x + " " + waypoint.y + " " + waypoint.z + "  " + dimensionName(waypoint.dimension);
			if (!editing) ui.small(place, cx + 22, y + 15, 0.75f, t.muted());
			int right = cx + cw - 12;
			String distance = mc.player == null ? "" : Math.round(Waypoints.distance(mc, waypoint)) + "m";
			ui.text(distance, right - 44 - ui.width(distance), y + 8, t.muted());
			int ex = right - 38;
			boolean eyeHover = ui.hover(ex, y + 5, ex + 16, y + 19);
			if (eyeHover) ui.round(ex, y + 5, ex + 16, y + 19, 4, t.cardOn());
			PixelIcons.draw(ui.d, waypoint.visible ? PixelIcons.EYE : PixelIcons.EYE_OFF, ex + 2, y + 6, 1,
					waypoint.visible ? t.text() : t.muted());
			ui.hit(ex, y + 5, ex + 16, y + 19, () -> {
				waypoint.visible = !waypoint.visible;
				Config.saveSoon();
			});
			int dx = right - 18;
			boolean delHover = ui.hover(dx, y + 5, dx + 16, y + 19);
			if (delHover) ui.round(dx, y + 5, dx + 16, y + 19, 4, MenuTheme.alpha(t.bad(), 0x50));
			PixelIcons.draw(ui.d, PixelIcons.TRASH, dx + 2, y + 6, 1, delHover ? t.bad() : t.muted());
			ui.hit(dx, y + 5, dx + 16, y + 19, () -> Waypoints.remove(mc, waypoint));
			y += 28;
		}
		ui.endClip();
		ui.scrollbar(cx + cw - 2, listY, listH, shownScroll, maxScroll, contentH);
	}

	private static String dimensionName(String key) {
		if (key.contains("the_nether")) return "Nether";
		if (key.contains("the_end")) return "End";
		if (key.contains("overworld")) return "Overworld";
		int slash = key.lastIndexOf('/');
		return slash >= 0 ? key.substring(slash + 1).replace("]", "").trim() : key;
	}

	/** A short message (profile switched, player added and so on) in a pill at the bottom for a moment. */
	private void drawNotice(Ui ui, int cx, int y, int cw) {
		long age = System.currentTimeMillis() - noticeAt;
		if (notice == null || age > 2500) return;
		int alpha = age > 2200 ? Math.round(255 * (2500 - age) / 300f) : 255;
		String text = notice.toUpperCase(Locale.ROOT);
		float w = ui.spacedWidth(text, 0.7f, 1.2f, true) + 20;
		float x = cx + (cw - w) / 2f;
		ui.round(x, y, x + w, y + 15, 7.5f, MenuTheme.alpha(0x0B0D12, alpha * 0xE6 / 255));
		ui.ring(x, y, x + w, y + 15, 7.5f, MenuTheme.alpha(0xFFFFFF, alpha * 0x30 / 255));
		ui.spaced(text, x + 10, y + 5.3f, 0.7f, 1.2f, true, MenuTheme.alpha(0xFFFFFF, alpha));
	}

	private Minecraft minecraft() {
		return Minecraft.getInstance();
	}

	// Input

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		int button = event.button();
		if (focus == Focus.HOTKEY_KEY && button >= 2) {
			pickHotkey(button, true);
			return true;
		}
		if (button == 0 || button == 1) {
			for (int i = hits.size() - 1; i >= 0; i--) {
				Ui.Hit hit = hits.get(i);
				if (!hit.contains(event.x() / unit, event.y() / unit)) continue;
				Runnable action = button == 0 ? hit.action() : hit.rightAction();
				if (action == null) continue;
				if (focus == Focus.WAYPOINT_NAME) finishRename();
				if (typingField()) focus = Focus.SEARCH;
				minecraft().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
				action.run();
				return true;
			}
		}
		if (focus == Focus.WAYPOINT_NAME) finishRename();
		if (typingField()) focus = Focus.SEARCH;
		return super.mouseClicked(event, doubleClick);
	}

	/** Fields that let go of the keyboard when you click somewhere else. */
	private boolean typingField() {
		return focus == Focus.PROFILE_NAME || focus == Focus.SETTING_TEXT || focus == Focus.PLAYER_NAME
				|| focus == Focus.RULE_FIND || focus == Focus.RULE_REPLACE || focus == Focus.HOTKEY_TEXT
				|| focus == Focus.HOTKEY_KEY;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (draggingSlider != null) {
			draggingSlider.setFraction((event.x() / unit - sliderX) / sliderW);
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
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
		scroll = Math.max(0, Math.min(maxScroll, scroll - vertical * 26));
		return true;
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		String typed = event.codepointAsString();
		if (typed.isEmpty() || typed.codePointAt(0) < 32 || typed.codePointAt(0) == 127) return false;
		if (System.currentTimeMillis() - hotkeyPickedAt < 150) return true;
		switch (focus) {
			case PROFILE_NAME -> {
				if (profileName.length() < 24) profileName += typed;
			}
			case WAYPOINT_NAME -> {
				if (waypointName.length() < 24) waypointName += typed;
			}
			case SETTING_TEXT -> {
				if (editingText != null) editingText.set(editingText.get() + typed);
			}
			case PLAYER_NAME -> {
				if (playerName.length() < 16 && typed.matches("[A-Za-z0-9_]")) playerName += typed;
			}
			case RULE_FIND -> {
				if (ruleFind.length() < 48) ruleFind += typed;
			}
			case RULE_REPLACE -> {
				if (ruleReplace.length() < 48) ruleReplace += typed;
			}
			case HOTKEY_TEXT -> {
				if (hotkeyText.length() < TextHotkeys.MAX_LENGTH) hotkeyText += typed;
			}
			case HOTKEY_KEY -> {
			}
			case SEARCH -> {
				if (tab != Tab.MODS || page != null) {
					tab = Tab.MODS;
					page = null;
				}
				if (search.length() < 32) search += typed;
				resetScroll();
			}
		}
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int key = event.key();
		if (focus == Focus.HOTKEY_KEY) {
			pickHotkey(key == GLFW.GLFW_KEY_ESCAPE ? -1 : key, false);
			return true;
		}
		if (key == GLFW.GLFW_KEY_BACKSPACE) {
			switch (focus) {
				case PROFILE_NAME -> profileName = dropLast(profileName);
				case WAYPOINT_NAME -> waypointName = dropLast(waypointName);
				case SETTING_TEXT -> {
					if (editingText != null) editingText.set(dropLast(editingText.get()));
				}
				case PLAYER_NAME -> playerName = dropLast(playerName);
				case RULE_FIND -> ruleFind = dropLast(ruleFind);
				case RULE_REPLACE -> ruleReplace = dropLast(ruleReplace);
				case HOTKEY_TEXT -> hotkeyText = dropLast(hotkeyText);
				case HOTKEY_KEY -> {
				}
				case SEARCH -> {
					search = dropLast(search);
					resetScroll();
				}
			}
			return true;
		}
		if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
			if (focus == Focus.PROFILE_NAME) createProfile();
			if (focus == Focus.WAYPOINT_NAME) finishRename();
			if (focus == Focus.PLAYER_NAME) addPlayer();
			if (focus == Focus.SETTING_TEXT) focus = Focus.SEARCH;
			if (focus == Focus.HOTKEY_TEXT) addHotkey();
			if (focus == Focus.RULE_REPLACE) addRule();
			else if (focus == Focus.RULE_FIND) focus = Focus.RULE_REPLACE;
			return true;
		}
		if (key == GLFW.GLFW_KEY_ESCAPE) {
			if (typingField()) {
				focus = Focus.SEARCH;
				return true;
			}
			if (focus == Focus.WAYPOINT_NAME) {
				renaming = null;
				focus = Focus.SEARCH;
				return true;
			}
			if (page != null) {
				leavePage();
				return true;
			}
			if (!search.isEmpty()) {
				search = "";
				resetScroll();
				return true;
			}
		}
		if (search.isEmpty() && focus == Focus.SEARCH && ThreevhyClient.openSettingsKey != null
				&& ThreevhyClient.openSettingsKey.matches(event)) {
			onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	private static String dropLast(String text) {
		return text.isEmpty() ? text : text.substring(0, text.length() - 1);
	}

	private void finishRename() {
		if (renaming != null && !waypointName.isBlank()) {
			renaming.name = waypointName.trim();
			Config.saveSoon();
		}
		renaming = null;
		focus = Focus.SEARCH;
	}

	@Override
	public void onClose() {
		Config.save();
		Minecraft.getInstance().setScreen(parent);
	}
}
