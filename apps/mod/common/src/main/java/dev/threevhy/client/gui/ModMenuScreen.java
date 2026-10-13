package dev.threevhy.client.gui;

import dev.threevhy.client.Config;
import dev.threevhy.client.Config.Waypoint;
import dev.threevhy.client.Draw;
import dev.threevhy.client.ThreevhyClient;
import dev.threevhy.client.compat.CompatScreen;
import dev.threevhy.client.discord.DiscordPresence;
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
 * layout, and a grid of mod cards with an OPTIONS bar and an ENABLED / DISABLED bar. The other
 * style, Panels, is {@link ClickGuiScreen}.
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
	private static final int SIDEBAR_W = 104;

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

	private static final int CARD_W = 80;
	private static final int CARD_H = 80;
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
	protected void drawBackdrop(Draw d) {
		MenuTheme t = MenuTheme.current();
		if (Minecraft.getInstance().level == null) MainMenuScreen.drawBackdrop(d, this.width, this.height);
		d.fill(0, 0, this.width, this.height, t.tint());
		snow.draw(d, this.width, this.height);
	}

	@Override
	protected void draw(Draw d, int mouseX, int mouseY, float delta) {
		hits.clear();
		long now = System.nanoTime();
		double seconds = lastFrame == 0 ? 0 : Math.min(0.1, (now - lastFrame) / 1_000_000_000.0);
		lastFrame = now;
		shownScroll += (scroll - shownScroll) * Math.min(1.0, seconds * 16.0);
		if (Math.abs(scroll - shownScroll) < 0.5) shownScroll = scroll;

		Ui ui = new Ui(d, mouseX, mouseY, hits);
		MenuTheme t = ui.t;
		int w = Math.min(this.width - 20, 560);
		int h = Math.min(this.height - 20, 330);
		int x0 = (this.width - w) / 2;
		int y0 = (this.height - h) / 2;

		float intro = Math.min(1f, (System.currentTimeMillis() - openedAt) / 200f);
		d.push();
		d.translate(0, (1 - (1 - (1 - intro) * (1 - intro))) * 10);

		ui.panel(x0, y0, x0 + w, y0 + h, t.panel());
		drawHeader(ui, x0, y0, w);
		d.fill(x0 + 1, y0 + 26, x0 + w - 1, y0 + 27, t.edge());

		int sideX = x0 + 6;
		int bodyTop = y0 + 32;
		int bodyBottom = y0 + h - 20;
		drawSidebar(ui, sideX, bodyTop, SIDEBAR_W, bodyBottom - bodyTop);
		int cx = sideX + SIDEBAR_W + 8;
		int cy = bodyTop;
		int cw = x0 + w - 8 - cx;
		int ch = bodyBottom - bodyTop;
		switch (tab) {
			case MODS, PROFILES -> {
				if (page != null) drawPage(ui, cx, cy, cw, ch);
				else drawGrid(ui, cx, cy, cw, ch);
			}
			case SETTINGS -> drawPage(ui, cx, cy, cw, ch);
			case WAYPOINTS -> drawWaypoints(ui, cx, cy, cw, ch);
		}
		d.fill(x0 + 8, y0 + h - 18, x0 + w - 8, y0 + h - 17, t.edge());
		drawFooter(ui, x0, y0 + h - 12, w);
		d.pop();
	}

	private void drawHeader(Ui ui, int px, int y0, int pw) {
		int y = y0 + 6;
		HubScreen.drawEmblem(ui.d, px + 8, y - 1, 16, 1, 255, ui.t.text());
		ui.title("3vhy", px + 29, y + 4, ui.t.text());
		ui.text("CLIENT", px + 31 + ui.titleWidth("3vhy"), y + 4, ui.t.muted());

		int total = -4;
		for (Tab each : SHOWN_TABS) total += ui.titleWidth(each.name()) + 16 + 4;
		int x = px + (pw - total) / 2;
		for (Tab each : SHOWN_TABS) {
			String label = each.name();
			int tw = ui.titleWidth(label) + 16;
			boolean active = tab == each || (each == Tab.MODS && tab == Tab.PROFILES);
			boolean hover = ui.hover(x, y, x + tw, y + 15);
			ui.d.roundRect(x, y, x + tw, y + 15, Ui.R_CONTROL, active ? MenuTheme.alpha(ui.t.accent(), 0x40) : hover ? ui.t.cardHover() : 0);
			ui.d.roundOutline(x, y, x + tw, y + 15, Ui.R_CONTROL, active ? ui.t.accent() : ui.t.edge());
			ui.title(label, x + 8, y + 4, active || hover ? ui.t.text() : ui.t.muted());
			ui.hit(x, y, x + tw, y + 15, () -> openTab(each));
			x += tw + 4;
		}

		int right = px + pw - 8;
		boolean closeHover = ui.hover(right - 15, y, right, y + 15);
		ui.d.roundRect(right - 15, y, right, y + 15, Ui.R_CONTROL, closeHover ? MenuTheme.alpha(ui.t.bad(), 0x70) : ui.t.card());
		ui.d.roundOutline(right - 15, y, right, y + 15, Ui.R_CONTROL, closeHover ? ui.t.bad() : ui.t.edge());
		Icons.draw(ui.d, Icons.CLOSE, right - 11, y + 4, closeHover ? 0xFFFFFFFF : ui.t.text());
		ui.hit(right - 15, y, right, y + 15, this::onClose);

		int themeX = right - 34;
		boolean light = ui.t.isLight();
		boolean hover = ui.hover(themeX, y, themeX + 15, y + 15);
		if (hover) ui.d.roundRect(themeX, y, themeX + 15, y + 15, Ui.R_CONTROL, ui.t.cardHover());
		PixelIcons.draw(ui.d, light ? PixelIcons.MOON : PixelIcons.SUN, themeX + 2, y + 2, 1, hover ? ui.t.accent() : ui.t.text());
		ui.hit(themeX, y, themeX + 15, y + 15, () -> Modules.THEME_MODE.next());
		if (hover) ui.tooltip(light ? "Dark theme" : "Light theme", themeX + 8, y + 18);
	}

	/** Profiles down the left, like Lunar's: click one to switch to it; Save as new profile and Edit HUD layout at the bottom. */
	private void drawSidebar(Ui ui, int x, int y, int w, int h) {
		MenuTheme t = ui.t;
		ui.d.roundRect(x, y, x + w, y + h, Ui.R_CARD, MenuTheme.alpha(0x000000, t.isLight() ? 0x14 : 0x40));
		ui.small("PROFILES", x + 6, y + 5, 0.75f, t.muted());
		Config config = Config.get();
		int listTop = y + 14;
		int buttonsH = 16 + 4 + 16 + 4 + 16;
		int listBottom = y + h - buttonsH - 8;
		ui.clip(x, listTop, x + w, listBottom);
		int ry = listTop;
		for (String name : config.profileNames()) {
			boolean active = name.equals(config.profile);
			boolean hover = ui.hover(x + 3, ry, x + w - 3, ry + 16);
			if (active || hover) ui.d.roundRect(x + 3, ry, x + w - 3, ry + 16, Ui.R_CONTROL, active ? t.cardOn() : t.cardHover());
			if (active) ui.d.fill(x + 3, ry + 3, x + 5, ry + 13, t.accent());
			ui.text(ui.trim(name, w - 30), x + 9, ry + 4, active ? t.text() : MenuTheme.alpha(t.text(), 0xC0));
			if (!active) {
				ui.hit(x + 3, ry, x + w - 3, ry + 16, () -> {
					config.loadProfile(name);
					notice("Switched to " + name);
				});
				if (hover && !name.equals(Config.DEFAULT_PROFILE)) {
					int dx = x + w - 18;
					boolean delHover = ui.hover(dx, ry + 2, dx + 13, ry + 14);
					PixelIcons.draw(ui.d, PixelIcons.TRASH, dx, ry + 2, 1, delHover ? t.bad() : t.muted());
					ui.hit(dx, ry + 2, dx + 13, ry + 14, () -> {
						if (config.deleteProfile(name)) notice("Deleted " + name);
					});
				}
			} else {
				ui.small("ACTIVE", x + w - 6 - Math.round(ui.width("ACTIVE") * 0.6f), ry + 5, 0.6f, t.accent());
			}
			ry += 18;
		}
		ui.endClip();

		int by = y + h - buttonsH - 2;
		ui.field(x + 3, by, w - 6, 16, profileName, "Profile name", focus == Focus.PROFILE_NAME, null);
		ui.hit(x + 3, by, x + w - 3, by + 16, () -> focus = Focus.PROFILE_NAME);
		by += 20;
		sideButton(ui, x + 3, by, w - 6, 16, "Save as new profile", false, this::createProfile);
		by += 20;
		sideButton(ui, x + 3, by, w - 6, 16, "Edit HUD layout", true, () -> minecraft().setScreen(new HubScreen(this).editOnly()));
	}

	/** A sidebar button with its label in small capitals, so the whole label fits like Lunar's. */
	private static void sideButton(Ui ui, int x, int y, int w, int h, String label, boolean primary, Runnable action) {
		MenuTheme t = ui.t;
		boolean hover = ui.hover(x, y, x + w, y + h);
		int fill = primary ? (hover ? Draw.mix(t.accent(), 0xFFFFFFFF, 0.12) : t.accent()) : hover ? t.cardHover() : t.card();
		ui.card(x, y, x + w, y + h, fill, primary ? fill : hover ? t.edgeStrong() : t.edge());
		float scale = 0.75f;
		String text = ui.trim(label.toUpperCase(Locale.ROOT), (int) ((w - 6) / scale));
		float textW = ui.width(text) * scale;
		ui.small(text, x + (w - textW) / 2f, y + (h - 6) / 2f, scale, primary ? t.onAccent() : t.text());
		ui.hit(x, y, x + w, y + h, action);
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

	private void drawGrid(Ui ui, int cx, int cy, int cw, int ch) {
		int x = cx;
		record Chip(String label, Category category, boolean newOnly) {
		}
		List<Chip> chips = new ArrayList<>();
		chips.add(new Chip("ALL", null, false));
		chips.add(new Chip("NEW", null, true));
		for (Category each : Category.values()) chips.add(new Chip(each.label.toUpperCase(Locale.ROOT), each, false));
		for (Chip chip : chips) {
			int tw = ui.width(chip.label()) + 10;
			boolean active = category == chip.category() && newOnly == chip.newOnly();
			boolean hover = ui.hover(x, cy, x + tw, cy + 13);
			int fill = active ? (chip.newOnly() ? MenuTheme.alpha(ui.t.good(), 0xC0) : MenuTheme.alpha(ui.t.accent(), 0xD8))
					: hover ? ui.t.cardHover() : ui.t.card();
			ui.d.roundRect(x, cy, x + tw, cy + 13, Ui.R_CONTROL, fill);
			ui.text(chip.label(), x + 5, cy + 3, active ? ui.t.onAccent() : hover ? ui.t.text() : ui.t.muted());
			ui.hit(x, cy, x + tw, cy + 13, () -> {
				category = chip.category();
				newOnly = chip.newOnly();
				resetScroll();
			});
			x += tw + 3;
		}
		int searchW = Math.max(60, Math.min(120, cx + cw - x - 6));
		int searchX = cx + cw - searchW;
		ui.field(searchX, cy - 1, searchW, 15, search, "Search", focus == Focus.SEARCH, SEARCH_ICON);
		ui.hit(searchX, cy - 1, searchX + searchW, cy + 14, () -> focus = Focus.SEARCH);
		int gy = cy + 17;
		int gh = ch - 17;
		List<Module> shown = shownModules();
		int gap = 6;
		int cols = Math.max(2, (cw - 6 + gap) / (CARD_W + gap));
		int cardW = (cw - 6 - (cols - 1) * gap) / cols;
		int rows = (shown.size() + cols - 1) / cols;
		int contentH = Math.max(0, rows * (CARD_H + gap) - gap);
		maxScroll = Math.max(0, contentH - gh);
		scroll = Math.max(0, Math.min(scroll, maxScroll));
		shownScroll = Math.max(0, Math.min(shownScroll, maxScroll));

		if (shown.isEmpty()) {
			String none = "No mods match \"" + search + "\"";
			ui.text(none, cx + (cw - ui.width(none)) / 2, gy + gh / 2 - 4, ui.t.muted());
			return;
		}
		ui.clip(cx, gy, cx + cw, gy + gh);
		for (int i = 0; i < shown.size(); i++) {
			int x1 = cx + (i % cols) * (cardW + gap);
			int y1 = gy + (i / cols) * (CARD_H + gap) - (int) Math.round(shownScroll);
			if (y1 + CARD_H < gy || y1 > gy + gh) continue;
			card(ui, shown.get(i), x1, y1, cardW);
		}
		ui.endClip();
		ui.scrollbar(cx + cw - 2, gy, gh, shownScroll, maxScroll, contentH);
	}

	static boolean hasPage(Module module) {
		return !module.settings().isEmpty() || !module.toggleable()
				|| HudWidgets.ALL.stream().anyMatch(widget -> widget.module == module);
	}

	/** A Lunar-style card: icon and name, an OPTIONS bar, and an ENABLED / DISABLED bar that switches the mod. */
	private void card(Ui ui, Module module, int x, int y, int w) {
		MenuTheme t = ui.t;
		boolean on = module.enabled();
		boolean toggleable = module.toggleable();
		boolean hasPage = hasPage(module);
		boolean hover = ui.hover(x, y, x + w, y + CARD_H);
		ui.card(x, y, x + w, y + CARD_H, hover ? t.cardHover() : t.card(), hover ? t.edgeStrong() : t.edge());
		if (module.isNew()) ui.small("NEW", x + 5, y + 4, 0.6f, t.good());

		int iconColor = !toggleable || on ? t.text() : t.muted();
		PixelIcons.draw(ui.d, PixelIcons.forModule(module), x + (w - 24) / 2, y + 7, 2, iconColor);
		String name = ui.trim(module.name, w - 8);
		ui.text(name, x + (w - ui.width(name)) / 2, y + 34, !toggleable || on ? t.text() : MenuTheme.alpha(t.text(), 0xB8));

		Runnable openPage = () -> openModule(module);
		ui.hit(x, y, x + w, y + CARD_H, hasPage ? openPage : module::toggle, openPage);
		int barX1 = x + 4;
		int barX2 = x + w - 4;
		int optionsY = y + CARD_H - 31;
		if (hasPage) {
			boolean optionsHover = ui.hover(barX1, optionsY, barX2, optionsY + 13);
			ui.d.roundRect(barX1, optionsY, barX2, optionsY + 13, 3, optionsHover ? t.cardOn() : MenuTheme.alpha(0x000000, t.isLight() ? 0x18 : 0x55));
			String options = "OPTIONS";
			int ow = ui.width(options);
			int ox = barX1 + (barX2 - barX1 - ow - 10) / 2;
			ui.text(options, ox, optionsY + 3, optionsHover ? t.text() : MenuTheme.alpha(t.text(), 0xC8));
			Icons.draw(ui.d, Icons.GEAR, ox + ow + 3, optionsY + 3, optionsHover ? t.accent() : t.muted());
			ui.hit(barX1, optionsY, barX2, optionsY + 13, openPage);
		}
		int barY = y + CARD_H - 16;
		String label = !toggleable ? "ALWAYS ON" : on ? "ENABLED" : "DISABLED";
		int fill = !toggleable ? MenuTheme.alpha(t.accent(), 0x55) : MenuTheme.alpha(on ? t.good() : t.bad(), 0xB8);
		boolean barHover = ui.hover(barX1, barY, barX2, barY + 12);
		ui.d.roundRect(barX1, barY, barX2, barY + 12, 3, barHover && toggleable ? Draw.mix(fill, 0xFFFFFFFF, 0.15) : fill);
		String shown = ui.trimTitle(label, barX2 - barX1 - 4);
		ui.d.text(Ui.bold(shown), barX1 + (barX2 - barX1 - ui.boldWidth(shown)) / 2, barY + 2, 0xFFFFFFFF, false);
		ui.hit(barX1, barY, barX2, barY + 12, toggleable ? module::toggle : openPage, openPage);
	}

	private void drawPage(Ui ui, int cx, int cy, int cw, int ch) {
		MenuTheme t = ui.t;
		Module module = page;
		// Back, icon, name and description
		boolean backHover = ui.hover(cx, cy, cx + 16, cy + 16);
		ui.d.roundRect(cx, cy, cx + 16, cy + 16, 8, backHover ? t.cardHover() : t.card());
		ui.d.roundOutline(cx, cy, cx + 16, cy + 16, 8, backHover ? t.edgeStrong() : t.edge());
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
		if (hover) ui.d.roundRect(x, y, x + w, y + ROW_H, Ui.R_CARD, ui.t.cardHover());
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
			ui.d.roundRect(bx, y + 6, bx + boxW, y + 20, 7, hover ? t.cardHover() : t.field());
			ui.d.roundOutline(bx, y + 6, bx + boxW, y + 20, 7, hover ? t.edgeStrong() : t.edge());
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
				if (selected) ui.d.roundRect(sx - 2, y + 6, sx + size + 2, y + 10 + size, 4, t.accent());
				ui.d.roundRect(sx, y + 8, sx + size, y + 8 + size, 3, swatch);
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
				ui.d.roundRect(sx, y, sx + pw, y + 12, 6, hover ? t.cardHover() : t.card());
				ui.d.roundOutline(sx, y, sx + pw, y + 12, 6, hover ? t.edgeStrong() : t.edge());
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
			if (delHover) ui.d.roundRect(dx, y + 3, dx + 16, y + 19, 4, MenuTheme.alpha(t.bad(), 0x50));
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
		ui.d.roundRect(bx, y + 6, bx + boxW, y + 20, 7, hover ? t.cardHover() : t.field());
		ui.d.roundOutline(bx, y + 6, bx + boxW, y + 20, 7, hover ? t.edgeStrong() : t.edge());
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
			if (delHover) ui.d.roundRect(dx, y + 3, dx + 16, y + 19, 4, MenuTheme.alpha(t.bad(), 0x50));
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
		ui.field(fx, y, fieldW, 16, hotkeyText, "Message or /command, like /home", focus == Focus.HOTKEY_TEXT, PixelIcons.PENCIL);
		ui.hit(fx, y, fx + fieldW, y + 16, () -> focus = Focus.HOTKEY_TEXT);
		ui.button(x + w - addW, y, addW, 16, "+ Add", true, this::addHotkey);
		y += 22;
		String hint = "Start with / to run a command. Works in game with no menu or chat open. Mouse side buttons work too.";
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
			if (delHover) ui.d.roundRect(dx, y + 3, dx + 16, y + 19, 4, MenuTheme.alpha(t.bad(), 0x50));
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
			ui.d.roundRect(cx + 6, y + 7, cx + 16, y + 17, 3, waypoint.color | 0xFF000000);
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
			if (eyeHover) ui.d.roundRect(ex, y + 5, ex + 16, y + 19, 4, t.cardOn());
			PixelIcons.draw(ui.d, waypoint.visible ? PixelIcons.EYE : PixelIcons.EYE_OFF, ex + 2, y + 6, 1,
					waypoint.visible ? t.text() : t.muted());
			ui.hit(ex, y + 5, ex + 16, y + 19, () -> {
				waypoint.visible = !waypoint.visible;
				Config.saveSoon();
			});
			int dx = right - 18;
			boolean delHover = ui.hover(dx, y + 5, dx + 16, y + 19);
			if (delHover) ui.d.roundRect(dx, y + 5, dx + 16, y + 19, 4, MenuTheme.alpha(t.bad(), 0x50));
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

	private void drawFooter(Ui ui, int px, int y, int pw) {
		MenuTheme t = ui.t;
		int enabled = 0;
		int total = 0;
		for (Module module : Modules.ALL) {
			if (!module.toggleable()) continue;
			total++;
			if (module.enabled()) enabled++;
		}
		String left = notice != null && System.currentTimeMillis() - noticeAt < 2500 ? notice.toUpperCase(Locale.ROOT)
				: enabled + " / " + total + " MODS ON  ·  PROFILE " + Config.get().profile.toUpperCase(Locale.ROOT);
		ui.small(ui.trim(left, (int) ((pw / 2 - 8) / 0.75f)), px + 8, y + 1, 0.75f, t.muted());

		float scale = 0.75f;
		boolean connected = DiscordPresence.connected();
		String status = !Modules.DISCORD.enabled() ? "DISCORD OFF"
				: connected ? "DISCORD CONNECTED" : "DISCORD NOT OPEN";
		float textW = ui.width(status) * scale;
		float sx = px + pw - 8 - textW;
		ui.small(status, sx, y + 1, scale, t.muted());
		int dot = (int) sx - 8;
		ui.dot(dot, y + 1, 5, connected ? t.good() : MenuTheme.alpha(t.muted(), 0x90));
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
				if (!hit.contains(event.x(), event.y())) continue;
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
			draggingSlider.setFraction((event.x() - sliderX) / sliderW);
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
