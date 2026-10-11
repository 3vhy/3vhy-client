package dev.threevhy.client.gui;

import dev.threevhy.client.Draw;
import dev.threevhy.client.Hooks;
import dev.threevhy.client.hud.Theme;
import dev.threevhy.client.module.Modules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.PlayerSkin;

/**
 * Your head and name in a small glass pill at the bottom right of the main menu, the pause menu,
 * your inventory and chests. Name Protect and Skin Protect apply to it like everywhere else.
 */
public final class MenuNameTag {
	private static final int MARGIN = 6;
	private static final int HEIGHT = 16;
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
		String name = mc.getUser().getName();
		boolean head = Modules.MENU_NAME_HEAD.get();
		int w = mc.font.width(name) + (head ? 26 : 14);
		int x = d.width() - MARGIN - w;
		int y = d.height() - MARGIN - HEIGHT;
		d.roundRect(x, y, x + w, y + HEIGHT, 8, 0xC0051246);
		d.roundOutline(x, y, x + w, y + HEIGHT, 8, 0x40A9D8F2);
		int textX = x + 7;
		if (head) {
			d.face(ownSkin(mc), x + 4, y + 3, 10);
			textX = x + 18;
		}
		d.text(name, textX, y + 4, Theme.ICE, false);
		Hooks.menuNameDrawn = true;
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
