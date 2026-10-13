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
 * The 3vhy Client logo in a small dark pill at the bottom right of the main menu, the pause menu,
 * your inventory and chests: the pixel wordmark and CLIENT. It can also show your head and name
 * (Name Protect and Skin Protect apply to them like everywhere else), or only those.
 */
public final class MenuNameTag {
	private static final int MARGIN = 6;
	private static final int HEIGHT = 19;
	private static final int FILL = 0xD8161719;
	private static final int EDGE = 0x30FFFFFF;
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
		boolean logo = !Modules.MENU_NAME_SHOW.is("Name");
		boolean name = !Modules.MENU_NAME_SHOW.is("Logo");
		boolean head = Modules.MENU_NAME_HEAD.get();
		String player = mc.getUser().getName();
		String client = "CLIENT";

		int logoW = Wordmark.COLUMNS + 5 + mc.font.width(client);
		int nameW = mc.font.width(player) + (head ? 13 : 0);
		int w = 16 + (logo ? logoW : 0) + (logo && name ? 13 : 0) + (name ? nameW : 0);
		int x = d.width() - MARGIN - w;
		int y = d.height() - MARGIN - HEIGHT;
		d.roundRect(x, y, x + w, y + HEIGHT, HEIGHT / 2, FILL);
		d.roundOutline(x, y, x + w, y + HEIGHT, HEIGHT / 2, EDGE);

		int cx = x + 8;
		if (logo) {
			Wordmark.draw(d, cx, y + 2, 1, Theme.ICE, 0);
			cx += Wordmark.COLUMNS + 2;
			d.fill(cx, y + 9, cx + 2, y + 11, Theme.CYAN);
			cx += 3;
			d.text(client, cx, y + 6, 0xFFB8C0CC, false);
			cx += mc.font.width(client);
		}
		if (logo && name) {
			cx += 6;
			d.fill(cx, y + 5, cx + 1, y + HEIGHT - 5, EDGE);
			cx += 7;
		}
		if (name) {
			if (head) {
				d.face(ownSkin(mc), cx, y + 4, 10);
				cx += 13;
			}
			d.text(player, cx, y + 6, Theme.ICE, false);
		}
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
