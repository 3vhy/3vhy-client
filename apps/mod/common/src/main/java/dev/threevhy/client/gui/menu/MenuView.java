package dev.threevhy.client.gui.menu;

import dev.threevhy.client.Config;
import dev.threevhy.client.Draw;
import dev.threevhy.client.module.Module;
import dev.threevhy.client.module.Module.Category;
import dev.threevhy.client.module.Modules;
import dev.threevhy.client.ui.Anim;
import dev.threevhy.client.ui.Ease;
import dev.threevhy.client.ui.Hits;
import dev.threevhy.client.ui.Keys;
import dev.threevhy.client.ui.Logo;
import dev.threevhy.client.ui.Paint;
import dev.threevhy.client.ui.Sky;
import dev.threevhy.client.ui.Tokens;
import dev.threevhy.client.ui.Type;
import dev.threevhy.client.ui.Type.Weight;
import dev.threevhy.client.ui.Widgets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The 3vhy mod menu, laid out like the Render menu on the website: the logo, a row of round tab buttons
 * with the active one as a sliding gradient pill, a search field ({@code /}), the tab's name and an
 * "N of M active" count, then a grid of module cards, each with a gradient icon tile, its name and
 * one line about it, a settings button and a switch. Click a card to switch it, right-click it (or its
 * settings button) for its settings, Esc for the Orbit.
 *
 * <p>Everything is laid out on the website mockup's 1600 x 900 canvas and scaled to fit the window, so
 * the menu is the same shape at any GUI scale, and drawn with {@link Paint}, so it's sharp at any size.
 * This class has no Minecraft screen in it; {@link MenuScreen} hosts it.
 */
public final class MenuView {
	/** What the menu needs from the game. */
	public interface Host {
		/** The key on a module's card (C for Zoom), or null. */
		String keyFor(Module module);

		String playerName();

		/** Draws your head in a {@code size} square with corners of {@code radius}. */
		void drawFace(Draw d, float x, float y, float size, float radius);

		boolean inWorld();

		void openHudEditor();

		/** Esc at the top: back to the Orbit, or out of the menu. */
		void back();

		void clickSound();

		SettingsSheet.Extras extras();
	}

	/** A tab: the Profile page or a module category. */
	public record Tab(String name, String icon, String tagline, Category category) {
	}

	static final float DESIGN_W = 1600;
	static final float DESIGN_H = 900;
	static final float CONTENT_W = 1456;
	private static final float TAB_SIZE = 44;
	private static final float TAB_GAP = 11;
	private static final float GRID_TOP = 188;
	private static final int COLUMNS = 4;
	private static final float CARD_H = 138;
	private static final float GAP_X = 21;
	private static final float GAP_Y = 22;
	private static final float CARD_R = 18;
	private static final float PAD = 22;
	private static final float TILE = 50;
	private static final int REVEAL_STAGGER = 34;
	private static final int REVEAL_TIME = 760;

	public static final List<Tab> TABS = tabs();

	private final Host host;
	private final Hits hits = new Hits();
	private final SettingsSheet sheet;
	private final long openedAt = System.currentTimeMillis();
	private final Map<Module, CardState> cards = new HashMap<>();
	private final Anim[] tabHover = new Anim[TABS.size()];
	private final Anim[] tabX = new Anim[TABS.size()];
	private final Anim pillX = new Anim(0, Tokens.T_INDICATOR, Ease.OUT);
	private final Anim pillW = new Anim(0, Tokens.T_INDICATOR, Ease.OUT);
	private final Anim searchHover = new Anim(0, Tokens.T_COLOR, Ease.OUT);
	private final Anim editHover = new Anim(0, Tokens.T_COLOR, Ease.OUT);

	private int tab;
	private long tabSince = System.currentTimeMillis();
	private String search = "";
	private boolean searchFocused = false;
	private double scroll = 0;
	private double shownScroll = 0;
	private double maxScroll = 0;
	private long lastFrame = 0;
	private long frames = 0;
	private boolean laidOut = false;
	private String newProfile = "";
	private boolean typingProfile = false;
	private String notice = null;
	private long noticeAt = 0;

	/** The scale from layout units to GUI units, and the layout canvas, worked out each frame. */
	private float unit = 1;
	private float canvasW = DESIGN_W;
	private float canvasH = DESIGN_H;

	public MenuView(Host host, Category start) {
		this.host = host;
		this.sheet = new SettingsSheet(host.extras(), host::clickSound);
		for (int i = 0; i < TABS.size(); i++) {
			tabHover[i] = new Anim(0, Tokens.T_COLOR, Ease.OUT);
			tabX[i] = new Anim(0, Tokens.T_INDICATOR, Ease.OUT);
		}
		tab = indexOf(start);
	}

	private static List<Tab> tabs() {
		List<Tab> list = new ArrayList<>();
		list.add(new Tab("Profile", "circle-user-round", "Your setups, one click apart", null));
		for (Category category : Category.values()) list.add(new Tab(category.label, category.icon, category.tagline, category));
		return List.copyOf(list);
	}

	/** The tab for a category (the Profile tab for null). */
	public static int indexOf(Category category) {
		for (int i = 0; i < TABS.size(); i++) {
			if (TABS.get(i).category() == category) return i;
		}
		return 1;
	}

	public Category category() {
		return TABS.get(tab).category();
	}

	/** Opens a module's settings straight away. */
	public MenuView openSettings(Module module) {
		tab = indexOf(module.category);
		sheet.open(module);
		return this;
	}

	/** Shows the Profile page. */
	public MenuView openProfiles() {
		tab = 0;
		return this;
	}

	public MenuView search(String text) {
		search = text;
		searchFocused = !text.isEmpty();
		scroll = 0;
		return this;
	}

	public String searchText() {
		return search;
	}

	/** The middle of a module's card as last drawn, in GUI units, or null if it isn't on screen. */
	public float[] cardCenter(Module module) {
		CardState state = cards.get(module);
		if (state == null || state.drawnAt != frames) return null;
		return new float[] {state.cx * unit, state.cy * unit};
	}

	public boolean sheetOpen() {
		return sheet.isOpen();
	}

	public Module sheetModule() {
		return sheet.module();
	}

	/** Draws the menu over a {@code guiW} x {@code guiH} screen; the mouse is in GUI units. */
	public void draw(Draw d, float guiW, float guiH, double mouseX, double mouseY) {
		long now = System.currentTimeMillis();
		frames++;
		long frame = System.nanoTime();
		double dt = lastFrame == 0 ? 0 : Math.min(0.1, (frame - lastFrame) / 1e9);
		lastFrame = frame;
		shownScroll += (scroll - shownScroll) * Math.min(1.0, dt * 14);
		if (Math.abs(scroll - shownScroll) < 0.3) shownScroll = scroll;

		unit = Math.min(guiW / DESIGN_W, guiH / DESIGN_H);
		canvasW = guiW / unit;
		canvasH = guiH / unit;
		float mx = (float) (mouseX / unit);
		float my = (float) (mouseY / unit);
		boolean sheetBlocks = sheet.isOpen();
		float hx = sheetBlocks ? -1 : mx;
		float hy = sheetBlocks ? -1 : my;
		hits.clear();

		float intro = Ease.OUT.apply((now - openedAt) / 420.0);
		float old = Paint.opacity(intro);
		d.push();
		d.scale(unit);
		Sky.backdrop(d, canvasW, canvasH, host.inWorld(), 1);
		if (Modules.THEME_SNOW.get()) Sky.stars(d, canvasW, canvasH, (float) (Modules.THEME_SNOW_AMOUNT.get() / 100.0), 1, now);

		float x0 = (canvasW - CONTENT_W) / 2f;
		drawHeader(d, x0, hx, hy, now);
		Tab current = TABS.get(tab);
		if (current.category() == null && search.isEmpty()) {
			drawProfiles(d, x0, hx, hy, now);
		} else {
			drawModules(d, x0, hx, hy, now, (float) dt);
		}
		drawFooter(d);
		sheet.draw(d, canvasW, canvasH, mx, my, now);
		drawNotice(d, now);
		d.pop();
		Paint.opacity(old);
	}

	private void drawHeader(Draw d, float x0, float mx, float my, long now) {
		float top = 28;
		Logo.lockup(d, x0 - 27, top + 3, 38, 11.5f, now);

		float total = -TAB_GAP;
		float[] widths = new float[TABS.size()];
		for (int i = 0; i < TABS.size(); i++) {
			widths[i] = i == tab ? pillWidth(TABS.get(i)) : TAB_SIZE;
			total += widths[i] + TAB_GAP;
		}
		float x = canvasW / 2f - total / 2f;
		for (int i = 0; i < TABS.size(); i++) {
			tabX[i].set(x);
			if (i == tab) {
				pillX.set(x);
				pillW.set(widths[i]);
			}
			x += widths[i] + TAB_GAP;
		}
		if (!laidOut) {
			for (int i = 0; i < TABS.size(); i++) tabX[i].snap(tabX[i].target());
			pillX.snap(pillX.target());
			pillW.snap(pillW.target());
			laidOut = true;
		}
		for (int i = 0; i < TABS.size(); i++) {
			if (i == tab) continue;
			float bx = tabX[i].get();
			boolean hover = within(mx, my, bx, top, bx + TAB_SIZE, top + TAB_SIZE);
			tabHover[i].set(hover);
			Tab each = TABS.get(i);
			if (each.category() == null) {
				Paint.circle(d, bx + TAB_SIZE / 2f, top + TAB_SIZE / 2f, TAB_SIZE / 2f, Widgets.mixColor(Tokens.CONTROL, Tokens.CONTROL_HOVER, tabHover[i].get()));
				Paint.ring(d, bx + TAB_SIZE / 2f, top + TAB_SIZE / 2f, TAB_SIZE / 2f, 1, Tokens.CONTROL_EDGE);
				face(d, bx + TAB_SIZE / 2f, top + TAB_SIZE / 2f, TAB_SIZE * 0.46f);
			} else {
				Widgets.roundButton(d, bx + TAB_SIZE / 2f, top + TAB_SIZE / 2f, TAB_SIZE / 2f, each.icon(), tabHover[i].get(), 0.84f);
			}
			int index = i;
			hits.add(bx, top, bx + TAB_SIZE, top + TAB_SIZE, () -> switchTab(index));
		}
		float px = pillX.get();
		float pw = pillW.get();
		Widgets.accentPill(d, px, top, px + pw, top + TAB_SIZE, 1);
		Tab active = TABS.get(tab);
		float iconSize = 18;
		float ix = px + 18;
		if (active.category() == null) face(d, ix + iconSize / 2f, top + TAB_SIZE / 2f, iconSize * 1.05f);
		else Paint.icon(d, active.icon(), ix, top + (TAB_SIZE - iconSize) / 2f, iconSize, 0xFFFFFFFF);
		Type.draw(d, active.name(), ix + iconSize + 10, top + (TAB_SIZE - 12f) / 2f, 12f, Weight.SEMIBOLD, 0xFFFFFFFF);

		float sx2 = x0 + CONTENT_W;
		float sx1 = sx2 - 152;
		boolean searchHovered = within(mx, my, sx1, top, sx2, top + TAB_SIZE);
		searchHover.set(searchHovered);
		Widgets.search(d, sx1, top, sx2, top + TAB_SIZE, search, "Search", "/", searchFocused, searchHover.get(), now);
		hits.add(sx1, top, sx2, top + TAB_SIZE, () -> searchFocused = true);
	}

	private float pillWidth(Tab tab) {
		return 18 + 18 + 10 + Type.width(tab.name(), 12f, Weight.SEMIBOLD) + 22;
	}

	private void face(Draw d, float cx, float cy, float size) {
		host.drawFace(d, cx - size / 2f, cy - size / 2f, size, size / 2f);
	}

	private List<Module> shownModules() {
		if (!search.isEmpty()) {
			String query = search.trim().toLowerCase(Locale.ROOT);
			List<Module> found = new ArrayList<>();
			for (Module module : Modules.ALL) {
				if (module.name.toLowerCase(Locale.ROOT).contains(query) || module.description.toLowerCase(Locale.ROOT).contains(query)) {
					found.add(module);
				}
			}
			return found;
		}
		Category category = category();
		return category == null ? List.of() : Modules.in(category);
	}

	private void drawTitle(Draw d, float x0, String title, String subtitle, long now) {
		float t = Ease.OUT.apply((now - tabSince) / (double) Tokens.T_CAPTION);
		float old = Paint.opacity(Paint.opacity() * t);
		float rise = (1 - t) * 10;
		Type.draw(d, title, x0, 112 + rise, 26, Weight.BOLD, Tokens.TEXT);
		Type.draw(d, subtitle, x0, 148 + rise, 12.5f, Weight.MEDIUM, Tokens.TEXT_2);
		Paint.opacity(old);
	}

	private void drawModules(Draw d, float x0, float mx, float my, long now, float dt) {
		List<Module> shown = shownModules();
		boolean searching = !search.isEmpty();
		String title = searching ? "Search" : TABS.get(tab).name();
		String subtitle = searching ? (shown.isEmpty() ? "Nothing matches \"" + search.trim() + "\"" : shown.size() + (shown.size() == 1 ? " result" : " results")
				+ " for \"" + search.trim() + "\"") : TABS.get(tab).tagline();
		drawTitle(d, x0, title, subtitle, now);

		int toggleable = 0;
		int on = 0;
		for (Module module : shown) {
			if (!module.toggleable()) continue;
			toggleable++;
			if (module.enabled()) on++;
		}
		float right = x0 + CONTENT_W;
		if (toggleable > 0) {
			String count = on + " of " + toggleable + " active";
			float cap = 11.5f;
			float tw = Type.width(count, cap, Weight.MEDIUM);
			Type.draw(d, count, right - tw, 125, cap, Weight.MEDIUM, Tokens.TEXT_2);
			Paint.glow(d, right - tw - 12, 125 + cap / 2f, 3.5f, 8, Paint.fade(Tokens.MAGENTA, 0.6f));
			Paint.circle(d, right - tw - 12, 125 + cap / 2f, 3.5f, Tokens.MAGENTA);
			right -= tw + 30;
		}
		if (category() == Category.HUD && !searching) {
			String label = "Edit layout";
			float cap = 11f;
			float w = 16 + 14 + 8 + Type.width(label, cap, Weight.SEMIBOLD) + 16;
			float bx = right - w;
			float by = 115;
			boolean hover = within(mx, my, bx, by, bx + w, by + 32);
			editHover.set(hover);
			float e = editHover.get();
			Paint.rect(d, bx, by, bx + w, by + 32, 16, Widgets.mixColor(0x0BFFFFFF, 0x14FFFFFF, e));
			Paint.stroke(d, bx, by, bx + w, by + 32, 16, 1, Widgets.mixColor(0x1AFFFFFF, 0x4DCDB6FF, e));
			Paint.icon(d, "move", bx + 16, by + 9, 14, Widgets.mixColor(Tokens.LILAC, 0xFFFFFFFF, e));
			Type.draw(d, label, bx + 38, by + (32 - cap) / 2f, cap, Weight.SEMIBOLD, Tokens.TEXT);
			hits.add(bx, by, bx + w, by + 32, host::openHudEditor);
		}

		float cardW = (CONTENT_W - (COLUMNS - 1) * GAP_X) / COLUMNS;
		float viewTop = GRID_TOP - 14;
		float viewBottom = canvasH - 66;
		int rows = (shown.size() + COLUMNS - 1) / COLUMNS;
		float contentH = rows * (CARD_H + GAP_Y) - GAP_Y + 14;
		maxScroll = Math.max(0, contentH - (viewBottom - GRID_TOP));
		scroll = Math.max(0, Math.min(scroll, maxScroll));
		shownScroll = Math.max(0, Math.min(shownScroll, maxScroll));

		d.enableScissor(0, Math.round(viewTop), Math.round(canvasW), Math.round(viewBottom));
		hits.clip(0, viewTop, canvasW, viewBottom);
		boolean mouseInGrid = my >= viewTop && my < viewBottom;
		for (int i = 0; i < shown.size(); i++) {
			Module module = shown.get(i);
			float x = x0 + (i % COLUMNS) * (cardW + GAP_X);
			float y = GRID_TOP + (i / COLUMNS) * (CARD_H + GAP_Y) - (float) shownScroll;
			if (y > viewBottom + 10 || y + CARD_H < viewTop - 10) continue;
			float reveal = Ease.OUT.apply((now - tabSince - i * REVEAL_STAGGER) / (double) REVEAL_TIME);
			drawCard(d, module, x, y, cardW, reveal, mouseInGrid ? mx : -1, mouseInGrid ? my : -1, now);
		}
		hits.unclip();
		d.disableScissor();
		if (maxScroll > 0) {
			float barH = viewBottom - GRID_TOP;
			float thumb = Math.max(40, barH * barH / (barH + (float) maxScroll));
			float ty = GRID_TOP + (barH - thumb) * (float) (shownScroll / maxScroll);
			float bx = x0 + CONTENT_W + 14;
			Paint.rect(d, bx, GRID_TOP, bx + 3, GRID_TOP + barH, 1.5f, 0x0FFFFFFF);
			Paint.rect(d, bx, ty, bx + 3, ty + thumb, 1.5f, 0x40CDB6FF);
		}
		if (shown.isEmpty()) {
			String hint = searching ? "Try another word, or Esc to clear the search" : "Nothing here yet";
			Type.center(d, hint, canvasW / 2f, GRID_TOP + 60, 12, Weight.MEDIUM, Tokens.TEXT_3);
		}
	}

	private CardState state(Module module) {
		return cards.computeIfAbsent(module, m -> new CardState(m.enabled()));
	}

	private void drawCard(Draw d, Module module, float x, float y, float w, float reveal, float mx, float my, long now) {
		if (reveal <= 0) return;
		CardState state = state(module);
		state.drawnAt = frames;
		state.cx = x + w / 2f;
		state.cy = y + CARD_H / 2f;
		boolean on = module.enabled();
		state.knob.set(on);
		state.lit.set(on);
		boolean hover = within(mx, my, x, y, x + w, y + CARD_H);
		state.hover.set(hover);
		float h = state.hover.get();
		float old = Paint.opacity(Paint.opacity() * reveal);
		float lift = -Tokens.HOVER_LIFT * h + (1 - reveal) * Tokens.REVEAL_RISE;
		float top = y + lift;

		Widgets.card(d, x, top, x + w, top + CARD_H, CARD_R, h);
		float lit = module.toggleable() ? state.lit.get() : 1;
		Widgets.iconTile(d, x + PAD, top + PAD, TILE, module.icon(), lit, h);
		drawRipples(d, module, x, top, w, now);

		boolean hasSettings = module == Modules.PROFILES || SettingsSheet.hasPage(module, sheet.extras());
		float toggleX = x + w - PAD - 50;
		Runnable openSettings = module == Modules.PROFILES ? () -> switchTab(0) : () -> sheet.open(module);
		if (module.toggleable()) {
			Widgets.toggle(d, toggleX, top + 27, 50, 28, state.knob.get(), state.lit.get());
		}
		if (hasSettings) {
			float scx = module.toggleable() ? toggleX - 12 - 16 : x + w - PAD - 16;
			float scy = top + 41;
			boolean settingsHover = within(mx, my, scx - 16, scy - 16, scx + 16, scy + 16);
			state.settings.set(settingsHover);
			float sh = state.settings.get();
			Paint.circle(d, scx, scy, 16, Widgets.mixColor(0x0AFFFFFF, 0x1AFFFFFF, Math.max(sh, h * 0.4f)));
			Paint.icon(d, "sliders-horizontal", scx - 7.5f, scy - 7.5f, 15, Widgets.mixColor(Tokens.TEXT_3, Tokens.TEXT, sh));
		}

		float textRight = x + w - PAD;
		String key = host.keyFor(module);
		if (key != null) {
			float bw = Widgets.keyBadge(d, x + w - PAD, top + 95, 22, key);
			textRight -= bw + 10;
		}
		Type.draw(d, Type.fit(module.name, w - 2 * PAD, 14, Weight.SEMIBOLD), x + PAD, top + 86, 14, Weight.SEMIBOLD, Tokens.TEXT);
		Type.draw(d, Type.fit(module.description, textRight - x - PAD, 10.6f, Weight.MEDIUM), x + PAD, top + 110.5f, 10.6f, Weight.MEDIUM,
				Tokens.TEXT_2);
		Paint.opacity(old);

		if (reveal < 0.5f) return;
		Runnable primary = module.toggleable() ? () -> toggle(module) : openSettings;
		hits.add(x, top, x + w, top + CARD_H, primary, hasSettings ? openSettings : null);
		if (module.toggleable()) hits.add(toggleX - 4, top + 23, toggleX + 54, top + 59, () -> toggle(module), hasSettings ? openSettings : null);
		if (hasSettings) {
			float scx = module.toggleable() ? toggleX - 12 - 16 : x + w - PAD - 16;
			hits.add(scx - 17, top + 24, scx + 17, top + 58, openSettings, openSettings);
		}
	}

	private Module rippleOwner = null;
	private float rippleX;
	private float rippleY;
	private long rippleAt;

	private void toggle(Module module) {
		module.toggle();
		rippleOwner = module;
		rippleX = lastMouseX;
		rippleY = lastMouseY;
		rippleAt = System.currentTimeMillis();
	}

	private void drawRipples(Draw d, Module module, float x, float top, float w, long now) {
		if (module != rippleOwner) return;
		float t = (now - rippleAt) / (float) Tokens.T_RIPPLE;
		if (t >= 1) return;
		float e = Ease.OUT.apply(t);
		d.enableScissor(Math.round(x), Math.round(top), Math.round(x + w), Math.round(top + CARD_H));
		Paint.glow(d, rippleX, rippleY, 4 + e * 240, 60 + e * 80, Paint.fade(0x59CDB6FF, 1 - t));
		d.disableScissor();
	}

	private void drawProfiles(Draw d, float x0, float mx, float my, long now) {
		drawTitle(d, x0, "Profile", TABS.get(0).tagline(), now);
		Config config = Config.get();
		float reveal = Ease.OUT.apply((now - tabSince) / (double) REVEAL_TIME);
		float old = Paint.opacity(Paint.opacity() * reveal);
		float rise = (1 - reveal) * Tokens.REVEAL_RISE;

		float cardW = (CONTENT_W - (COLUMNS - 1) * GAP_X) / COLUMNS;
		float y = GRID_TOP + rise;
		float youW = cardW;
		float youH = 2 * CARD_H + GAP_Y;
		Widgets.card(d, x0, y, x0 + youW, y + youH, CARD_R, 0);
		float face = 96;
		float fx = x0 + (youW - face) / 2f;
		float fy = y + 40;
		Paint.glow(d, fx + face / 2f, fy + face / 2f, face * 0.4f, 60, Paint.fade(Tokens.PURPLE, 0.45f));
		Paint.rect(d, fx - 6, fy - 6, fx + face + 6, fy + face + 6, 22, 0x14FFFFFF);
		host.drawFace(d, fx, fy, face, 18);
		Type.center(d, Type.fit(host.playerName(), youW - 40, 16, Weight.BOLD), x0 + youW / 2f, fy + face + 26, 16, Weight.BOLD, Tokens.TEXT);
		String using = "Using " + config.profile;
		Type.center(d, Type.fit(using, youW - 40, 11, Weight.MEDIUM), x0 + youW / 2f, fy + face + 52, 11, Weight.MEDIUM, Tokens.TEXT_2);
		float ex = x0 + 24;
		float ew = youW - 48;
		float ey = y + youH - 24 - 40;
		boolean editHovered = within(mx, my, ex, ey, ex + ew, ey + 40);
		editHover.set(editHovered);
		Paint.rect(d, ex, ey, ex + ew, ey + 40, 20, Widgets.mixColor(0x0BFFFFFF, 0x17FFFFFF, editHover.get()));
		Paint.stroke(d, ex, ey, ex + ew, ey + 40, 20, 1, Widgets.mixColor(0x1AFFFFFF, 0x4DCDB6FF, editHover.get()));
		float lw = 14 + 8 + Type.width("Edit HUD layout", 11.5f, Weight.SEMIBOLD);
		Paint.icon(d, "move", ex + (ew - lw) / 2f, ey + 13, 14, Tokens.LILAC);
		Type.draw(d, "Edit HUD layout", ex + (ew - lw) / 2f + 22, ey + (40 - 11.5f) / 2f, 11.5f, Weight.SEMIBOLD, Tokens.TEXT);
		hits.add(ex, ey, ex + ew, ey + 40, host::openHudEditor);

		List<String> names = config.profileNames();
		int slot = 1;
		for (String name : names) {
			float cx = x0 + (slot % COLUMNS) * (cardW + GAP_X);
			float cy = GRID_TOP + (slot / COLUMNS) * (CARD_H + GAP_Y) + rise;
			drawProfileCard(d, config, name, cx, cy, cardW, mx, my);
			slot++;
			if (slot % COLUMNS == 0) slot++;
		}
		float cx = x0 + (slot % COLUMNS) * (cardW + GAP_X);
		float cy = GRID_TOP + (slot / COLUMNS) * (CARD_H + GAP_Y) + rise;
		drawNewProfile(d, cx, cy, cardW, mx, my, now);
		Paint.opacity(old);
	}

	private void drawProfileCard(Draw d, Config config, String name, float x, float y, float w, float mx, float my) {
		boolean active = name.equals(config.profile);
		boolean hover = within(mx, my, x, y, x + w, y + CARD_H);
		Widgets.card(d, x, y, x + w, y + CARD_H, CARD_R, hover ? 1 : 0);
		Widgets.iconTile(d, x + PAD, y + PAD, TILE, "layers", active ? 1 : 0, hover ? 1 : 0);
		Type.draw(d, Type.fit(name, w - 2 * PAD, 14, Weight.SEMIBOLD), x + PAD, y + 86, 14, Weight.SEMIBOLD, Tokens.TEXT);
		Type.draw(d, active ? "In use now" : "Click to switch to it", x + PAD, y + 110.5f, 10.6f, Weight.MEDIUM, Tokens.TEXT_2);
		if (active) {
			float pw = 16 + Type.width("Active", 10, Weight.SEMIBOLD) + 14;
			float px = x + w - PAD - pw;
			Paint.rect(d, px, y + 28, px + pw, y + 54, 13, 0x29880FF9);
			Paint.stroke(d, px, y + 28, px + pw, y + 54, 13, 1, 0x40CDB6FF);
			Paint.circle(d, px + 11, y + 41, 3, Tokens.MAGENTA);
			Type.draw(d, "Active", px + 19, y + 41 - 5, 10, Weight.SEMIBOLD, Tokens.LILAC);
			return;
		}
		hits.add(x, y, x + w, y + CARD_H, () -> {
			config.loadProfile(name);
			notice("Switched to " + name);
		});
		if (!name.equals(Config.DEFAULT_PROFILE)) {
			float bx = x + w - PAD - 16;
			float by = y + 41;
			boolean delHover = within(mx, my, bx - 16, by - 16, bx + 16, by + 16);
			Paint.circle(d, bx, by, 16, delHover ? 0x33FF5C84 : 0x0AFFFFFF);
			Paint.icon(d, "trash-2", bx - 7.5f, by - 7.5f, 15, delHover ? Tokens.LOW : Tokens.TEXT_3);
			hits.add(bx - 17, by - 17, bx + 17, by + 17, () -> {
				if (config.deleteProfile(name)) notice("Deleted " + name);
			});
		}
	}

	private void drawNewProfile(Draw d, float x, float y, float w, float mx, float my, long now) {
		Paint.rect(d, x, y, x + w, y + CARD_H, CARD_R, 0x08FFFFFF);
		Paint.stroke(d, x, y, x + w, y + CARD_H, CARD_R, 1, 0x1FCDB6FF);
		Type.draw(d, "New profile", x + PAD, y + PAD + 2, 14, Weight.SEMIBOLD, Tokens.TEXT);
		Type.draw(d, "A copy of your setup, under a new name", x + PAD, y + PAD + 24, 10.6f, Weight.MEDIUM, Tokens.TEXT_2);
		float fy = y + CARD_H - PAD - 36;
		float bw = 86;
		float fx2 = x + w - PAD - bw - 8;
		Widgets.field(d, x + PAD, fy, fx2, fy + 36, "pencil", newProfile, "Name it", null, typingProfile, 0, now);
		hits.add(x + PAD, fy, fx2, fy + 36, () -> typingProfile = true);
		float bx = x + w - PAD - bw;
		Widgets.accentPill(d, bx, fy, bx + bw, fy + 36, 0.8f);
		Type.center(d, "Create", bx + bw / 2f, fy + (36 - 11.5f) / 2f, 11.5f, Weight.SEMIBOLD, 0xFFFFFFFF);
		hits.add(bx, fy, bx + bw, fy + 36, this::createProfile);
	}

	private void createProfile() {
		if (newProfile.isBlank()) {
			notice("Type a name first");
			typingProfile = true;
			return;
		}
		if (Config.get().createProfile(newProfile)) {
			notice("Created " + newProfile.trim());
			newProfile = "";
			typingProfile = false;
		} else {
			notice("That name is taken");
		}
	}

	private void drawFooter(Draw d) {
		String hint = TABS.get(tab).category() == null && search.isEmpty() ? "Click a profile to switch to it  ·  Esc for the orbit"
				: "Click to toggle  ·  Right-click for settings  ·  Esc for the orbit";
		Type.center(d, hint, canvasW / 2f, canvasH - 34, 10.5f, Weight.MEDIUM, Paint.fade(Tokens.TEXT_3, 0.85f));
	}

	/** A short message in a glass pill at the bottom for a moment, like "Added Steve". */
	public void notice(String text) {
		notice = text;
		noticeAt = System.currentTimeMillis();
	}

	private void drawNotice(Draw d, long now) {
		if (notice == null) return;
		long age = now - noticeAt;
		if (age > 2600) return;
		float in = Ease.OUT.apply(age / 400.0);
		float out = age > 2200 ? 1 - (age - 2200) / 400f : 1;
		float a = Math.min(in, out);
		float cap = 11.5f;
		float w = Type.width(notice, cap, Weight.SEMIBOLD) + 18 + 16 + 22;
		float x = (canvasW - w) / 2f;
		float y = canvasH - 92 + (1 - in) * 16;
		float old = Paint.opacity(Paint.opacity() * a);
		Widgets.glass(d, x, y, x + w, y + 40, 20);
		Paint.icon(d, "info", x + 18, y + 12, 16, Tokens.LILAC);
		Type.draw(d, notice, x + 18 + 16 + 10, y + (40 - cap) / 2f, cap, Weight.SEMIBOLD, Tokens.TEXT);
		Paint.opacity(old);
	}

	private static boolean within(float x, float y, float x1, float y1, float x2, float y2) {
		return x >= x1 && x < x2 && y >= y1 && y < y2;
	}

	private float lastMouseX;
	private float lastMouseY;

	private void switchTab(int index) {
		if (index == tab && search.isEmpty()) return;
		tab = index;
		tabSince = System.currentTimeMillis();
		scroll = 0;
		shownScroll = 0;
		search = "";
		searchFocused = false;
		typingProfile = false;
	}

	/** A click at (x, y) in GUI units; returns whether the menu used it. */
	public boolean mouseClicked(double x, double y, int button) {
		float mx = (float) (x / unit);
		float my = (float) (y / unit);
		lastMouseX = mx;
		lastMouseY = my;
		if (sheet.isOpen()) {
			if (sheet.mouseClicked(mx, my, button)) return true;
			sheet.close();
			return true;
		}
		Runnable action = hits.at(mx, my, button);
		searchFocused = false;
		typingProfile = false;
		if (action == null) return false;
		host.clickSound();
		action.run();
		return true;
	}

	public boolean mouseDragged(double x, double y) {
		return sheet.isOpen() && sheet.mouseDragged((float) (x / unit), (float) (y / unit));
	}

	public boolean mouseReleased() {
		return sheet.isOpen() && sheet.mouseReleased();
	}

	public boolean mouseScrolled(double x, double y, double amount) {
		if (sheet.isOpen()) return sheet.mouseScrolled((float) (x / unit), (float) (y / unit), amount);
		scroll = Math.max(0, Math.min(maxScroll, scroll - amount * 70));
		return true;
	}

	/** A key press; returns whether the menu used it. */
	public boolean keyPressed(int key, boolean ctrl) {
		if (sheet.isOpen()) {
			if (sheet.keyPressed(key)) return true;
			if (key == Keys.ESCAPE) {
				sheet.close();
				return true;
			}
			return false;
		}
		if (typingProfile) {
			if (key == Keys.BACKSPACE) {
				if (!newProfile.isEmpty()) newProfile = newProfile.substring(0, newProfile.length() - 1);
				return true;
			}
			if (Keys.enter(key)) {
				createProfile();
				return true;
			}
			if (key == Keys.ESCAPE) {
				typingProfile = false;
				return true;
			}
			return true;
		}
		if (key == Keys.ESCAPE) {
			if (!search.isEmpty() || searchFocused) {
				search = "";
				searchFocused = false;
				tabSince = System.currentTimeMillis();
				return true;
			}
			host.back();
			return true;
		}
		if (searchFocused) {
			if (key == Keys.BACKSPACE) {
				if (!search.isEmpty()) {
					search = ctrl ? "" : search.substring(0, search.length() - 1);
					scroll = 0;
				}
				return true;
			}
			if (Keys.enter(key)) {
				searchFocused = false;
				return true;
			}
			return key != Keys.LEFT && key != Keys.RIGHT;
		}
		if (key == Keys.LEFT || key == Keys.RIGHT) {
			switchTab(Math.floorMod(tab + (key == Keys.RIGHT ? 1 : -1), TABS.size()));
			return true;
		}
		return false;
	}

	/** A typed character; returns whether the menu used it. */
	public boolean charTyped(String typed) {
		if (typed.isEmpty() || typed.codePointAt(0) < 32) return false;
		if (sheet.isOpen()) return sheet.charTyped(typed);
		if (typingProfile) {
			if (newProfile.length() < 24) newProfile += typed;
			return true;
		}
		if (!searchFocused) {
			if (typed.equals("/")) {
				searchFocused = true;
				return true;
			}
			if (!Character.isLetterOrDigit(typed.codePointAt(0))) return false;
			searchFocused = true;
		}
		if (search.length() < 32) {
			search += typed;
			scroll = 0;
			tabSince = System.currentTimeMillis();
		}
		return true;
	}

	/** Whether a text field has the keyboard, so keys like the menu key shouldn't close it. */
	public boolean typing() {
		return searchFocused || typingProfile || sheet.typing();
	}

	/** Per-card animation state, made once per module. */
	private static final class CardState {
		final Anim hover = new Anim(0, Tokens.T_HOVER, Ease.OUT);
		final Anim settings = new Anim(0, Tokens.T_COLOR, Ease.OUT);
		final Anim knob;
		final Anim lit;
		long drawnAt = -1;
		float cx;
		float cy;

		CardState(boolean on) {
			knob = new Anim(on ? 1 : 0, Tokens.T_SWITCH_KNOB, Ease.SPRING);
			lit = new Anim(on ? 1 : 0, Tokens.T_SWITCH_FILL, Ease.OUT);
		}
	}
}
