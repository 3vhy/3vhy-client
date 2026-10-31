package dev.eventide.client.hud;

import dev.eventide.client.Draw;
import dev.eventide.client.module.Modules;
import dev.eventide.client.module.PopTracker;
import dev.eventide.client.module.PopTracker.Entry;
import dev.eventide.client.ui.Anim;
import dev.eventide.client.ui.Ease;
import dev.eventide.client.ui.Paint;
import dev.eventide.client.ui.Tokens;
import dev.eventide.client.ui.Type;
import dev.eventide.client.ui.Type.Weight;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.UUIDUtil;

/**
 * Totem Pops, on the same card as the Staff List: a violet tile with "Totem Pops" and a pill counting
 * every pop, then the players who popped most, each with their head, their name and a tag with their
 * pops, lilac turning pink the more they've burned. A new pop flashes the row, a player who dies slides
 * away, and the rest move to their new places.
 */
final class PopsWidget extends HudWidget {
	private static final int MAX = 5;
	private static final int PINK = 0xFFFF6EB4;
	private static final Entry[] SAMPLE = {new Entry(UUIDUtil.createOfflinePlayerUUID("Steve"), "Steve", 3),
			new Entry(UUIDUtil.createOfflinePlayerUUID("Alex"), "Alex", 1)};

	private final Map<UUID, RowState> rows = new LinkedHashMap<>();
	private final Anim appear = new Anim(0, 420, Ease.OUT);
	private final Anim height = new Anim(ListCard.HEADER, 420, Ease.OUT);
	private boolean primed = false;
	private int total = -1;
	private int oldTotal = 0;
	private long totalAt = 0;

	PopsWidget() {
		super("pops", Modules.POPS, Anchor.TOP_RIGHT);
	}

	private static List<Entry> entries(boolean preview) {
		List<Entry> list = PopTracker.top(MAX);
		return list.isEmpty() && preview ? List.of(SAMPLE) : list;
	}

	@Override
	public boolean visible(Minecraft mc) {
		return !PopTracker.top(1).isEmpty() || appear.get() > 0.01f;
	}

	@Override
	public int width(Minecraft mc) {
		return ListCard.W;
	}

	@Override
	public int height(Minecraft mc) {
		if (!primed) return (int) Math.ceil(ListCard.height(entries(true).size(), 0));
		return (int) Math.ceil(height.get());
	}

	/** The tag's colour: lilac for one pop, turning pink by the fifth. */
	private static int color(int pops) {
		return Draw.mix(Tokens.LILAC, PINK, Math.min(1, (pops - 1) / 4.0));
	}

	/** Brings the rows up to date: new pops (to flash), who died (to slide away), and the total. */
	private void follow(List<Entry> list, boolean quiet) {
		long now = System.currentTimeMillis();
		Set<UUID> present = new HashSet<>();
		int sum = 0;
		for (int i = 0; i < list.size(); i++) {
			Entry entry = list.get(i);
			sum += entry.pops();
			present.add(entry.id());
			RowState state = rows.get(entry.id());
			if (state == null) {
				state = new RowState(i);
				rows.put(entry.id(), state);
				if (quiet) state.in.snap(1);
				else state.poppedAt = now;
			} else if (entry.pops() > state.entry.pops() && !quiet) {
				state.poppedAt = now;
			}
			state.leaving = false;
			state.in.set(1);
			state.slot.set(i);
			state.entry = entry;
		}
		Iterator<RowState> it = rows.values().iterator();
		while (it.hasNext()) {
			RowState state = it.next();
			if (present.contains(state.entry.id())) continue;
			state.leaving = true;
			state.in.set(0);
			if (state.in.get() < 0.01f) it.remove();
		}
		if (sum != total) {
			oldTotal = Math.max(0, total);
			totalAt = quiet || total < 0 ? now - 1000 : now;
			total = sum;
		}
	}

	@Override
	public void draw(Draw d, int x, int y, Minecraft mc, boolean preview) {
		List<Entry> list = entries(preview);
		boolean quiet = !primed;
		follow(list, quiet);
		height.set(ListCard.height(list.size(), 0));
		appear.set(!list.isEmpty());
		if (quiet || preview) appear.snap(appear.target());
		if (quiet) {
			height.snap(height.target());
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
		ListCard.header(d, left, y, "shield-plus", "Totem Pops", pillLeft, shadow);

		float listTop = y + ListCard.HEADER;
		if (!rows.isEmpty()) {
			ListCard.divider(d, left, listTop, Math.min(1, (h - ListCard.HEADER) / (ListCard.ROW * 0.5f)));
			d.enableScissor((int) Math.floor(left), (int) Math.floor(listTop), (int) Math.ceil(left + ListCard.W), (int) Math.ceil(y + h));
			for (RowState state : rows.values()) drawRow(d, mc, state, left, listTop + ListCard.LIST_PAD, shadow, now);
			d.disableScissor();
		}
		Paint.opacity(old);
	}

	/** "N pops" in lilac, the number rolling as it grows; returns the pill's left edge. */
	private float pill(Draw d, float right, float cy, long now) {
		float k = ListCard.K;
		float cap = ListCard.PILL_CAP;
		int count = Math.max(0, total);
		String label = count == 1 ? "pop" : "pops";
		float numW = Type.width(Integer.toString(count), cap, Weight.BOLD);
		float w = 12 * k + numW + 2.5f + Type.width(label, cap, Weight.MEDIUM) + 14 * k;
		float x1 = ListCard.pill(d, right, cy, w, Paint.fade(Tokens.LILAC, 0.12f), Paint.fade(Tokens.LILAC, 0.5f));
		ListCard.clipPill(d, right, cy, w);
		float nx = x1 + 12 * k;
		ListCard.number(d, count, oldTotal, totalAt, now, nx, cy, Tokens.TEXT);
		Type.draw(d, label, nx + numW + 2.5f, cy - cap / 2f, cap, Weight.MEDIUM, Tokens.LILAC);
		d.disableScissor();
		return x1;
	}

	private void drawRow(Draw d, Minecraft mc, RowState state, float left, float top, boolean shadow, long now) {
		float in = state.in.get();
		if (in <= 0.01f) return;
		Entry entry = state.entry;
		int color = color(entry.pops());
		float cy = top + state.slot.get() * ListCard.ROW + ListCard.ROW / 2f;
		float shift = state.leaving ? -(1 - in) * 14 : (1 - in) * 6;
		float old = Paint.opacity(Paint.opacity() * in);
		float flash = Math.max(0, 1 - (now - state.poppedAt) / 1200f);
		ListCard.arrival(d, left + 8 * ListCard.K + shift, left + ListCard.W - 8 * ListCard.K + shift, cy, flash, color);
		PlayerInfo info = mc.getConnection() != null ? mc.getConnection().getPlayerInfo(entry.id()) : null;
		ListCard.head(d, info, entry.name(), ListCard.headX(left) + shift, cy, color, 1, flash);
		String tag = entry.pops() + (entry.pops() == 1 ? " POP" : " POPS");
		float tagLeft = ListCard.tag(d, tag, left + ListCard.W - ListCard.PAD + shift, cy, color);
		ListCard.name(d, entry.name(), ListCard.nameX(left) + shift, tagLeft, cy, Tokens.TEXT, shadow);
		Paint.opacity(old);
	}

	/** One player's animation state. */
	private static final class RowState {
		final Anim in = new Anim(0, 420, Ease.OUT);
		final Anim slot;
		long poppedAt = 0;
		boolean leaving = false;
		Entry entry;

		RowState(int index) {
			slot = new Anim(index, 380, Ease.OUT);
		}
	}
}
