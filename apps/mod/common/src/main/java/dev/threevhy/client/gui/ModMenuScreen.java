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
import dev.threevhy.client.module.Setting;
import dev.threevhy.client.module.Waypoints;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
 * The 3vhy mod menu, laid out like NoRisk's: a sidebar, MODS / PROFILES / WAYPOINTS tabs, a theme
 * switch and search along the top, and wide mod cards. A card toggles its mod; "..." (or right
 * click) opens the mod's settings.
 */
public final class ModMenuScreen extends CompatScreen {
	public enum Tab {
		MODS,
		PROFILES,
		WAYPOINTS
	}

	private enum Focus {
		SEARCH,
		PROFILE_NAME,
		WAYPOINT_NAME
	}

	private static final int CARD_H = 42;
	private static final int ROW_H = 26;

	private final Screen parent;
	private final List<Ui.Hit> hits = Ui.newHits();
	private final Snow snow = new Snow();
	private final long openedAt = System.currentTimeMillis();

	private Tab tab = Tab.MODS;
	private Category category = null;
	private Module page = null;
	private Focus focus = Focus.SEARCH;
	private String search = "";
	private String profileName = "";
	private Waypoint renaming = null;
	private String waypointName = "";
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

	/** Opens a mod's settings page. Returns this, for use when creating the screen. */
	public ModMenuScreen openModule(Module module) {
		if (module == Modules.PROFILES) return openTab(Tab.PROFILES);
		tab = Tab.MODS;
		page = module;
		resetScroll();
		return this;
	}

	public ModMenuScreen openTab(Tab next) {
		tab = next;
		page = null;
		renaming = null;
		focus = next == Tab.PROFILES ? Focus.PROFILE_NAME : Focus.SEARCH;
		resetScroll();
		return this;
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
		int w = Math.min(this.width - 20, 520);
		int h = Math.min(this.height - 20, 320);
		int x0 = (this.width - w) / 2;
		int y0 = (this.height - h) / 2;
		int sideW = 46;
		int px = x0 + sideW + 6;
		int pw = w - sideW - 6;

		float intro = Math.min(1f, (System.currentTimeMillis() - openedAt) / 200f);
		d.push();
		d.translate(0, (1 - (1 - (1 - intro) * (1 - intro))) * 10);

		drawSidebar(ui, x0, y0, sideW, h);
		ui.panel(px, y0, px + pw, y0 + h, t.panel());
		drawHeader(ui, px, y0, pw);
		d.fill(px + 8, y0 + 30, px + pw - 8, y0 + 31, t.edge());

		int cx = px + 8;
		int cy = y0 + 37;
		int cw = pw - 16;
		int ch = h - 37 - 22;
		switch (tab) {
			case MODS -> {
				if (page != null) drawPage(ui, cx, cy, cw, ch);
				else drawGrid(ui, cx, cy, cw, ch);
			}
			case PROFILES -> drawProfiles(ui, cx, cy, cw, ch);
			case WAYPOINTS -> drawWaypoints(ui, cx, cy, cw, ch);
		}
		d.fill(px + 8, y0 + h - 21, px + pw - 8, y0 + h - 20, t.edge());
		drawFooter(ui, px, y0 + h - 15, pw);
		d.pop();
	}

	private void drawSidebar(Ui ui, int x, int y, int w, int h) {
		ui.panel(x, y, x + w, y + h, ui.t.sidebar());
		record Item(String label, String[] icon, Runnable action, boolean active) {
		}
		List<Item> items = List.of(
				new Item("Hub", PixelIcons.SNOWFLAKE, () -> minecraft().setScreen(new HubScreen(this)), false),
				new Item("Crosshair", PixelIcons.forModule(Modules.CROSSHAIR), () -> openModule(Modules.CROSSHAIR),
						page == Modules.CROSSHAIR),
				new Item("Theme", PixelIcons.forModule(Modules.THEME), () -> openModule(Modules.THEME), page == Modules.THEME),
				new Item("Discord", PixelIcons.forModule(Modules.DISCORD), () -> openModule(Modules.DISCORD),
						page == Modules.DISCORD),
				new Item("Options", PixelIcons.GEAR, () -> minecraft().setScreen(dev.threevhy.client.compat.Compat.optionsScreen(this)),
						false));
		int iy = y + 8;
		for (Item item : items) {
			boolean hover = ui.hover(x + 3, iy, x + w - 3, iy + 38);
			if (item.active() || hover) {
				ui.d.roundRect(x + 3, iy, x + w - 3, iy + 38, 2, item.active() ? ui.t.cardOn() : ui.t.cardHover());
			}
			int color = item.active() ? ui.t.accent() : hover ? ui.t.text() : MenuTheme.alpha(ui.t.text(), 0xD0);
			PixelIcons.draw(ui.d, item.icon(), x + (w - 24) / 2, iy + 4, 2, color);
			String label = item.label().toUpperCase(Locale.ROOT);
			float scale = 0.5f;
			ui.small(label, x + w / 2f - ui.width(label) * scale / 2f, iy + 31, scale, color);
			ui.hit(x + 3, iy, x + w - 3, iy + 38, item.action());
			iy += 42;
			if (iy + 38 > y + h) break;
		}
	}

	private void drawHeader(Ui ui, int px, int y0, int pw) {
		int y = y0 + 8;
		int x = px + 8;
		for (Tab each : Tab.values()) {
			String label = each.name();
			int tw = ui.titleWidth(label) + 14;
			boolean active = tab == each;
			boolean hover = ui.hover(x, y, x + tw, y + 16);
			if (active) {
				ui.d.roundRect(x, y, x + tw, y + 16, 2, ui.t.cardOn());
				ui.d.roundOutline(x, y, x + tw, y + 16, 2, ui.t.edgeStrong());
			}
			ui.title(label, x + 7, y + 4, active || hover ? ui.t.text() : ui.t.muted());
			ui.hit(x, y, x + tw, y + 16, () -> openTab(each));
			x += tw + 4;
		}

		int searchW = Math.max(70, Math.min(120, pw / 4));
		int searchX = px + pw - 8 - searchW;
		ui.field(searchX, y, searchW, 16, search, "Search...", tab == Tab.MODS && focus == Focus.SEARCH, SEARCH_ICON);
		ui.hit(searchX, y, searchX + searchW, y + 16, () -> {
			if (tab != Tab.MODS) openTab(Tab.MODS);
			page = null;
			focus = Focus.SEARCH;
		});

		int themeX = searchX - 20;
		boolean light = ui.t.isLight();
		boolean hover = ui.hover(themeX, y, themeX + 16, y + 16);
		if (hover) ui.d.roundRect(themeX, y, themeX + 16, y + 16, 2, ui.t.cardHover());
		PixelIcons.draw(ui.d, light ? PixelIcons.MOON : PixelIcons.SUN, themeX + 2, y + 2, 1, hover ? ui.t.accent() : ui.t.text());
		ui.hit(themeX, y, themeX + 16, y + 16, () -> Modules.THEME_MODE.next());
		if (hover) ui.tooltip(light ? "Dark theme" : "Light theme", themeX + 8, y + 19);
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
			if (!query.isEmpty() && !module.name.toLowerCase(Locale.ROOT).contains(query)
					&& !module.description.toLowerCase(Locale.ROOT).contains(query)) {
				continue;
			}
			shown.add(module);
		}
		return shown;
	}

	private void drawGrid(Ui ui, int cx, int cy, int cw, int ch) {
		// Category chips
		int x = cx;
		List<Category> chips = new ArrayList<>();
		chips.add(null);
		for (Category each : Category.values()) chips.add(each);
		for (Category each : chips) {
			String label = each == null ? "ALL" : each.label.toUpperCase(Locale.ROOT);
			int tw = ui.width(label) + 10;
			boolean active = category == each;
			boolean hover = ui.hover(x, cy, x + tw, cy + 12);
			ui.d.roundRect(x, cy, x + tw, cy + 12, 2, active ? MenuTheme.alpha(ui.t.accent(), 0xD8) : hover ? ui.t.cardHover() : 0);
			ui.text(label, x + 5, cy + 2, active ? ui.t.onAccent() : hover ? ui.t.text() : ui.t.muted());
			ui.hit(x, cy, x + tw, cy + 12, () -> {
				category = each;
				resetScroll();
			});
			x += tw + 3;
		}
		int gy = cy + 17;
		int gh = ch - 17;
		List<Module> shown = shownModules();
		int gap = 6;
		int colW = (cw - 6 - gap) / 2;
		int rows = (shown.size() + 1) / 2;
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
			int x1 = cx + (i % 2) * (colW + gap);
			int y1 = gy + (i / 2) * (CARD_H + gap) - (int) Math.round(shownScroll);
			if (y1 + CARD_H < gy || y1 > gy + gh) continue;
			card(ui, shown.get(i), x1, y1, colW);
		}
		ui.endClip();
		ui.scrollbar(cx + cw - 2, gy, gh, shownScroll, maxScroll, contentH);
	}

	private void card(Ui ui, Module module, int x, int y, int w) {
		MenuTheme t = ui.t;
		boolean on = module.enabled();
		boolean toggleable = module.toggleable();
		boolean hover = ui.hover(x, y, x + w, y + CARD_H);
		int fill = hover ? t.cardHover() : toggleable && on ? t.cardOn() : t.card();
		ui.d.roundRect(x, y, x + w, y + CARD_H, 2, fill);
		ui.d.roundOutline(x, y, x + w, y + CARD_H, 2, toggleable && on ? t.edgeStrong() : t.edge());

		int iconColor = !toggleable || on ? t.text() : t.muted();
		PixelIcons.draw(ui.d, PixelIcons.forModule(module), x + 8, y + (CARD_H - 24) / 2, 2, iconColor);
		if (module.isNew()) ui.small("NEW", x + 4, y + 3, 0.6f, t.badge());

		boolean hasPage = !module.settings().isEmpty() || !toggleable || HudWidgets.ALL.stream().anyMatch(widget -> widget.module == module);
		int textX = x + 40;
		int textW = w - 40 - (hasPage ? 22 : 8);
		ui.title(ui.trimTitle(module.name, textW), textX, y + 7, !toggleable || on ? t.text() : MenuTheme.alpha(t.text(), 0xB0));
		List<String> lines = ui.wrap(module.description, textW, 0.75f, 2);
		for (int i = 0; i < lines.size(); i++) ui.small(lines.get(i), textX, y + 19 + i * 8, 0.75f, t.muted());

		if (toggleable) {
			ui.d.fill(x + w - 7, y + 4, x + w - 3, y + 8, on ? t.good() : MenuTheme.alpha(t.muted(), 0x80));
		}
		if (hasPage) {
			int bx = x + w - 19;
			int by = y + (CARD_H - 12) / 2;
			boolean dotsHover = ui.hover(bx, by, bx + 14, by + 12);
			if (dotsHover) ui.d.roundRect(bx, by, bx + 14, by + 12, 2, t.cardOn());
			for (int i = 0; i < 3; i++) ui.d.fill(bx + 3 + i * 3, by + 7, bx + 5 + i * 3, by + 9, dotsHover ? t.accent() : t.text());
		}

		// Clicks: the card toggles (or opens the page); the dots, added last so they're on top, open the page
		Runnable openPage = () -> openModule(module);
		ui.hit(x, y, x + w, y + CARD_H, toggleable ? module::toggle : openPage, openPage);
		if (hasPage) ui.hit(x + w - 19, y + (CARD_H - 12) / 2, x + w - 5, y + (CARD_H + 12) / 2, openPage);
	}

	private void drawPage(Ui ui, int cx, int cy, int cw, int ch) {
		MenuTheme t = ui.t;
		Module module = page;
		// Back, icon, name and description
		boolean backHover = ui.hover(cx, cy, cx + 16, cy + 16);
		ui.d.roundRect(cx, cy, cx + 16, cy + 16, 2, backHover ? t.cardHover() : t.card());
		ui.d.roundOutline(cx, cy, cx + 16, cy + 16, 2, backHover ? t.edgeStrong() : t.edge());
		Icons.draw(ui.d, Icons.BACK, cx + 4, cy + 4, t.text());
		ui.hit(cx, cy, cx + 16, cy + 16, () -> {
			page = null;
			resetScroll();
		});
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
		int count = settings.size() + (widget != null ? 1 : 0) + (module == Modules.THEME ? 1 : 0);
		int contentH = count * (ROW_H + 4);
		maxScroll = Math.max(0, contentH - listH);
		scroll = Math.max(0, Math.min(scroll, maxScroll));
		shownScroll = Math.max(0, Math.min(shownScroll, maxScroll));

		ui.clip(cx, listY, cx + cw, listY + listH);
		int y = listY - (int) Math.round(shownScroll);
		int rowW = cw - 6;
		if (widget != null) {
			row(ui, cx, y, rowW, "Position", "Drag it anywhere on screen in the hub (Right Shift)");
			ui.button(cx + rowW - 96, y + 6, 88, 14, "Move on screen", false, () -> minecraft().setScreen(new HubScreen(this)));
			y += ROW_H + 4;
		}
		for (Setting setting : settings) {
			settingRow(ui, setting, cx, y, rowW);
			y += ROW_H + 4;
		}
		if (module == Modules.THEME) {
			row(ui, cx, y, rowW, "HUD layout", "Put every HUD widget back where it started");
			ui.button(cx + rowW - 96, y + 6, 88, 14, "Reset layout", false, () -> {
				for (HudWidget each : HudWidgets.ALL) each.resetPosition();
				notice("HUD layout reset");
			});
		}
		ui.endClip();
		ui.scrollbar(cx + cw - 2, listY, listH, shownScroll, maxScroll, contentH);
	}

	private void row(Ui ui, int x, int y, int w, String name, String description) {
		boolean hover = ui.hover(x, y, x + w, y + ROW_H);
		ui.d.roundRect(x, y, x + w, y + ROW_H, 2, hover ? ui.t.cardHover() : ui.t.card());
		ui.d.roundOutline(x, y, x + w, y + ROW_H, 2, ui.t.edge());
		ui.text(name, x + 8, y + 5, ui.t.text());
		ui.small(ui.trim(description, (int) ((w - 150) / 0.75f)), x + 8, y + 16, 0.75f, ui.t.muted());
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
			int boxW = 92;
			int bx = right - boxW;
			boolean hover = ui.hover(bx, y + 6, bx + boxW, y + 20);
			ui.d.roundRect(bx, y + 6, bx + boxW, y + 20, 2, hover ? t.cardHover() : t.field());
			ui.d.roundOutline(bx, y + 6, bx + boxW, y + 20, 2, hover ? t.edgeStrong() : t.edge());
			ui.text("<", bx + 4, y + 9, t.muted());
			ui.text(">", bx + boxW - 9, y + 9, t.muted());
			String value = choice.get().toUpperCase(Locale.ROOT);
			ui.text(value, bx + (boxW - ui.width(value)) / 2, y + 9, t.text());
			ui.hit(bx, y + 6, bx + boxW / 2, y + 20, choice::previous);
			ui.hit(bx + boxW / 2, y + 6, bx + boxW, y + 20, choice::next);
		} else if (setting instanceof Setting.Color color) {
			int[] swatches = Setting.Color.SWATCHES;
			int size = 10;
			int sx = right - swatches.length * (size + 2) + 2;
			for (int swatch : swatches) {
				boolean selected = (color.get() | 0xFF000000) == swatch;
				if (selected) ui.d.fill(sx - 1, y + 7, sx + size + 1, y + 9 + size, t.accent());
				ui.d.fill(sx, y + 8, sx + size, y + 8 + size, swatch);
				int pick = swatch;
				ui.hit(sx, y + 8, sx + size, y + 8 + size, () -> color.set(pick));
				sx += size + 2;
			}
		}
	}

	private void drawProfiles(Ui ui, int cx, int cy, int cw, int ch) {
		MenuTheme t = ui.t;
		Config config = Config.get();
		int fieldW = cw - 78;
		ui.field(cx, cy, fieldW, 16, profileName, "New profile name (type here)", focus == Focus.PROFILE_NAME, null);
		ui.hit(cx, cy, cx + fieldW, cy + 16, () -> focus = Focus.PROFILE_NAME);
		ui.button(cx + fieldW + 6, cy, 72, 16, "+ Create", true, this::createProfile);

		List<String> names = config.profileNames();
		int listY = cy + 24;
		int listH = ch - 24;
		int contentH = names.size() * (24 + 4);
		maxScroll = Math.max(0, contentH - listH);
		scroll = Math.max(0, Math.min(scroll, maxScroll));
		shownScroll = Math.max(0, Math.min(shownScroll, maxScroll));
		ui.clip(cx, listY, cx + cw, listY + listH);
		int y = listY - (int) Math.round(shownScroll);
		for (String name : names) {
			boolean active = name.equals(config.profile);
			boolean hover = ui.hover(cx, y, cx + cw - 6, y + 24);
			ui.d.roundRect(cx, y, cx + cw - 6, y + 24, 2, active ? t.cardOn() : hover ? t.cardHover() : t.card());
			ui.d.roundOutline(cx, y, cx + cw - 6, y + 24, 2, active ? t.edgeStrong() : t.edge());
			PixelIcons.draw(ui.d, PixelIcons.forModule(Modules.PROFILES), cx + 6, y + 6, 1, t.text());
			ui.title(ui.trimTitle(name, cw - 180), cx + 24, y + 8, t.text());
			int right = cx + cw - 12;
			if (active) {
				String badge = "ACTIVE";
				int bw = ui.width(badge) + 8;
				ui.d.roundRect(right - bw, y + 6, right, y + 18, 2, MenuTheme.alpha(t.accent(), 0xD8));
				ui.text(badge, right - bw + 4, y + 8, t.onAccent());
			} else {
				ui.button(right - 58, y + 5, 58, 14, "Load", false, () -> {
					config.loadProfile(name);
					notice("Switched to " + name);
				});
				int dx = right - 58 - 20;
				boolean delHover = ui.hover(dx, y + 5, dx + 16, y + 19);
				if (delHover) ui.d.roundRect(dx, y + 5, dx + 16, y + 19, 2, MenuTheme.alpha(t.bad(), 0x50));
				PixelIcons.draw(ui.d, PixelIcons.TRASH, dx + 2, y + 6, 1, delHover ? t.bad() : t.muted());
				ui.hit(dx, y + 5, dx + 16, y + 19, () -> {
					if (config.deleteProfile(name)) notice("Deleted " + name);
				});
			}
			y += 28;
		}
		ui.endClip();
		ui.scrollbar(cx + cw - 2, listY, listH, shownScroll, maxScroll, contentH);
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
			ui.d.roundRect(cx, y, cx + cw - 6, y + 24, 2, hover ? t.cardHover() : t.card());
			ui.d.roundOutline(cx, y, cx + cw - 6, y + 24, 2, t.edge());
			// Colour: click to change
			ui.d.fill(cx + 6, y + 7, cx + 16, y + 17, waypoint.color | 0xFF000000);
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
			if (eyeHover) ui.d.roundRect(ex, y + 5, ex + 16, y + 19, 2, t.cardOn());
			PixelIcons.draw(ui.d, waypoint.visible ? PixelIcons.EYE : PixelIcons.EYE_OFF, ex + 2, y + 6, 1,
					waypoint.visible ? t.text() : t.muted());
			ui.hit(ex, y + 5, ex + 16, y + 19, () -> {
				waypoint.visible = !waypoint.visible;
				Config.saveSoon();
			});
			int dx = right - 18;
			boolean delHover = ui.hover(dx, y + 5, dx + 16, y + 19);
			if (delHover) ui.d.roundRect(dx, y + 5, dx + 16, y + 19, 2, MenuTheme.alpha(t.bad(), 0x50));
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

		boolean connected = DiscordPresence.connected();
		String status = !Modules.DISCORD.enabled() ? "DISCORD OFF"
				: connected ? "DISCORD CONNECTED" : "DISCORD NOT OPEN";
		float scale = 0.75f;
		float textW = ui.width(status) * scale;
		float sx = px + pw - 8 - textW;
		ui.small(status, sx, y + 1, scale, t.muted());
		int dot = (int) sx - 8;
		ui.d.fill(dot, y + 1, dot + 5, y + 6, connected ? t.good() : MenuTheme.alpha(t.muted(), 0x90));
	}

	private Minecraft minecraft() {
		return Minecraft.getInstance();
	}

	// Input

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		int button = event.button();
		if (button == 0 || button == 1) {
			for (int i = hits.size() - 1; i >= 0; i--) {
				Ui.Hit hit = hits.get(i);
				if (!hit.contains(event.x(), event.y())) continue;
				Runnable action = button == 0 ? hit.action() : hit.rightAction();
				if (action == null) continue;
				if (focus == Focus.WAYPOINT_NAME) finishRename();
				minecraft().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
				action.run();
				return true;
			}
		}
		if (focus == Focus.WAYPOINT_NAME) finishRename();
		return super.mouseClicked(event, doubleClick);
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
		switch (focus) {
			case PROFILE_NAME -> {
				if (profileName.length() < 24) profileName += typed;
			}
			case WAYPOINT_NAME -> {
				if (waypointName.length() < 24) waypointName += typed;
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
		if (key == GLFW.GLFW_KEY_BACKSPACE) {
			switch (focus) {
				case PROFILE_NAME -> profileName = dropLast(profileName);
				case WAYPOINT_NAME -> waypointName = dropLast(waypointName);
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
			return true;
		}
		if (key == GLFW.GLFW_KEY_ESCAPE) {
			if (focus == Focus.WAYPOINT_NAME) {
				renaming = null;
				focus = Focus.SEARCH;
				return true;
			}
			if (page != null) {
				page = null;
				resetScroll();
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
