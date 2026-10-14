package dev.threevhy.client.gui;

import dev.threevhy.client.Draw;
import dev.threevhy.client.Hooks;
import dev.threevhy.client.compat.Compat;
import dev.threevhy.client.module.Modules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.PlayerSkin;

/**
 * The 3vhy Client logo at the bottom right of the main menu, the pause menu, your inventory and
 * chests: the pixel 3vhy wordmark and a spaced-out CLIENT, like the launcher's logo, on a small dark
 * pill. It can also show your head and name (Name Protect and Skin Protect apply to them like
 * everywhere else), or only those. With Show in game on, the HUD shows it too.
 *
 * <p>It's drawn in real screen pixels, each wordmark cell a whole number of them (one on a 1080p
 * screen), so it stays crisp and the same size at any GUI scale. Like 67's logo, the wordmark, CLIENT
 * and your name share one capital height, and the pill is 2.4 of them tall.
 */
public final class MenuNameTag {
	private static final int FILL = 0xE6111216;
	private static final int EDGE = 0x2EFFFFFF;
	private static final int WORD = 0xFFE3F3FC;
	private static final int WORD_SHADOW = 0xFF04104A;
	private static final int CLIENT_COLOR = 0xFF8CC4EA;
	private static final String CLIENT = "CLIENT";
	/** The wordmark's capital height in cells (its "y" hangs below that). */
	private static final int WORD_CAP = 12;
	/** The title screen's copyright line along the bottom (in GUI pixels), which the pill sits above. */
	private static final int COPYRIGHT_LINE = 10;
	/** Your own entry, for your skin when you're not in a world. */
	private static PlayerInfo offline = null;

	/** The pill's sizes in real pixels. */
	private record Layout(int cell, int cap, float textScale, int pad, int h, int letterGap, int clientW, int faceSize,
			int nameW, boolean logo, boolean name, boolean head, Component playerText, int w) {
	}

	private MenuNameTag() {
	}

	/** The menus it shows on; other screens often have buttons in that corner. */
	public static boolean shows(Screen screen) {
		return screen instanceof MainMenuScreen || screen instanceof TitleScreen || screen instanceof PauseScreen
				|| screen instanceof AbstractContainerScreen<?>;
	}

	private static Layout layout(Minecraft mc) {
		int cell = Math.max(1, Math.round(mc.getWindow().getHeight() / 1080f));
		int cap = WORD_CAP * cell;
		float textScale = cap / 6.8f;
		int pad = Math.round(0.9f * cap);
		int h = Math.round(2.4f * cap);
		int letterGap = Math.max(1, Math.round(0.22f * cap));
		boolean logo = !Modules.MENU_NAME_SHOW.is("Name");
		boolean name = !Modules.MENU_NAME_SHOW.is("Logo");
		boolean head = Modules.MENU_NAME_HEAD.get();
		Component playerText = UiFont.semibold(mc.getUser().getName());
		int clientW = -letterGap;
		for (char c : CLIENT.toCharArray()) clientW += Math.round(mc.font.width(UiFont.bold(String.valueOf(c))) * textScale) + letterGap;
		int logoW = Wordmark.COLUMNS * cell + Math.round(0.5f * cap) + clientW;
		int faceSize = Math.round(1.15f * cap / 2) * 2;
		int nameW = Math.round(mc.font.width(playerText) * textScale) + (head ? faceSize + Math.round(0.4f * cap) : 0);
		int w = 2 * pad + (logo ? logoW : 0) + (logo && name ? Math.round(1.2f * cap) : 0) + (name ? nameW : 0);
		return new Layout(cell, cap, textScale, pad, h, letterGap, clientW, faceSize, nameW, logo, name, head, playerText, w);
	}

	/** The pill's {width, height} in real pixels. */
	public static int[] size(Minecraft mc) {
		Layout l = layout(mc);
		return new int[] {l.w(), l.h()};
	}

	public static void draw(Draw d, Screen screen) {
		if (!Modules.MENU_NAME.enabled() || !shows(screen)) return;
		Minecraft mc = Minecraft.getInstance();
		float guiScale = (float) mc.getWindow().getGuiScale();
		Layout l = layout(mc);
		int screenW = Math.round(d.width() * guiScale);
		int screenH = Math.round(d.height() * guiScale);
		int margin = Math.round(0.4f * l.cap());
		int x = screenW - margin - l.w();
		int y = screenH - margin - l.h() - (screen instanceof TitleScreen ? Math.round(COPYRIGHT_LINE * guiScale) : 0);
		if (coversButton(screen, x / guiScale, y / guiScale, l.w() / guiScale, l.h() / guiScale)) {
			y = margin;
			if (coversButton(screen, x / guiScale, y / guiScale, l.w() / guiScale, l.h() / guiScale)) return;
		}
		d.push();
		d.scale(1f / guiScale);
		drawPill(d, mc, l, x, y);
		d.pop();
		Hooks.menuNameDrawn = true;
	}

	/** Draws the pill with its top left at (x, y); {@code d} must be drawing in real pixels. */
	public static void drawPill(Draw d, Minecraft mc, int x, int y) {
		drawPill(d, mc, layout(mc), x, y);
	}

	private static void drawPill(Draw d, Minecraft mc, Layout l, int x, int y) {
		int cell = l.cell();
		int cap = l.cap();
		int h = l.h();
		Smooth.roundRect(d, x, y, x + l.w(), y + h, h / 2f, FILL);
		Smooth.roundOutline(d, x, y, x + l.w(), y + h, h / 2f, 1, EDGE);
		int capTop = y + (h - cap) / 2;
		int cx = x + l.pad();
		if (l.logo()) {
			Wordmark.draw(d, cx, capTop, cell, WORD, WORD_SHADOW);
			cx += Wordmark.COLUMNS * cell + Math.round(0.5f * cap);
			for (char c : CLIENT.toCharArray()) {
				Component letter = UiFont.bold(String.valueOf(c));
				text(d, letter, cx, capTop, l.textScale(), CLIENT_COLOR);
				cx += Math.round(mc.font.width(letter) * l.textScale()) + l.letterGap();
			}
			cx -= l.letterGap();
		}
		if (l.logo() && l.name()) {
			int gap = Math.round(0.6f * cap);
			cx += gap;
			int lineH = Math.round(1.2f * cap);
			d.fill(cx, y + (h - lineH) / 2, cx + Math.max(1, cell), y + (h + lineH) / 2, EDGE);
			cx += Math.round(1.2f * cap) - gap;
		}
		if (l.name()) {
			if (l.head()) {
				d.face(ownSkin(mc), cx, y + (h - l.faceSize()) / 2, l.faceSize());
				cx += l.faceSize() + Math.round(0.4f * cap);
			}
			text(d, l.playerText(), cx, capTop, l.textScale(), 0xFFFFFFFF);
		}
	}

	private static void text(Draw d, Component text, float x, float y, float scale, int color) {
		d.push();
		d.translate(x, y);
		d.scale(scale);
		d.text(text, 0, 0, color, false);
		d.pop();
	}

	/** Whether the pill at this spot (in GUI pixels) would sit on one of the screen's buttons (small windows put buttons in the corners). */
	private static boolean coversButton(Screen screen, float x, float y, float w, float h) {
		for (AbstractWidget widget : Compat.widgets(screen)) {
			if (widget.visible && widget.getX() < x + w && widget.getX() + widget.getWidth() > x && widget.getY() < y + h
					&& widget.getY() + widget.getHeight() > y) {
				return true;
			}
		}
		return false;
	}

	private static PlayerSkin ownSkin(Minecraft mc) {
		ClientPacketListener connection = mc.getConnection();
		if (connection != null && mc.player != null) {
			PlayerInfo info = connection.getPlayerInfo(mc.player.getUUID());
			if (info != null) return info.getSkin();
		}
		if (offline == null) offline = new PlayerInfo(mc.getGameProfile(), false);
		return offline.getSkin();
	}
}
