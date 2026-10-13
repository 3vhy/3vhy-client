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
 * The 3vhy Client logo in a small dark pill at the bottom right of the main menu, the pause menu,
 * your inventory and chests, like 67 Client's: 3VHY in the client's colour (Theme > Panels colour),
 * a dot and CLIENT, in the clean font with smooth corners. It can also show your head and name
 * (Name Protect and Skin Protect apply to them like everywhere else), or only those. It's the same
 * real size at any GUI scale.
 */
public final class MenuNameTag {
	/** Layout units to the window's height, like the Panels menu: a unit is two pixels at 1080p. */
	private static final float UNITS_PER_SCREEN = 540f;
	private static final int MARGIN = 8;
	private static final int HEIGHT = 17;
	private static final int PAD = 9;
	private static final int FILL = 0xE6161719;
	private static final int EDGE = 0x2EFFFFFF;
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
		float unit = Math.max(1f, mc.getWindow().getHeight() / UNITS_PER_SCREEN) / (float) mc.getWindow().getGuiScale();
		boolean logo = !Modules.MENU_NAME_SHOW.is("Name");
		boolean name = !Modules.MENU_NAME_SHOW.is("Logo");
		boolean head = Modules.MENU_NAME_HEAD.get();
		String player = mc.getUser().getName();
		Component brand = UiFont.bold("3VHY");
		Component dot = UiFont.bold("\u00B7");
		Component client = UiFont.bold("CLIENT");
		Component playerText = UiFont.semibold(player);

		int logoW = UiFont.width(brand) + 4 + UiFont.width(dot) + 4 + UiFont.width(client);
		int nameW = UiFont.width(playerText) + (head ? 13 : 0);
		int w = 2 * PAD + (logo ? logoW : 0) + (logo && name ? 15 : 0) + (name ? nameW : 0);
		int vw = Math.round(d.width() / unit);
		int vh = Math.round(d.height() / unit);
		int x = vw - MARGIN - w;
		int y = vh - MARGIN - HEIGHT - (screen instanceof TitleScreen ? Math.round(COPYRIGHT_LINE / unit) : 0);
		if (coversButton(screen, x * unit, y * unit, w * unit, HEIGHT * unit)) {
			y = MARGIN;
			if (coversButton(screen, x * unit, y * unit, w * unit, HEIGHT * unit)) return;
		}

		d.push();
		d.scale(unit);
		Smooth.roundRect(d, x, y, x + w, y + HEIGHT, HEIGHT / 2f, FILL);
		Smooth.roundOutline(d, x, y, x + w, y + HEIGHT, HEIGHT / 2f, 1, EDGE);
		int cx = x + PAD;
		int ty = y + 5;
		if (logo) {
			int accent = Modules.THEME_PANEL_COLOR.get() | 0xFF000000;
			d.text(brand, cx, ty, accent, false);
			cx += UiFont.width(brand) + 4;
			d.text(dot, cx, ty, 0xFF9AA0AA, false);
			cx += UiFont.width(dot) + 4;
			d.text(client, cx, ty, 0xFFFFFFFF, false);
			cx += UiFont.width(client);
		}
		if (logo && name) {
			cx += 7;
			d.fill(cx, y + 4, cx + 1, y + HEIGHT - 4, EDGE);
			cx += 8;
		}
		if (name) {
			if (head) {
				d.face(ownSkin(mc), cx, y + 4, 9);
				cx += 13;
			}
			d.text(playerText, cx, ty, 0xFFFFFFFF, false);
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
