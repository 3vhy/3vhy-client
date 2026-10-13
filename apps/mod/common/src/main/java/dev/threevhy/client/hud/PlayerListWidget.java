package dev.threevhy.client.hud;

import dev.threevhy.client.Draw;
import dev.threevhy.client.compat.Compat;
import dev.threevhy.client.gui.Accent;
import dev.threevhy.client.gui.Smooth;
import dev.threevhy.client.gui.UiFont;
import dev.threevhy.client.module.Modules;
import dev.threevhy.client.module.PlayerList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;

/**
 * The Player List on the HUD, like 67's staff list: a dark rounded box with a small title and a badge
 * in the client's colour counting who's on, then each listed player's head (where 67 has a star) and
 * their name in capitals, six at most with "+N MORE" under them. Every size is a share of the names'
 * capital height, measured off 67's.
 */
final class PlayerListWidget extends HudWidget {
	private static final float NAME = 0.76f;
	private static final float TITLE = 0.48f;
	private static final float COUNT = 0.5f;
	private static final float MORE = 0.43f;
	private static final float C = 6.8f * NAME;
	private static final float PAD = 0.9f * C;
	private static final float HEAD = 1.1f * C;
	private static final float HEAD_GAP = 0.55f * C;
	private static final float TITLE_TOP = 1.2f * C;
	private static final float TITLE_MID = TITLE_TOP + 3.4f * TITLE;
	private static final float FIRST_ROW = TITLE_MID + 2.6f * C;
	private static final float ROW = 2.17f * C;
	private static final float BOTTOM = 2.05f * C;
	private static final float MORE_GAP = 1.9f * C;
	private static final float MORE_BOTTOM = 1.5f * C;
	private static final float END_PAD = 1.9f * C;
	private static final float BADGE_R = 0.8f * C;
	private static final float BADGE_RING = 0.15f * C;
	private static final float BADGE_PAD = 0.8f * C;
	private static final float RADIUS = 0.67f * C;
	private static final int SHOWN = 6;
	private static final int FILL = 0x121019;
	private static final int TITLE_COLOR = 0xFFD9DADF;
	private static final int NAME_COLOR = 0xFFFFFFFF;
	private static final int MORE_COLOR = 0xFF6C6672;
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

	private static String more(List<Row> rows) {
		return "+" + (rows.size() - SHOWN) + " MORE";
	}

	@Override
	public int width(Minecraft mc) {
		float widest = PAD + width(mc, bold(title()), TITLE) + 2 * C + 2 * BADGE_R + BADGE_PAD;
		for (Row row : shown(rows(mc, true))) {
			float name = width(mc, bold(row.name().toUpperCase(Locale.ROOT)), NAME);
			widest = Math.max(widest, PAD + HEAD + HEAD_GAP + name + END_PAD);
		}
		return (int) Math.ceil(widest);
	}

	private static List<Row> shown(List<Row> rows) {
		return rows.size() > SHOWN ? rows.subList(0, SHOWN) : rows;
	}

	@Override
	public int height(Minecraft mc) {
		List<Row> rows = rows(mc, true);
		float lastRow = FIRST_ROW + (Math.max(1, shown(rows).size()) - 1) * ROW;
		return (int) Math.ceil(rows.size() > SHOWN ? lastRow + MORE_GAP + MORE_BOTTOM : lastRow + BOTTOM);
	}

	@Override
	public void draw(Draw d, int x, int y, Minecraft mc, boolean preview) {
		List<Row> rows = rows(mc, preview);
		int w = width(mc);
		int alpha = backgroundAlpha();
		if (alpha > 0) Smooth.roundRect(d, x, y, x + w, y + height(mc), RADIUS, (alpha << 24) | FILL);
		boolean shadow = textShadow();
		text(d, bold(title()), x + PAD, y + TITLE_TOP, TITLE, TITLE_COLOR, shadow);

		int accent = Accent.get();
		float badgeX = x + w - BADGE_PAD - BADGE_R;
		float badgeY = y + TITLE_MID;
		int boxAlpha = Math.max(alpha, 0x60) << 24;
		Smooth.circle(d, badgeX, badgeY, BADGE_R, boxAlpha | (Draw.mix(FILL, accent, 0.3) & 0xFFFFFF));
		Smooth.ring(d, badgeX, badgeY, BADGE_R, BADGE_RING,
				boxAlpha | (Draw.mix(FILL, Draw.mix(accent, 0xFFFFFFFF, 0.35), 0.45) & 0xFFFFFF));
		Component count = bold(Integer.toString(online(rows)));
		text(d, count, badgeX - width(mc, count, COUNT) / 2f, badgeY - 3.4f * COUNT, COUNT,
				Draw.mix(accent, 0xFFFFFFFF, 0.55), false);

		float mid = y + FIRST_ROW;
		for (Row row : shown(rows)) {
			float headX = x + PAD;
			float headY = mid - HEAD / 2f;
			d.push();
			d.translate(headX, headY);
			d.scale(HEAD / 8f);
			if (row.info() != null) {
				d.face(row.info().getSkin(), 0, 0, 8);
			} else {
				d.face(DefaultPlayerSkin.get(UUIDUtil.createOfflinePlayerUUID(row.name())), 0, 0, 8);
			}
			d.pop();
			text(d, bold(row.name().toUpperCase(Locale.ROOT)), headX + HEAD + HEAD_GAP, mid - C / 2f, NAME,
					NAME_COLOR, shadow);
			mid += ROW;
		}
		if (rows.size() > SHOWN) {
			float moreY = mid - ROW + MORE_GAP;
			text(d, bold(more(rows)), x + PAD + HEAD + HEAD_GAP, moreY - 3.4f * MORE, MORE, MORE_COLOR, shadow);
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
