package dev.threevhy.client.hud;

import dev.threevhy.client.Draw;
import dev.threevhy.client.compat.Compat;
import dev.threevhy.client.gui.Accent;
import dev.threevhy.client.gui.menu.MenuScreen;
import dev.threevhy.client.module.Modules;
import dev.threevhy.client.module.PlayerList;
import dev.threevhy.client.ui.Paint;
import dev.threevhy.client.ui.Tokens;
import dev.threevhy.client.ui.Type;
import dev.threevhy.client.ui.Type.Weight;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.UUIDUtil;

/**
 * The Player List on the HUD, on violet glass: the list's title with a badge counting who's on, then
 * each listed player with a dot (mint when they're on) or their head, six at most with "+N more"
 * under them.
 */
final class PlayerListWidget extends HudWidget {
	private static final float PAD = 7;
	private static final float HEADER = 17;
	private static final float ROW = 11;
	private static final float ICON = 7;
	private static final float CAP = Hud.CAP * 0.92f;
	private static final int SHOWN = 6;
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
			PlayerInfo info = PlayerList.isOnline(name) ? PlayerList.info(mc, name) : null;
			if (info == null && !showOffline) continue;
			String shown = info == null ? name : Compat.profileName(info.getProfile());
			rows.add(new Row(shown == null ? name : shown, info, info != null));
		}
		if (rows.isEmpty() && preview) {
			for (String name : SAMPLE) rows.add(new Row(name, null, true));
		}
		rows.sort(Comparator.comparing((Row row) -> !row.online()));
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

	private static String more(List<Row> rows) {
		return "+" + (rows.size() - SHOWN) + " more";
	}

	private static List<Row> shown(List<Row> rows) {
		return rows.size() > SHOWN ? rows.subList(0, SHOWN) : rows;
	}

	private static float badgeWidth(List<Row> rows) {
		return Math.max(12, Type.width(Integer.toString(online(rows)), Hud.SMALL, Weight.BOLD) + 7);
	}

	@Override
	public int width(Minecraft mc) {
		List<Row> rows = rows(mc, true);
		float widest = PAD + 9 + 4 + Type.spacedWidth(title(), Hud.SMALL, 0.08f, Weight.SEMIBOLD) + 10 + badgeWidth(rows) + PAD;
		for (Row row : shown(rows)) widest = Math.max(widest, PAD + ICON + 5 + Type.width(row.name(), CAP, Weight.SEMIBOLD) + 10 + PAD);
		return (int) Math.ceil(Math.max(78, widest));
	}

	@Override
	public int height(Minecraft mc) {
		List<Row> rows = rows(mc, true);
		float h = HEADER + Math.max(1, shown(rows).size()) * ROW + (rows.size() > SHOWN ? ROW - 2 : 0) + 4;
		return (int) Math.ceil(h);
	}

	@Override
	public void draw(Draw d, int x, int y, Minecraft mc, boolean preview) {
		List<Row> rows = rows(mc, preview);
		int w = width(mc);
		boolean shadow = textShadow();
		glass(d, x, y, w, height(mc), Hud.RADIUS);
		Paint.icon(d, "users", x + PAD, y + 5, 8, Accent.text());
		Type.spaced(d, title(), x + PAD + 12, y + 7, Hud.SMALL, 0.08f, Weight.SEMIBOLD, Tokens.TEXT_2);
		String count = Integer.toString(online(rows));
		float bw = badgeWidth(rows);
		float bx = x + w - PAD - bw;
		Paint.rect(d, bx, y + 5, bx + bw, y + 13, 4, Accent.primary());
		Type.center(d, count, bx + bw / 2f, y + 9 - Hud.SMALL / 2f, Hud.SMALL, Weight.BOLD, 0xFFFFFFFF);
		Paint.rect(d, x + PAD, y + HEADER - 1.5f, x + w - PAD, y + HEADER - 1, 0, 0x14FFFFFF);

		float mid = y + HEADER + ROW / 2f;
		boolean heads = Modules.PLAYER_LIST_ICON.is("Head");
		for (Row row : shown(rows)) {
			if (heads) {
				MenuScreen.face(d, row.info() != null ? row.info().getSkin() : DefaultPlayerSkin.get(UUIDUtil.createOfflinePlayerUUID(row.name())),
						x + PAD, mid - ICON / 2f, ICON, 2);
			} else {
				int dot = row.online() ? Tokens.MINT : Tokens.TEXT_3;
				if (row.online()) Paint.glow(d, x + PAD + ICON / 2f, mid, 2, 5, Paint.fade(Tokens.MINT, 0.45f));
				Paint.circle(d, x + PAD + ICON / 2f, mid, 2.2f, dot);
			}
			Hud.text(d, row.name(), x + PAD + ICON + 5, mid - CAP / 2f, CAP, Weight.SEMIBOLD, row.online() ? Tokens.TEXT : Tokens.TEXT_3, shadow);
			mid += ROW;
		}
		if (rows.size() > SHOWN) {
			Hud.text(d, more(rows), x + PAD + ICON + 5, mid - Hud.SMALL / 2f - 1, Hud.SMALL, Weight.SEMIBOLD, Tokens.TEXT_3, shadow);
		}
	}
}
