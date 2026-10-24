package dev.threevhy.client.hud;

import dev.threevhy.client.Draw;
import dev.threevhy.client.compat.Compat;
import dev.threevhy.client.gui.Accent;
import dev.threevhy.client.gui.menu.MenuScreen;
import dev.threevhy.client.module.Modules;
import dev.threevhy.client.module.PlayerList;
import dev.threevhy.client.ui.Anim;
import dev.threevhy.client.ui.Ease;
import dev.threevhy.client.ui.Paint;
import dev.threevhy.client.ui.Tokens;
import dev.threevhy.client.ui.Type;
import dev.threevhy.client.ui.Type.Weight;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.UUIDUtil;

/**
 * The Player List on the HUD, on violet glass: the list's title with a badge counting who's on, then
 * each listed player with a dot (mint when they're on) or their head, six at most with "+N more"
 * under them. Someone joining slides in with a pulse on their dot, someone leaving dims and slides
 * out, the rest glide to their new places, and the count gives a little bump when it changes.
 */
final class PlayerListWidget extends HudWidget {
	private static final float PAD = 7;
	private static final float HEADER = 17;
	private static final float ROW = 11;
	private static final float ICON = 7;
	private static final float CAP = Hud.CAP * 0.92f;
	private static final int SHOWN = 6;
	private static final String[] SAMPLE = {"Steve", "Alex", "Player372"};

	private final Map<String, RowState> states = new LinkedHashMap<>();
	private final Anim appear = new Anim(0, 420, Ease.OUT);
	private final Anim widthAnim = new Anim(0, 380, Ease.OUT);
	private final Anim heightAnim = new Anim(0, 380, Ease.OUT);
	private boolean sized = false;
	private int lastCount = -1;
	private long countChangedAt = 0;

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
		return !rows(mc, false).isEmpty() || appear.get() > 0.01f;
	}

	private static String more(List<Row> rows) {
		return "+" + (rows.size() - SHOWN) + " more";
	}

	private static List<Row> shown(List<Row> rows) {
		return rows.size() > SHOWN ? rows.subList(0, SHOWN) : rows;
	}

	private static float badgeWidth(int count) {
		return Math.max(12, Type.width(Integer.toString(count), Hud.SMALL, Weight.BOLD) + 7);
	}

	private float targetWidth(Minecraft mc, List<Row> rows) {
		float widest = PAD + 9 + 4 + Type.spacedWidth(title(), Hud.SMALL, 0.08f, Weight.SEMIBOLD) + 10 + badgeWidth(online(rows)) + PAD;
		for (Row row : shown(rows)) widest = Math.max(widest, PAD + ICON + 5 + Type.width(row.name(), CAP, Weight.SEMIBOLD) + 10 + PAD);
		return Math.max(78, widest);
	}

	private static float targetHeight(List<Row> rows) {
		return HEADER + Math.max(1, shown(rows).size()) * ROW + (rows.size() > SHOWN ? ROW - 2 : 0) + 4;
	}

	@Override
	public int width(Minecraft mc) {
		List<Row> rows = rows(mc, true);
		if (!sized) return (int) Math.ceil(targetWidth(mc, rows));
		widthAnim.set(targetWidth(mc, rows));
		return (int) Math.ceil(widthAnim.get());
	}

	@Override
	public int height(Minecraft mc) {
		List<Row> rows = rows(mc, true);
		if (!sized) return (int) Math.ceil(targetHeight(rows));
		heightAnim.set(targetHeight(rows));
		return (int) Math.ceil(heightAnim.get());
	}

	/** Brings each name's animation up to date: who joined, who left, who moved, and the count. */
	private void follow(List<Row> shown, int count, boolean quiet) {
		long now = System.currentTimeMillis();
		Set<String> present = new HashSet<>();
		for (int i = 0; i < shown.size(); i++) {
			Row row = shown.get(i);
			String key = row.name().toLowerCase(Locale.ROOT);
			present.add(key);
			RowState state = states.get(key);
			if (state == null) {
				state = new RowState(i, row.online());
				states.put(key, state);
				if (quiet) state.in.snap(1);
				else if (row.online()) state.joinedAt = now;
			} else if (row.online() && !state.wasOnline && !quiet) {
				state.joinedAt = now;
			}
			state.in.set(1);
			state.slot.set(i);
			state.online.set(row.online());
			state.wasOnline = row.online();
			state.row = row;
		}
		Iterator<Map.Entry<String, RowState>> it = states.entrySet().iterator();
		while (it.hasNext()) {
			RowState state = it.next().getValue();
			if (present.contains(state.row.name().toLowerCase(Locale.ROOT))) continue;
			state.in.set(0);
			if (state.in.get() < 0.01f) it.remove();
		}
		if (lastCount >= 0 && count != lastCount && !quiet) countChangedAt = now;
		lastCount = count;
	}

	@Override
	public void draw(Draw d, int x, int y, Minecraft mc, boolean preview) {
		List<Row> rows = rows(mc, preview);
		boolean quiet = !sized;
		int count = online(rows);
		follow(shown(rows), count, quiet);
		if (!sized) {
			widthAnim.snap(targetWidth(mc, rows));
			heightAnim.snap(targetHeight(rows));
			sized = true;
		}
		appear.set(!rows.isEmpty());
		if (preview || quiet) appear.snap(rows.isEmpty() ? 0 : 1);
		float a = appear.get();
		if (a <= 0.01f) return;
		long now = System.currentTimeMillis();
		float w = width(mc);
		float h = height(mc);
		float old = Paint.opacity(Paint.opacity() * a);
		float left = x + (1 - a) * 10;
		boolean shadow = textShadow();
		glass(d, left, y, w, h, Hud.RADIUS);
		Paint.icon(d, "users", left + PAD, y + 5, 8, Accent.text());
		Type.spaced(d, title(), left + PAD + 12, y + 7, Hud.SMALL, 0.08f, Weight.SEMIBOLD, Tokens.TEXT_2);

		String label = Integer.toString(count);
		float bump = 1 + 0.28f * (1 - Ease.OUT.apply((now - countChangedAt) / 520.0));
		float bw = badgeWidth(count);
		float bcx = left + w - PAD - bw / 2f;
		float bcy = y + 9;
		float hw = bw / 2f * bump;
		float hh = 4 * bump;
		if (bump > 1.01f) Paint.shadow(d, bcx - hw, bcy - hh, bcx + hw, bcy + hh, hh, 0, 0, 8, 0, Accent.glow((bump - 1) * 2.5f));
		Paint.rect(d, bcx - hw, bcy - hh, bcx + hw, bcy + hh, hh, Accent.primary());
		float cap = Hud.SMALL * bump;
		Type.center(d, label, bcx, bcy - cap / 2f, cap, Weight.BOLD, 0xFFFFFFFF);
		Paint.rect(d, left + PAD, y + HEADER - 1.5f, left + w - PAD, y + HEADER - 1, 0, 0x14FFFFFF);

		boolean heads = Modules.PLAYER_LIST_ICON.is("Head");
		d.enableScissor((int) Math.floor(left), (int) Math.floor(y + HEADER - 1), (int) Math.ceil(left + w), (int) Math.ceil(y + h));
		for (RowState state : states.values()) drawRow(d, state, left, y, heads, shadow, now);
		d.disableScissor();
		if (rows.size() > SHOWN) {
			float moreY = y + HEADER + SHOWN * ROW + ROW / 2f;
			Hud.text(d, more(rows), left + PAD + ICON + 5, moreY - Hud.SMALL / 2f - 1, Hud.SMALL, Weight.SEMIBOLD, Tokens.TEXT_3, shadow);
		}
		Paint.opacity(old);
	}

	/**
	 * One name: slides in from the right as they join (their dot pulsing for a moment), dims when they
	 * go offline, and slides back out as they leave the list.
	 */
	private void drawRow(Draw d, RowState state, float left, float top, boolean heads, boolean shadow, long now) {
		float in = state.in.get();
		if (in <= 0.01f) return;
		Row row = state.row;
		float mid = top + HEADER + ROW / 2f + state.slot.get() * ROW;
		float shift = (1 - in) * 14;
		float on = state.online.get();
		float old = Paint.opacity(Paint.opacity() * in);
		float ix = left + PAD + shift;
		if (heads) {
			MenuScreen.face(d, row.info() != null ? row.info().getSkin() : DefaultPlayerSkin.get(UUIDUtil.createOfflinePlayerUUID(row.name())), ix,
					mid - ICON / 2f, ICON, 2);
			if (on < 0.99f) {
				float dim = Paint.opacity(Paint.opacity() * (1 - on) * 0.6f);
				Paint.rect(d, ix, mid - ICON / 2f, ix + ICON, mid + ICON / 2f, 2, 0xFF0A0814);
				Paint.opacity(dim);
			}
		} else {
			float cx = ix + ICON / 2f;
			float pulse = (now - state.joinedAt) / 1600f;
			if (pulse < 1) {
				float ring = Ease.OUT.apply(pulse);
				Paint.ring(d, cx, mid, 2.2f + ring * 5, 0.8f, Paint.fade(Tokens.MINT, 0.7f * (1 - pulse)));
			}
			if (on > 0.01f) Paint.glow(d, cx, mid, 2, 5, Paint.fade(Tokens.MINT, 0.45f * on));
			Paint.circle(d, cx, mid, 2.2f, Draw.mix(Tokens.TEXT_3, Tokens.MINT, on));
		}
		Hud.text(d, row.name(), ix + ICON + 5, mid - CAP / 2f, CAP, Weight.SEMIBOLD, Draw.mix(Tokens.TEXT_3, Tokens.TEXT, on), shadow);
		Paint.opacity(old);
	}

	/** One listed name's animation state. */
	private static final class RowState {
		final Anim in = new Anim(0, 420, Ease.OUT);
		final Anim slot;
		final Anim online;
		long joinedAt = 0;
		boolean wasOnline;
		Row row;

		RowState(int index, boolean on) {
			slot = new Anim(index, 380, Ease.OUT);
			online = new Anim(on ? 1 : 0, 420, Ease.OUT);
			wasOnline = on;
		}
	}
}
