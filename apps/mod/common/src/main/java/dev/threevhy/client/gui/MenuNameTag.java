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
 * everywhere else), or only those.
 *
 * <p>It's drawn in real screen pixels, each wordmark cell a whole number of them (two on a 1080p
 * screen), so it stays crisp and the same size at any GUI scale.
 */
public final class MenuNameTag {
	private static final int FILL = 0xE6111216;
	private static final int EDGE = 0x2EFFFFFF;
	private static final int WORD = 0xFFE3F3FC;
	private static final int WORD_SHADOW = 0xFF04104A;
	private static final int CLIENT_COLOR = 0xFF8CC4EA;
	/** The title screen's copyright line along the bottom (in GUI pixels), which the pill sits above. */
	private static final int COPYRIGHT_LINE = 10;
	/** Your own entry, for your skin when you're not in a world. */
	private static PlayerInfo offline = null;

	private MenuNameTag() {
	}

	/** The menus it shows on; other screens often have buttons in that corner. */
	public static boolean shows(Screen screen) {
		return screen instanceof MainMenuScreen || screen instanceof TitleScreen || screen instanceof PauseScreen
				|| screen instanceof AbstractContainerScreen<?>;
	}

	public static void draw(Draw d, Screen screen) {
		if (!Modules.MENU_NAME.enabled() || !shows(screen)) return;
		Minecraft mc = Minecraft.getInstance();
		float guiScale = (float) mc.getWindow().getGuiScale();
		// One wordmark cell in real pixels: 2 at 1080p, 3 at 4K
		int cell = Math.max(1, Math.round(mc.getWindow().getHeight() / 720f));
		boolean logo = !Modules.MENU_NAME_SHOW.is("Name");
		boolean name = !Modules.MENU_NAME_SHOW.is("Logo");
		boolean head = Modules.MENU_NAME_HEAD.get();
		String player = mc.getUser().getName();
		Component playerText = UiFont.semibold(player);

		// Sizes in real pixels
		int pad = 5 * cell;
		int wordW = Wordmark.COLUMNS * cell;
		int wordH = Wordmark.ROWS * cell;
		int h = wordH + cell + 2 * (2 * cell);
		float textScale = 4.4f * cell / 7f;
		int letterGap = 2 * cell;
		String client = "CLIENT";
		int clientW = -letterGap;
		for (char c : client.toCharArray()) clientW += Math.round(mc.font.width(UiFont.bold(String.valueOf(c))) * textScale) + letterGap;
		int logoW = wordW + 4 * cell + clientW;
		int faceSize = 9 * cell / 2 * 2;
		int nameW = Math.round(mc.font.width(playerText) * textScale * 1.15f) + (head ? faceSize + 3 * cell : 0);
		int w = 2 * pad + (logo ? logoW : 0) + (logo && name ? 9 * cell : 0) + (name ? nameW : 0);

		int screenW = Math.round(d.width() * guiScale);
		int screenH = Math.round(d.height() * guiScale);
		int margin = 4 * cell;
		int x = screenW - margin - w;
		int y = screenH - margin - h - (screen instanceof TitleScreen ? Math.round(COPYRIGHT_LINE * guiScale) : 0);
		if (coversButton(screen, x / guiScale, y / guiScale, w / guiScale, h / guiScale)) {
			y = margin;
			if (coversButton(screen, x / guiScale, y / guiScale, w / guiScale, h / guiScale)) return;
		}

		d.push();
		d.scale(1f / guiScale);
		Smooth.roundRect(d, x, y, x + w, y + h, h / 2f, FILL);
		Smooth.roundOutline(d, x, y, x + w, y + h, h / 2f, 1, EDGE);
		int cx = x + pad;
		if (logo) {
			int wordY = y + 2 * cell;
			Wordmark.draw(d, cx, wordY, cell, WORD, WORD_SHADOW);
			cx += wordW + 4 * cell;
			int ty = Math.round(wordY + 7 * cell - 3.5f * textScale);
			for (char c : client.toCharArray()) {
				Component letter = UiFont.bold(String.valueOf(c));
				d.push();
				d.translate(cx, ty);
				d.scale(textScale);
				d.text(letter, 0, 0, CLIENT_COLOR, false);
				d.pop();
				cx += Math.round(mc.font.width(letter) * textScale) + letterGap;
			}
			cx += -letterGap;
		}
		if (logo && name) {
			cx += 4 * cell;
			d.fill(cx, y + 4 * cell, cx + Math.max(1, cell / 2), y + h - 4 * cell, EDGE);
			cx += 5 * cell;
		}
		if (name) {
			if (head) {
				d.face(ownSkin(mc), cx, y + (h - faceSize) / 2, faceSize);
				cx += faceSize + 3 * cell;
			}
			float nameScale = textScale * 1.15f;
			d.push();
			d.translate(cx, y + (h - 7 * nameScale) / 2f);
			d.scale(nameScale);
			d.text(playerText, 0, 0, 0xFFFFFFFF, false);
			d.pop();
		}
		d.pop();
		Hooks.menuNameDrawn = true;
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
