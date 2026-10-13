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
 * The Player List on the HUD, like 67's staff list: a small title with a red badge counting who's
 * on, then each listed player with their head and their name in capitals.
 */
final class PlayerListWidget extends HudWidget {
	private static final int ROW = 11;
	private static final int TOP = 17;
	private static final int PAD = 7;
	private static final int MAX_ROWS = 12;
	private static final float TITLE = 0.62f;
	private static final float NAME = 0.78f;
	private static final int FILL = 0x111216;
	private static final int BADGE = 0xFFE5484D;
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

	private static int width(Minecraft mc, Component text, float scale) {
		return Math.round(mc.font.width(text) * scale);
	}

	@Override
	public boolean visible(Minecraft mc) {
		return !rows(mc, false).isEmpty();
	}

	@Override
	public int width(Minecraft mc) {
		List<Row> rows = rows(mc, true);
		int widest = width(mc, bold(title()), TITLE) + 18;
		for (Row row : rows) widest = Math.max(widest, width(mc, bold(row.name().toUpperCase(Locale.ROOT)), NAME) + 12);
		return Math.max(70, widest + PAD * 2);
	}

	@Override
	public int height(Minecraft mc) {
		return TOP + rows(mc, true).size() * ROW + 4;
	}

	@Override
	public void draw(Draw d, int x, int y, Minecraft mc, boolean preview) {
		List<Row> rows = rows(mc, preview);
		int w = width(mc);
		int alpha = backgroundAlpha();
		if (alpha > 0) Smooth.roundRect(d, x, y, x + w, y + height(mc), 5, (alpha << 24) | FILL);
		boolean shadow = textShadow();
		small(d, bold(title()), x + PAD, y + 6, TITLE, 0xFFFFFFFF, shadow);
		String count = Integer.toString(online(rows));
		float badgeX = x + w - PAD - 3.5f;
		Smooth.circle(d, badgeX, y + 8, 4f, BADGE);
		Component countText = bold(count);
		float countW = mc.font.width(countText) * 0.55f;
		small(d, countText, badgeX - countW / 2f, y + 6.3f, 0.55f, 0xFFFFFFFF, false);
		int rowY = y + TOP;
		for (Row row : rows) {
			if (row.info() != null) {
				d.face(row.info().getSkin(), x + PAD, rowY, 8);
			} else {
				d.face(DefaultPlayerSkin.get(UUIDUtil.createOfflinePlayerUUID(row.name())), x + PAD, rowY, 8);
				if (!row.online()) d.fill(x + PAD, rowY, x + PAD + 8, rowY + 8, 0x90111216);
			}
			small(d, bold(row.name().toUpperCase(Locale.ROOT)), x + PAD + 12, rowY + 1.5f, NAME,
					row.online() ? 0xFFFFFFFF : 0xFF7D838F, shadow);
			rowY += ROW;
		}
	}

	private static void small(Draw d, Component text, float x, float y, float scale, int color, boolean shadow) {
		d.push();
		d.translate(x, y);
		d.scale(scale);
		d.text(text, 0, 0, color, shadow);
		d.pop();
	}
}
