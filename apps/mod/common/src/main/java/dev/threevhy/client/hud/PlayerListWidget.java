package dev.threevhy.client.hud;

import dev.threevhy.client.Draw;
import dev.threevhy.client.compat.Compat;
import dev.threevhy.client.module.Modules;
import dev.threevhy.client.module.PlayerList;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.UUIDUtil;

/** The Player List on the HUD: a title with how many are on, then each listed player with their head. */
final class PlayerListWidget extends HudWidget {
	private static final int ROW = 11;
	private static final int MAX_ROWS = 12;
	private static final String[] SAMPLE = {"Steve", "Alex", "Player372"};

	private record Row(String name, PlayerInfo info, boolean online) {
	}

	PlayerListWidget() {
		super("player_list", Modules.PLAYER_LIST, Anchor.TOP_RIGHT);
	}

	private static List<Row> rows(Minecraft mc, boolean preview) {
		List<Row> rows = new ArrayList<>();
		boolean showOffline = Modules.PLAYER_LIST_OFFLINE.get();
		for (String name : PlayerList.names()) {
			if (rows.size() >= MAX_ROWS) break;
			PlayerInfo info = PlayerList.isOnline(name) ? PlayerList.info(mc, name) : null;
			if (info == null && !showOffline) continue;
			String shown = info == null ? name : Compat.profileName(info.getProfile());
			rows.add(new Row(shown == null ? name : shown, info, info != null));
		}
		if (rows.isEmpty() && preview) {
			for (String name : SAMPLE) rows.add(new Row(name, null, true));
		}
		return rows;
	}

	private static String title() {
		return Modules.PLAYER_LIST_TITLE.getOrDefault().toUpperCase(Locale.ROOT);
	}

	private static int online(List<Row> rows) {
		int count = 0;
		for (Row row : rows) {
			if (row.online()) count++;
		}
		return count;
	}

	@Override
	public boolean visible(Minecraft mc) {
		return !rows(mc, false).isEmpty();
	}

	@Override
	public int width(Minecraft mc) {
		List<Row> rows = rows(mc, true);
		int widest = textWidth(mc, title() + " " + online(rows)) + 12;
		for (Row row : rows) widest = Math.max(widest, textWidth(mc, row.name()) + 24);
		return Math.max(80, widest);
	}

	@Override
	public int height(Minecraft mc) {
		return 15 + rows(mc, true).size() * ROW + 2;
	}

	@Override
	public void draw(Draw d, int x, int y, Minecraft mc, boolean preview) {
		List<Row> rows = rows(mc, preview);
		int w = width(mc);
		panel(d, x, y, w, height(mc));
		boolean shadow = textShadow();
		String title = title();
		d.text(title, x + 6, y + 4, Theme.MUTED, shadow);
		d.text(Integer.toString(online(rows)), x + 6 + textWidth(mc, title + " "), y + 4, Theme.CYAN, shadow);
		int rowY = y + 15;
		for (Row row : rows) {
			if (row.info() != null) {
				d.face(row.info().getSkin(), x + 6, rowY, 8);
			} else {
				d.face(DefaultPlayerSkin.get(UUIDUtil.createOfflinePlayerUUID(row.name())), x + 6, rowY, 8);
				if (!row.online()) box(d, x + 6, rowY, x + 14, rowY + 8, 0x90060E2B);
			}
			d.text(row.name(), x + 18, rowY, row.online() ? Theme.ICE : Theme.MUTED, shadow);
			rowY += ROW;
		}
	}
}
