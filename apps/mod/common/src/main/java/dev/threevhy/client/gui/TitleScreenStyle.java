package dev.threevhy.client.gui;

import dev.threevhy.client.Draw;
import dev.threevhy.client.Hooks;
import dev.threevhy.client.ThreevhyClient;
import dev.threevhy.client.compat.Compat;
import dev.threevhy.client.hud.Theme;
import dev.threevhy.client.mixin.TitleScreenAccessor;
import dev.threevhy.client.module.Modules;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.Util;

/**
 * The main menu laid out like Lunar's, over Minecraft's own title screen: Minecraft's logo, panorama
 * and stone buttons, a Mods button with 3vhy's gem under Minecraft Realms, a column of dark buttons on
 * the right (Profiles, Edit HUD, Pictures for your screenshots, Settings), and your account in a pill
 * at the top left. The title screen itself stays, so buttons other mods add still show up. In October
 * the panorama makes way for {@link October}'s ghosts.
 */
public final class TitleScreenStyle {
	public static final String MODS_LABEL = "Mods";
	private static final int MARGIN = 6;
	/** Minecraft's title screen rows are 24 apart, with 12 more above the Options row. */
	private static final int ROW_GAP = 4;
	private static final int OPTIONS_GAP = 12;
	/** The version and copyright text along the bottom. */
	private static final int FOOTER = 12;
	private static final int SIDE_W = 80;
	private static final int SIDE_H = 20;
	private static final Set<AbstractWidget> SIDE = Collections.newSetFromMap(new WeakHashMap<>());
	private static final Map<AbstractWidget, String[]> SIDE_ICONS = new WeakHashMap<>();
	private static final Set<AbstractWidget> GEMS = Collections.newSetFromMap(new WeakHashMap<>());

	private static final String[] PROFILE_ICON = {
			"...##...",
			"..####..",
			"..####..",
			"...##...",
			".######.",
			"########",
			"########",
	};
	private static final String[] HUD_ICON = {
			"###.####",
			"#.#.#..#",
			"###.####",
			"........",
			"####.###",
			"#..#.#.#",
			"####.###",
	};
	private static final String[] PICTURE_ICON = {
			"########",
			"#......#",
			"#.##...#",
			"#....#.#",
			"#...####",
			"#.######",
			"########",
	};
	private static final String[] SETTINGS_ICON = {
			".##.....",
			"########",
			".##.....",
			"........",
			".....##.",
			"########",
			".....##.",
	};

	private record Side(String label, String[] icon, Runnable action) {
	}

	private TitleScreenStyle() {
	}

	public static boolean enabled() {
		return Modules.MAIN_MENU.enabled();
	}

	/** Whether the main menu shows October's backdrop instead of Minecraft's panorama. */
	public static boolean drawsBackground() {
		return enabled() && October.active();
	}

	private static boolean onTitleScreen() {
		return enabled() && Minecraft.getInstance().screen instanceof TitleScreen;
	}

	/** Called once the title screen has made its buttons (and other mods have added theirs). */
	public static void prepare(Screen screen) {
		if (!enabled() || !(screen instanceof TitleScreen)) return;
		TitleScreenAccessor access = (TitleScreenAccessor) screen;
		access.threevhy$setSplash(null);
		if (access.threevhy$realmsNotifications() != null) {
			access.threevhy$realmsNotifications().removed();
			access.threevhy$setRealmsNotifications(null);
		}
		List<AbstractWidget> widgets = new ArrayList<>(Compat.widgets(screen));
		Minecraft mc = Minecraft.getInstance();
		String label = taken(widgets, MODS_LABEL) ? "3vhy " + MODS_LABEL : MODS_LABEL;
		Button mods = Button.builder(Component.literal(label), button -> mc.setScreen(ModMenuScreen.create(screen))).build();
		GEMS.add(mods);
		int[] column = layOut(screen, widgets, mods);
		if (column == null) {
			mods.setX(MARGIN);
			mods.setY(freeSpot(widgets, MARGIN, 80, 20));
			mods.setWidth(80);
		}
		Compat.addWidget(screen, mods);
		addSideButtons(screen, mc, column);
		Compat.afterScreen(screen, d -> drawOverlay(d, screen));
		ScreenKeyboardEvents.afterKeyPress(screen).register((shown, event) -> {
			if (ThreevhyClient.openSettingsKey != null && ThreevhyClient.openSettingsKey.matches(event) && mc.screen == shown) {
				mc.setScreen(ModMenuScreen.create(shown));
			}
		});
		Hooks.mainMenuShown = true;
	}

	/** The title screen's widget whose label is this translation key, or null. */
	private static AbstractWidget find(List<AbstractWidget> widgets, String key) {
		for (AbstractWidget widget : widgets) {
			if (widget.visible && widget.getMessage().getContents() instanceof TranslatableContents contents
					&& contents.getKey().equals(key)) {
				return widget;
			}
		}
		return null;
	}

	/** Whether another mod already has a button with this label (Mod Menu's Mods, say). */
	private static boolean taken(List<AbstractWidget> widgets, String label) {
		for (AbstractWidget widget : widgets) {
			if (widget.visible && widget.getMessage().getString().equals(label)) return true;
		}
		return false;
	}

	/**
	 * Puts the Mods row under Minecraft Realms and stacks the centre column like Lunar's: rows 24
	 * apart from a quarter of the way down (less 12), then the Options / Quit row 12 lower, moved up
	 * if it would reach the copyright line. Only widgets in the centre column move. Returns the
	 * column's {top, bottom}, or null if the title screen doesn't look like Minecraft's.
	 */
	private static int[] layOut(Screen screen, List<AbstractWidget> widgets, Button mods) {
		AbstractWidget first = find(widgets, "menu.singleplayer");
		AbstractWidget options = find(widgets, "menu.options");
		if (first == null || options == null || options.getY() <= first.getY()) return null;
		int center = screen.width / 2;
		Map<Integer, List<AbstractWidget>> byY = new TreeMap<>();
		List<AbstractWidget> bottom = new ArrayList<>();
		for (AbstractWidget widget : widgets) {
			if (!widget.visible || widget.getX() < center - 130 || widget.getX() + widget.getWidth() > center + 130) continue;
			if (Math.abs(widget.getY() - options.getY()) <= 2) bottom.add(widget);
			else if (widget.getY() >= first.getY() && widget.getY() < options.getY()) byY.computeIfAbsent(widget.getY(), y -> new ArrayList<>()).add(widget);
		}
		List<List<AbstractWidget>> rows = new ArrayList<>(byY.values());
		AbstractWidget realms = find(widgets, "menu.online");
		int at = rows.size();
		for (int i = 0; i < rows.size(); i++) {
			if (rows.get(i).contains(realms)) at = i + 1;
		}
		mods.setX(first.getX());
		mods.setWidth(first.getWidth());
		mods.setHeight(first.getHeight());
		rows.add(at, List.<AbstractWidget>of(mods));
		int columnH = -ROW_GAP;
		for (List<AbstractWidget> row : rows) columnH += rowHeight(row) + ROW_GAP;
		int bottomH = rowHeight(bottom);
		int total = columnH + ROW_GAP + OPTIONS_GAP + bottomH;
		int top = Math.max(MARGIN, Math.min(screen.height / 4 + 36, screen.height - FOOTER - total));
		int y = top;
		for (List<AbstractWidget> row : rows) {
			for (AbstractWidget widget : row) widget.setY(y);
			y += rowHeight(row) + ROW_GAP;
		}
		int bottomY = y + OPTIONS_GAP;
		int optionsY = options.getY();
		for (AbstractWidget widget : bottom) widget.setY(bottomY + widget.getY() - optionsY);
		return new int[] {top, bottomY + bottomH};
	}

	private static int rowHeight(List<AbstractWidget> row) {
		int h = 0;
		for (AbstractWidget widget : row) h = Math.max(h, widget.getHeight());
		return h;
	}

	/** The first height down the left edge where a button of this size doesn't cover another mod's button. */
	private static int freeSpot(List<AbstractWidget> widgets, int x, int w, int h) {
		int y = MARGIN;
		boolean moved = true;
		while (moved && y < 200) {
			moved = false;
			for (AbstractWidget other : widgets) {
				if (!other.visible) continue;
				boolean overlaps = other.getX() < x + w && other.getX() + other.getWidth() > x && other.getY() < y + h
						&& other.getY() + other.getHeight() > y;
				if (overlaps) {
					y = other.getY() + other.getHeight() + 4;
					moved = true;
				}
			}
		}
		return y;
	}

	/**
	 * Lunar's column of dark buttons on the right, centred beside the main buttons, with 3vhy's own
	 * shortcuts. Left out when the window is too narrow for it next to the main column.
	 */
	private static void addSideButtons(Screen screen, Minecraft mc, int[] column) {
		int center = screen.width / 2;
		int x = Math.min(Math.max(center + 112, Math.round(screen.width * 0.768f)), screen.width - MARGIN - SIDE_W);
		if (x < center + 106) return;
		List<Side> sides = List.of(
				new Side("Profiles", PROFILE_ICON, () -> mc.setScreen(new ModMenuScreen(screen).openTab(ModMenuScreen.Tab.PROFILES))),
				new Side("Edit HUD", HUD_ICON, () -> mc.setScreen(new HubScreen(screen).editOnly())),
				new Side("Pictures", PICTURE_ICON, TitleScreenStyle::openScreenshots),
				new Side("Settings", SETTINGS_ICON, () -> mc.setScreen(new ModMenuScreen(screen).openTab(ModMenuScreen.Tab.SETTINGS))));
		int total = sides.size() * (SIDE_H + ROW_GAP) - ROW_GAP;
		int middle = column != null ? (column[0] + column[1]) / 2 : screen.height / 2;
		int y = Math.max(MARGIN, middle - total / 2);
		for (Side side : sides) {
			Button button = Button.builder(Component.literal(side.label()), pressed -> side.action().run())
					.pos(x, y).size(SIDE_W, SIDE_H).build();
			SIDE.add(button);
			SIDE_ICONS.put(button, side.icon());
			Compat.addWidget(screen, button);
			y += SIDE_H + ROW_GAP;
		}
	}

	private static void openScreenshots() {
		Path folder = Minecraft.getInstance().gameDirectory.toPath().resolve("screenshots");
		try {
			Files.createDirectories(folder);
			Util.getPlatform().openPath(folder);
		} catch (IOException e) {
			ThreevhyClient.LOG.warn("Couldn't open the screenshots folder", e);
		}
	}

	/** Lunar's side buttons are drawn by 3vhy; Minecraft's own keep their stone look. */
	public static boolean styled(AbstractWidget widget) {
		return widget.visible && SIDE.contains(widget) && onTitleScreen();
	}

	/** October's backdrop, in place of the panorama. */
	public static void drawBackground(Draw d, int w, int h) {
		October.drawBackdrop(d, w, h);
	}

	/** A side button like Lunar's: near-black with a grey edge that turns white on hover, the label on the left, an icon on the right. */
	public static void drawButton(Draw d, AbstractWidget widget, int mouseX, int mouseY) {
		Hooks.titleButtonsStyled = true;
		int x = widget.getX();
		int y = widget.getY();
		int w = widget.getWidth();
		int h = widget.getHeight();
		boolean hover = widget.active && (widget.isFocused() || mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h);
		d.fill(x, y, x + w, y + h, hover ? 0xF0202226 : 0xE6101113);
		d.outline(x, y, x + w, y + h, hover ? 0xFFFFFFFF : 0xFF5C5F66);
		Minecraft mc = Minecraft.getInstance();
		String label = widget.getMessage().getString();
		d.text(label, x + 6, y + (h - 8) / 2, hover ? 0xFFFFFFFF : 0xFFE0E0E0, true);
		String[] icon = SIDE_ICONS.get(widget);
		if (icon != null) {
			int iw = icon[0].length();
			int ix = x + w - 6 - iw;
			int iy = y + (h - icon.length) / 2;
			for (int row = 0; row < icon.length; row++) {
				for (int col = 0; col < iw; col++) {
					if (icon[row].charAt(col) == '#') d.fill(ix + col, iy + row, ix + col + 1, iy + row + 1, hover ? 0xFFFFFFFF : 0xFFD6D8DC);
				}
			}
		}
	}

	/** The gem on the Mods button and the account pill, over the title screen's widgets. */
	private static void drawOverlay(Draw d, Screen screen) {
		if (!onTitleScreen()) return;
		for (AbstractWidget widget : Compat.widgets(screen)) {
			if (GEMS.contains(widget)) Gem.onButton(d, widget);
		}
		drawAccount(d);
	}

	/**
	 * Lunar's account pill at the top left, in real pixels: the 3vhy app icon, your head and your
	 * name on a dark rounded box 13 GUI pixels tall.
	 */
	private static void drawAccount(Draw d) {
		Minecraft mc = Minecraft.getInstance();
		float guiScale = (float) mc.getWindow().getGuiScale();
		int h = Math.round(13.3f * guiScale);
		int x = Math.round(MARGIN * guiScale);
		int y = Math.round(MARGIN * guiScale);
		int pad = Math.round(h * 0.3f);
		int icon = Math.round(h * 0.5f);
		int face = Math.round(h * 0.4f);
		float textScale = h * 0.28f / 6.8f;
		Component name = UiFont.semibold(mc.getUser().getName());
		int nameW = Math.round(mc.font.width(name) * textScale);
		int gap = Math.round(h * 0.22f);
		int w = pad + icon + gap + face + gap + nameW + Math.round(h * 0.4f);
		d.push();
		d.scale(1f / guiScale);
		Smooth.roundRect(d, x, y, x + w, y + h, h * 0.2f, 0xE616171B);
		int cx = x + pad;
		int iy = y + (h - icon) / 2;
		Smooth.roundRect(d, cx, iy, cx + icon, iy + icon, icon * 0.25f, Theme.ROYAL);
		int cell = Math.max(1, Math.round(icon * 0.6f / 12));
		Wordmark.drawGlyph(d, cx + (icon - 10 * cell) / 2, iy + (icon - 12 * cell) / 2, cell, Theme.ICE);
		cx += icon + gap;
		d.face(MenuNameTag.ownSkin(mc), cx, y + (h - face) / 2, face);
		cx += face + gap;
		d.push();
		d.translate(cx, y + (h - 6.8f * textScale) / 2f);
		d.scale(textScale);
		d.text(name, 0, 0, 0xFFE6E8EC, false);
		d.pop();
		d.pop();
	}
}
