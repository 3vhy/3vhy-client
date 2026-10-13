package dev.threevhy.client.hud;

import dev.threevhy.client.Draw;
import dev.threevhy.client.compat.Compat;
import dev.threevhy.client.gui.Smooth;
import dev.threevhy.client.gui.UiFont;
import dev.threevhy.client.module.Modules;
import dev.threevhy.client.module.PlayerList;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;

/**
 * The Player List on the HUD, like 67's staff list: a dark rounded box with a small title and a dark
 * red badge counting who's on, then each listed player's head (where 67 has a star) and their name
 * in capitals. Every size is a share of the names' capital height, measured off 67's.
 */
final class PlayerListWidget extends HudWidget {
	private static final float NAME = 0.76f;
	private static final float TITLE = 0.48f;
	private static final float COUNT = 0.5f;
	private static final float C = 6.8f * NAME;
	private static final float PAD = 0.9f * C;
	private static final float HEAD = 1.1f * C;
	private static final float HEAD_GAP = 0.55f * C;
	private static final float TITLE_TOP = 1.45f * C;
	private static final float TITLE_MID = TITLE_TOP + 3.4f * TITLE;
	private static final float FIRST_ROW = TITLE_MID + 2.6f * C;
	private static final float ROW = 2.05f * C;
	private static final float END_PAD = 1.9f * C;
	private static final float BADGE_R = 0.75f * C;
	private static final float BADGE_PAD = 0.8f * C;
	private static final float RADIUS = 0.67f * C;
	private static final int MAX_ROWS = 12;
	private static final int FILL = 0x111116;
	private static final int TITLE_COLOR = 0xFFD9DADF;
	private static final int NAME_COLOR = 0xFFF1F1F3;
	private static final int OFFLINE_COLOR = 0xFF6E7380;
	private static final int BADGE = 0xFF5C1717;
	private static final int BADGE_TEXT = 0xFFF07070;
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

	private static Component bold(String text) {
		return UiFont.clean() ? UiFont.bold(text) : Component.literal(text);
	}

	private static float width(Minecraft mc, Component text, float scale) {
		return mc.font.width(text) * scale;
	}

	@Override
	public boolean visible(Minecraft mc) {
		return !rows(mc, false).isEmpty();
	}

	@Override
	public int width(Minecraft mc) {
		float widest = PAD + width(mc, bold(title()), TITLE) + 2 * C + 2 * BADGE_R + BADGE_PAD;
		for (Row row : rows(mc, true)) {
			float name = width(mc, bold(row.name().toUpperCase(Locale.ROOT)), NAME);
			widest = Math.max(widest, PAD + HEAD + HEAD_GAP + name + END_PAD);
		}
		return (int) Math.ceil(widest);
	}

	@Override
	public int height(Minecraft mc) {
		int rows = Math.max(1, rows(mc, true).size());
		return (int) Math.ceil(FIRST_ROW + (rows - 1) * ROW + ROW);
	}

	@Override
	public void draw(Draw d, int x, int y, Minecraft mc, boolean preview) {
		List<Row> rows = rows(mc, preview);
		int w = width(mc);
		int alpha = backgroundAlpha();
		if (alpha > 0) Smooth.roundRect(d, x, y, x + w, y + height(mc), RADIUS, (alpha << 24) | FILL);
		boolean shadow = textShadow();
		text(d, bold(title()), x + PAD, y + TITLE_TOP, TITLE, TITLE_COLOR, shadow);

		float badgeX = x + w - BADGE_PAD - BADGE_R;
		float badgeY = y + TITLE_MID;
		Smooth.circle(d, badgeX, badgeY, BADGE_R, BADGE);
		Component count = bold(Integer.toString(online(rows)));
		text(d, count, badgeX - width(mc, count, COUNT) / 2f, badgeY - 3.4f * COUNT, COUNT, BADGE_TEXT, false);

		float mid = y + FIRST_ROW;
		for (Row row : rows) {
			float headX = x + PAD;
			float headY = mid - HEAD / 2f;
			d.push();
			d.translate(headX, headY);
			d.scale(HEAD / 8f);
			if (row.info() != null) {
				d.face(row.info().getSkin(), 0, 0, 8);
			} else {
				d.face(DefaultPlayerSkin.get(UUIDUtil.createOfflinePlayerUUID(row.name())), 0, 0, 8);
				if (!row.online()) d.fill(0, 0, 8, 8, 0x90111216);
			}
			d.pop();
			text(d, bold(row.name().toUpperCase(Locale.ROOT)), headX + HEAD + HEAD_GAP, mid - C / 2f, NAME,
					row.online() ? NAME_COLOR : OFFLINE_COLOR, shadow);
			mid += ROW;
		}
	}

	private static void text(Draw d, Component text, float x, float y, float scale, int color, boolean shadow) {
		d.push();
		d.translate(x, y);
		d.scale(scale);
		d.text(text, 0, 0, color, shadow);
		d.pop();
	}
}
