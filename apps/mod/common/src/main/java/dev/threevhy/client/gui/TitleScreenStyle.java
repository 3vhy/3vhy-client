package dev.threevhy.client.gui;

import dev.threevhy.client.Draw;
import dev.threevhy.client.Hooks;
import dev.threevhy.client.ThreevhyClient;
import dev.threevhy.client.compat.Compat;
import dev.threevhy.client.hud.Theme;
import dev.threevhy.client.mixin.TitleScreenAccessor;
import dev.threevhy.client.module.Modules;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

/**
 * The 3vhy main menu, laid over Minecraft's own title screen in Lunar's style: the 3vhy sky with
 * snow, the 3vhy emblem and name where Minecraft's logo was, and the menu buttons as slim glass.
 * The title screen itself stays, so buttons and panels other mods add to it (Mod Menu, Essential
 * and so on) still show up. Only Minecraft's and Mod Menu's own buttons are restyled; anything
 * else keeps its own look. "3vhy Mods" takes the place of Minecraft Realms (or Right Shift) and
 * opens the 3vhy mod menu. The menu's column is spaced out evenly between the top and the
 * copyright line, so it doesn't feel cramped in small windows. In October it wears {@link October}'s
 * white, ghostly look instead.
 */
public final class TitleScreenStyle {
	public static final String MODS_LABEL = "3vhy Mods";
	private static final int MARGIN = 6;
	private static final int ROW_GAP = 6;
	private static final int NAME_H = 12;
	/** The version and copyright text along the bottom. */
	private static final int FOOTER = 14;
	private static final Set<AbstractWidget> STYLED = Collections.newSetFromMap(new WeakHashMap<>());
	private static final Snow SNOW = new Snow();
	private static long openedAt = 0;
	/** Where the emblem goes, worked out with the buttons for a screen this tall. */
	private static int headerTop = 0;
	private static int headerCell = 2;
	private static int headerFor = -1;

	private TitleScreenStyle() {
	}

	public static boolean enabled() {
		return Modules.MAIN_MENU.enabled();
	}

	private static boolean onTitleScreen() {
		return enabled() && Minecraft.getInstance().screen instanceof TitleScreen;
	}

	/** Whether Minecraft's logo (and its splash text) should be left out right now. */
	public static boolean hidesMinecraftLogo() {
		return onTitleScreen();
	}

	/** Called once the title screen has made its buttons (and other mods have added theirs). */
	public static void prepare(Screen screen) {
		if (!enabled() || !(screen instanceof TitleScreen)) return;
		((TitleScreenAccessor) screen).threevhy$setSplash(null);
		List<AbstractWidget> widgets = Compat.widgets(screen);
		for (AbstractWidget widget : widgets) {
			if (restyles(widget)) STYLED.add(widget);
		}
		Minecraft mc = Minecraft.getInstance();
		Button.Builder mods = Button.builder(Component.literal(MODS_LABEL), button -> mc.setScreen(ModMenuScreen.create(screen)));
		AbstractWidget realms = find(widgets, "menu.online");
		Button added;
		if (realms != null) {
			realms.visible = false;
			realms.active = false;
			TitleScreenAccessor access = (TitleScreenAccessor) screen;
			if (access.threevhy$realmsNotifications() != null) {
				access.threevhy$realmsNotifications().removed();
				access.threevhy$setRealmsNotifications(null);
			}
			added = mods.pos(realms.getX(), realms.getY()).size(realms.getWidth(), realms.getHeight()).build();
		} else {
			added = mods.pos(MARGIN, freeSpot(widgets, MARGIN, 80, 20)).size(80, 20).build();
		}
		Compat.addWidget(screen, added);
		STYLED.add(added);
		layOut(screen, new ArrayList<>(widgets));
		ScreenKeyboardEvents.afterKeyPress(screen).register((shown, event) -> {
			if (ThreevhyClient.openSettingsKey != null && ThreevhyClient.openSettingsKey.matches(event) && mc.screen == shown) {
				mc.setScreen(ModMenuScreen.create(shown));
			}
		});
		openedAt = System.currentTimeMillis();
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

	/**
	 * Spreads the menu out: the emblem, the name, the button rows (Singleplayer down to the row
	 * before Options) and the Options / Quit row, centred between the top and the copyright line with
	 * even gaps. Only widgets in that centre column move; other mods' widgets elsewhere stay put.
	 */
	private static void layOut(Screen screen, List<AbstractWidget> widgets) {
		headerFor = -1;
		AbstractWidget first = find(widgets, "menu.singleplayer");
		AbstractWidget options = find(widgets, "menu.options");
		if (first == null || options == null || options.getY() <= first.getY()) return;
		int center = screen.width / 2;
		Map<Integer, List<AbstractWidget>> rows = new TreeMap<>();
		List<AbstractWidget> bottom = new ArrayList<>();
		for (AbstractWidget widget : widgets) {
			if (!widget.visible || widget.getX() < center - 130 || widget.getX() + widget.getWidth() > center + 130) continue;
			if (Math.abs(widget.getY() - options.getY()) <= 2) bottom.add(widget);
			else if (widget.getY() >= first.getY() && widget.getY() < options.getY()) rows.computeIfAbsent(widget.getY(), y -> new ArrayList<>()).add(widget);
		}
		if (rows.isEmpty()) return;
		int columnH = -ROW_GAP;
		for (List<AbstractWidget> row : rows.values()) columnH += rowHeight(row) + ROW_GAP;
		int bottomH = rowHeight(bottom);
		int cell = screen.height >= 300 ? 3 : 2;
		int header = 16 * cell + 6 + NAME_H + 16;
		int total = header + columnH + (bottomH > 0 ? 12 + bottomH : 0);
		int top = Math.max(MARGIN, (screen.height - FOOTER - total) / 2);
		int y = top + header;
		for (List<AbstractWidget> row : rows.values()) {
			for (AbstractWidget widget : row) widget.setY(y);
			y += rowHeight(row) + ROW_GAP;
		}
		int bottomY = y - ROW_GAP + 12;
		int optionsY = options.getY();
		for (AbstractWidget widget : bottom) widget.setY(bottomY + widget.getY() - optionsY);
		headerTop = top;
		headerCell = cell;
		headerFor = screen.height;
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

	/** Plain text buttons from Minecraft and Mod Menu. Icon buttons, links and other mods' own widgets keep their look. */
	private static boolean restyles(AbstractWidget widget) {
		if (!(widget instanceof Button) || widget instanceof SpriteIconButton || widget instanceof PlainTextButton) return false;
		String type = widget.getClass().getName();
		return type.startsWith("net.minecraft.") || type.contains("modmenu");
	}

	public static boolean styled(AbstractWidget widget) {
		return widget.visible && STYLED.contains(widget) && onTitleScreen();
	}

	/** The background: the 3vhy sky and snow, a light dim like Lunar's blur, then the emblem and name. */
	public static void drawBackground(Draw d, int w, int h) {
		boolean october = October.active();
		if (october) {
			October.drawBackdrop(d, w, h);
		} else {
			MainMenuScreen.drawBackdrop(d, w, h);
			SNOW.draw(d, w, h);
			d.fill(0, 0, w, h, 0x26000000);
		}
		float intro = Math.min(1f, (System.currentTimeMillis() - openedAt) / 400f);
		int alpha = Math.round(255 * intro);
		int cell = headerFor == h ? headerCell : h >= 300 ? 3 : 2;
		int size = 16 * cell;
		int spot = headerFor == h ? headerTop : Math.max(4, h / 4 + 48 - 22 - 6 - size);
		int top = spot + Math.round((1 - intro) * -6);
		int nameY = spot + size + 6;
		HubScreen.drawEmblem(d, (w - size) / 2, top, size, cell, alpha, october ? October.INK : 0xFFFFFFFF);
		Minecraft mc = Minecraft.getInstance();
		d.push();
		d.translate(w / 2f, nameY);
		d.scale(1.5f);
		String brand = "3VHY";
		String client = " CLIENT";
		int brandW = mc.font.width(Ui.bold(brand));
		int textW = brandW + mc.font.width(client);
		d.text(Ui.bold(brand), -textW / 2, 0, MenuTheme.alpha(october ? October.INK : 0xFFFFFF, alpha), !october);
		d.text(client, -textW / 2 + brandW, 0, MenuTheme.alpha(october ? October.INK_SOFT : 0xE3F3FC, october ? alpha : alpha * 0xD8 / 255),
				!october);
		d.pop();
		if (october) {
			String tagline = "HAPPY HALLOWEEN";
			d.push();
			d.translate(w / 2f, nameY + 15);
			d.scale(0.75f);
			d.text(tagline, -mc.font.width(tagline) / 2, 0, MenuTheme.alpha(October.PUMPKIN, alpha), false);
			d.pop();
		}
	}

	/** Draws a restyled button in place of Minecraft's: slim smoked glass, an ice-blue edge on hover, uppercase text. */
	public static void drawButton(Draw d, AbstractWidget widget, int mouseX, int mouseY) {
		Hooks.titleButtonsStyled = true;
		int x = widget.getX();
		int y = widget.getY();
		int w = widget.getWidth();
		int h = widget.getHeight();
		boolean active = widget.active;
		boolean hover = active && (widget.isFocused() || mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h);
		boolean ours = widget.getMessage().getString().equals(MODS_LABEL);
		boolean october = October.active();
		if (october) {
			d.roundRect(x, y, x + w, y + h, 4, hover ? 0xFFFFFFFF : 0xF2FFFFFF);
			d.roundOutline(x, y, x + w, y + h, 4, hover ? October.INK : ours ? 0xCCFF8A2B : 0x4D2A2238);
		} else {
			d.roundRect(x, y, x + w, y + h, 4, hover ? 0xB0163056 : ours ? 0x9C0E2448 : 0x8C080C18);
			d.roundOutline(x, y, x + w, y + h, 4, hover ? Theme.CYAN : ours ? 0x995BC8E6 : 0x59FFFFFF);
		}
		Minecraft mc = Minecraft.getInstance();
		String label = widget.getMessage().getString().toUpperCase(Locale.ROOT);
		int max = w - 8;
		if (mc.font.width(label) > max) {
			while (!label.isEmpty() && mc.font.width(label + "...") > max) label = label.substring(0, label.length() - 1);
			label = label + "...";
		}
		int color = october ? (!active ? 0x802A2238 : October.INK) : !active ? 0x80FFFFFF : hover ? 0xFFFFFFFF : 0xFFE3F3FC;
		d.text(label, x + (w - mc.font.width(label)) / 2, y + (h - 8) / 2, color, false);
	}
}
