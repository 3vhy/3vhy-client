package dev.eventide.client.hud;

import dev.eventide.client.Draw;
import dev.eventide.client.compat.Compat;
import dev.eventide.client.module.Modules;
import dev.eventide.client.module.PlayerList;
import dev.eventide.client.ui.Anim;
import dev.eventide.client.ui.Ease;
import dev.eventide.client.ui.Paint;
import dev.eventide.client.ui.Tokens;
import dev.eventide.client.ui.Type;
import dev.eventide.client.ui.Type.Weight;
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

/**
 * The Player List, on the same card as the Staff List: a violet tile with the list's title and a pill
 * counting who's on ("N online", mint while anyone is), then each listed player's head ringed in mint
 * when they're on, their name and an Online / Offline tag, six at most with "+N more" under them.
 * Someone coming online arrives with a mint glow, someone going offline dims, someone taken off the
 * list slides away, and the count rolls when it changes.
 */
final class PlayerListWidget extends HudWidget {
	private static final int SHOWN = 6;
	private static final float MORE_H = 40 * ListCard.K;
	private static final int MINT_TEXT = 0xFFD4F7E8;
	private static final String[] SAMPLE = {"Steve", "Alex", "Player372"};

	private final Map<String, RowState> states = new LinkedHashMap<>();
	private final Anim appear = new Anim(0, 420, Ease.OUT);
	private final Anim height = new Anim(ListCard.HEADER, 420, Ease.OUT);
	private final Anim lit = new Anim(0, 420, Ease.OUT);
	private boolean primed = false;
	private int shownCount = -1;
	private int oldCount = 0;
	private long countAt = 0;

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

	private static int online(List<Row> rows) {
		int count = 0;
		for (Row row : rows) {
			if (row.online()) count++;
		}
		return count;
	}

	private static List<Row> shown(List<Row> rows) {
		return rows.size() > SHOWN ? rows.subList(0, SHOWN) : rows;
	}

	private static float targetHeight(List<Row> rows) {
		return ListCard.height(shown(rows).size(), rows.size() > SHOWN ? MORE_H : 0);
	}

	@Override
	public boolean visible(Minecraft mc) {
		return !rows(mc, false).isEmpty() || appear.get() > 0.01f;
	}

	@Override
	public int width(Minecraft mc) {
		return ListCard.W;
	}

	@Override
	public int height(Minecraft mc) {
		if (!primed) return (int) Math.ceil(targetHeight(rows(mc, true)));
		return (int) Math.ceil(height.get());
	}

	/** Brings each name's animation up to date: who came on (to glow), who went off, who moved, and the count. */
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
			if (!row.online()) state.joinedAt = 0;
			state.leaving = false;
			state.in.set(1);
			state.slot.set(i);
			state.online.set(row.online());
			state.wasOnline = row.online();
			state.row = row;
		}
		Iterator<RowState> it = states.values().iterator();
		while (it.hasNext()) {
			RowState state = it.next();
			if (present.contains(state.row.name().toLowerCase(Locale.ROOT))) continue;
			state.leaving = true;
			state.in.set(0);
			if (state.in.get() < 0.01f) it.remove();
		}
		if (count != shownCount) {
			oldCount = Math.max(0, shownCount);
			countAt = quiet || shownCount < 0 ? now - 1000 : now;
			shownCount = count;
		}
	}

	@Override
	public void draw(Draw d, int x, int y, Minecraft mc, boolean preview) {
		List<Row> rows = rows(mc, preview);
		boolean quiet = !primed;
		int count = online(rows);
		follow(shown(rows), count, quiet);
		height.set(targetHeight(rows));
		lit.set(count > 0);
		appear.set(!rows.isEmpty());
		if (quiet || preview) appear.snap(appear.target());
		if (quiet) {
			height.snap(height.target());
			lit.snap(lit.target());
			primed = true;
		}
		float a = appear.get();
		if (a <= 0.01f) return;
		long now = System.currentTimeMillis();
		float h = height.get();
		float old = Paint.opacity(Paint.opacity() * a);
		float left = x + (1 - a) * 10;
		boolean shadow = textShadow();
		glass(d, left, y, ListCard.W, h, ListCard.RADIUS);
		float right = left + ListCard.W - ListCard.PAD;
		float pillLeft = pill(d, right, y + ListCard.HEADER / 2f, now);
		ListCard.header(d, left, y, "users", Modules.PLAYER_LIST_TITLE.getOrDefault(), pillLeft, shadow);

		float listTop = y + ListCard.HEADER;
		ListCard.divider(d, left, listTop, Math.min(1, (h - ListCard.HEADER) / (ListCard.ROW * 0.5f)));
		d.enableScissor((int) Math.floor(left), (int) Math.floor(listTop), (int) Math.ceil(left + ListCard.W), (int) Math.ceil(y + h));
		float top = listTop + ListCard.LIST_PAD;
		for (RowState state : states.values()) drawRow(d, state, left, top, shadow, now);
		if (rows.size() > SHOWN) {
			float cy = top + SHOWN * ListCard.ROW + MORE_H / 2f;
			String more = "+" + (rows.size() - SHOWN) + " more";
			Hud.text(d, more, ListCard.nameX(left), cy - Hud.SMALL / 2f, Hud.SMALL, Weight.SEMIBOLD, Tokens.TEXT_3, shadow);
		}
		d.disableScissor();
		Paint.opacity(old);
	}

	/** "N online", mint while anyone is on and grey when nobody is, the number rolling as it changes; returns the pill's left edge. */
	private float pill(Draw d, float right, float cy, long now) {
		float k = ListCard.K;
		float cap = ListCard.PILL_CAP;
		float on = lit.get();
		int count = Math.max(0, shownCount);
		float numW = Type.width(Integer.toString(count), cap, Weight.BOLD);
		float w = 12 * k + 3 + 4 + numW + 2.5f + Type.width("online", cap, Weight.MEDIUM) + 14 * k;
		int color = Draw.mix(Tokens.TEXT_3, Tokens.MINT, on);
		float x1 = ListCard.pill(d, right, cy, w, Paint.fade(color, 0.12f), Paint.fade(color, 0.45f));
		ListCard.clipPill(d, right, cy, w);
		float cx = x1 + 12 * k;
		Paint.circle(d, cx + 1.5f, cy, 1.5f, color);
		float nx = cx + 3 + 4;
		ListCard.number(d, count, oldCount, countAt, now, nx, cy, Draw.mix(Tokens.TEXT_2, Tokens.TEXT, on));
		Type.draw(d, "online", nx + numW + 2.5f, cy - cap / 2f, cap, Weight.MEDIUM, Draw.mix(Tokens.TEXT_3, MINT_TEXT, on));
		d.disableScissor();
		return x1;
	}

	/** One player: their head, name and Online / Offline tag, sliding and fading as they come and go. */
	private void drawRow(Draw d, RowState state, float left, float top, boolean shadow, long now) {
		float in = state.in.get();
		if (in <= 0.01f) return;
		Row row = state.row;
		float on = state.online.get();
		float cy = top + state.slot.get() * ListCard.ROW + ListCard.ROW / 2f;
		float shift = state.leaving ? -(1 - in) * 14 : (1 - in) * 6;
		float old = Paint.opacity(Paint.opacity() * in);
		float glow = Math.max(0, 1 - (now - state.joinedAt) / 1600f);
		ListCard.arrival(d, left + 8 * ListCard.K + shift, left + ListCard.W - 8 * ListCard.K + shift, cy, glow, Tokens.MINT);

		int ring = Draw.mix(Tokens.TEXT_3, Tokens.MINT, on);
		ListCard.head(d, row.info(), row.name(), ListCard.headX(left) + shift, cy, ring, 1 - 0.5f * (1 - on), glow);

		float tagRight = left + ListCard.W - ListCard.PAD + shift;
		float tagLeft = tagRight - Math.max(ListCard.tagWidth("ONLINE"), ListCard.tagWidth("OFFLINE"));
		if (on > 0.01f) {
			float tagOld = Paint.opacity(Paint.opacity() * on);
			ListCard.tag(d, "Online", tagRight, cy, Tokens.MINT);
			Paint.opacity(tagOld);
		}
		if (on < 0.99f) {
			float tagOld = Paint.opacity(Paint.opacity() * (1 - on));
			ListCard.tag(d, "Offline", tagRight, cy, Tokens.TEXT_3);
			Paint.opacity(tagOld);
		}
		ListCard.name(d, row.name(), ListCard.nameX(left) + shift, tagLeft, cy, Draw.mix(Tokens.TEXT_3, Tokens.TEXT, on), shadow);
		Paint.opacity(old);
	}

	/** One listed name's animation state. */
	private static final class RowState {
		final Anim in = new Anim(0, 420, Ease.OUT);
		final Anim slot;
		final Anim online;
		long joinedAt = 0;
		boolean wasOnline;
		boolean leaving = false;
		Row row;

		RowState(int index, boolean on) {
			slot = new Anim(index, 380, Ease.OUT);
			online = new Anim(on ? 1 : 0, 420, Ease.OUT);
			wasOnline = on;
		}
	}
}
