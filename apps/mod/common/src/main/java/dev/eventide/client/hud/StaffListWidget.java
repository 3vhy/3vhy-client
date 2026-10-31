package dev.eventide.client.hud;

import dev.eventide.client.Draw;
import dev.eventide.client.Hooks;
import dev.eventide.client.module.Modules;
import dev.eventide.client.module.StaffList;
import dev.eventide.client.module.StaffList.Staff;
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
import net.minecraft.client.Minecraft;

/**
 * The Staff List, like the website's: a glass card with a violet shield tile and "Staff", and a pill
 * that says "All clear" in mint while no staff are on, or "N online" in pink with the number rolling as
 * it changes. Under it, each staff member's head ringed in their rank's colour, their name and a rank
 * tag. Someone joining arrives with a glow in their rank's colour, someone spectating dims with an
 * eye-off badge on their head, and someone leaving slides away as the card shrinks.
 */
final class StaffListWidget extends HudWidget {
	private static final int MINT = Tokens.MINT;
	private static final int PINK = 0xFFFF6EB4;
	private static final Staff[] SAMPLE = {new Staff("Alex", "ADMIN", StaffList.colorOf("admin"), null, false),
			new Staff("Kai", "MOD", StaffList.colorOf("mod"), null, false)};

	private final Map<String, RowState> rows = new LinkedHashMap<>();
	private final Anim appear = new Anim(0, 420, Ease.OUT);
	private final Anim height = new Anim(ListCard.HEADER, 420, Ease.OUT);
	private final Anim busy = new Anim(0, 420, Ease.OUT);
	private boolean primed = false;
	private int shownCount = 0;
	private int oldCount = 0;
	private long countAt = 0;

	StaffListWidget() {
		super("staff_list", Modules.STAFF_LIST, Anchor.TOP_RIGHT);
	}

	private static boolean onServer(Minecraft mc) {
		return mc.getCurrentServer() != null && mc.getConnection() != null || StaffList.simulating();
	}

	/** The staff to list; while editing the HUD away from a server, two made-up ones to show the look. */
	private static List<Staff> staff(Minecraft mc, boolean preview) {
		List<Staff> list = StaffList.current(mc);
		return list.isEmpty() && preview && !onServer(mc) ? List.of(SAMPLE) : list;
	}

	@Override
	public boolean visible(Minecraft mc) {
		return onServer(mc) || !StaffList.current(mc).isEmpty() || appear.get() > 0.01f;
	}

	@Override
	public int width(Minecraft mc) {
		return ListCard.W;
	}

	@Override
	public int height(Minecraft mc) {
		if (!primed) return (int) Math.ceil(ListCard.height(staff(mc, true).size(), 0));
		return (int) Math.ceil(height.get());
	}

	/** Brings the rows up to date: who arrived (to glow), who left (to slide away), who's spectating. */
	private void follow(List<Staff> list, boolean quiet) {
		long now = System.currentTimeMillis();
		Set<String> present = new HashSet<>();
		for (int i = 0; i < list.size(); i++) {
			Staff staff = list.get(i);
			present.add(staff.name());
			RowState state = rows.get(staff.name());
			if (state == null) {
				state = new RowState(i);
				rows.put(staff.name(), state);
				if (quiet) state.in.snap(1);
				else state.joinedAt = now;
			}
			state.leaving = false;
			state.in.set(1);
			state.slot.set(i);
			state.spectating.set(staff.spectating());
			state.staff = staff;
		}
		Iterator<RowState> it = rows.values().iterator();
		while (it.hasNext()) {
			RowState state = it.next();
			if (present.contains(state.staff.name())) continue;
			state.leaving = true;
			state.in.set(0);
			if (state.in.get() < 0.01f) it.remove();
		}
		int count = 0;
		for (Staff staff : list) {
			if (!staff.spectating()) count++;
		}
		if (count != shownCount) {
			oldCount = shownCount;
			shownCount = count;
			countAt = quiet ? now - 1000 : now;
		}
	}

	@Override
	public void draw(Draw d, int x, int y, Minecraft mc, boolean preview) {
		List<Staff> list = staff(mc, preview);
		boolean quiet = !primed;
		follow(list, quiet);
		height.set(ListCard.height(list.size(), 0));
		busy.set(shownCount > 0);
		appear.set(onServer(mc) || !list.isEmpty() || preview);
		if (quiet) {
			height.snap(height.target());
			busy.snap(busy.target());
			appear.snap(appear.target());
			primed = true;
		}
		float a = appear.get();
		if (a <= 0.01f) return;
		Hooks.staffListDrawn = true;
		long now = System.currentTimeMillis();
		float h = height.get();
		float old = Paint.opacity(Paint.opacity() * a);
		float left = x + (1 - a) * 10;
		boolean shadow = textShadow();
		glass(d, left, y, ListCard.W, h, ListCard.RADIUS);
		float right = left + ListCard.W - ListCard.PAD;
		float pillLeft = pill(d, right, y + ListCard.HEADER / 2f, now);
		ListCard.header(d, left, y, "shield-user", "Staff", pillLeft, shadow);

		float listTop = y + ListCard.HEADER;
		if (!rows.isEmpty()) {
			ListCard.divider(d, left, listTop, Math.min(1, (h - ListCard.HEADER) / (ListCard.ROW * 0.5f)));
			d.enableScissor((int) Math.floor(left), (int) Math.floor(listTop), (int) Math.ceil(left + ListCard.W), (int) Math.ceil(y + h));
			for (RowState state : rows.values()) drawRow(d, state, left, listTop + ListCard.LIST_PAD, shadow, now);
			d.disableScissor();
		}
		Paint.opacity(old);
	}

	/** "All clear" in mint, or "N online" in pink with the number rolling as it changes; returns the pill's left edge. */
	private float pill(Draw d, float right, float cy, long now) {
		float k = ListCard.K;
		float cap = ListCard.PILL_CAP;
		float b = busy.get();
		String count = Integer.toString(shownCount);
		float onlineW = 12 * k + 3 + 4 + Type.width(count, cap, Weight.BOLD) + 2.5f + Type.width("online", cap, Weight.MEDIUM) + 14 * k;
		float clearW = 12 * k + 6 + 3.5f + Type.width("All clear", cap, Weight.SEMIBOLD) + 16 * k;
		float w = clearW + (onlineW - clearW) * b;
		int stroke = Draw.mix(Paint.fade(MINT, 0.45f), Paint.fade(PINK, 0.55f), b);
		int fill = Draw.mix(Paint.fade(MINT, 0.12f), Paint.fade(PINK, 0.12f), b);
		float x1 = ListCard.pill(d, right, cy, w, fill, stroke);
		ListCard.clipPill(d, right, cy, w);
		float cx = x1 + 12 * k;
		if (b < 0.99f) {
			float old = Paint.opacity(Paint.opacity() * (1 - b));
			Paint.icon(d, "check", cx, cy - 3, 6, MINT);
			Type.draw(d, "All clear", cx + 6 + 3.5f, cy - cap / 2f, cap, Weight.SEMIBOLD, MINT);
			Paint.opacity(old);
		}
		if (b > 0.01f) {
			float old = Paint.opacity(Paint.opacity() * b);
			Paint.glow(d, cx + 1.5f, cy, 1.5f, 4, Paint.fade(PINK, 0.5f));
			Paint.circle(d, cx + 1.5f, cy, 1.5f, PINK);
			float nx = cx + 3 + 4;
			ListCard.number(d, shownCount, oldCount, countAt, now, nx, cy, Tokens.TEXT);
			Type.draw(d, "online", nx + Type.width(count, cap, Weight.BOLD) + 2.5f, cy - cap / 2f, cap, Weight.MEDIUM, 0xFFF3D4E8);
			Paint.opacity(old);
		}
		d.disableScissor();
		return x1;
	}

	private void drawRow(Draw d, RowState state, float left, float top, boolean shadow, long now) {
		float in = state.in.get();
		if (in <= 0.01f) return;
		Staff staff = state.staff;
		float spect = state.spectating.get();
		float cy = top + state.slot.get() * ListCard.ROW + ListCard.ROW / 2f;
		float shift = state.leaving ? -(1 - in) * 14 : (1 - in) * 6;
		float old = Paint.opacity(Paint.opacity() * in);
		float glow = Math.max(0, 1 - (now - state.joinedAt) / 1600f);
		ListCard.arrival(d, left + 8 * ListCard.K + shift, left + ListCard.W - 8 * ListCard.K + shift, cy, glow, staff.color());

		float hx = ListCard.headX(left) + shift;
		float dim = 1 - 0.45f * spect;
		ListCard.head(d, staff.info(), staff.name(), hx, cy, staff.color(), dim, glow);
		if (spect > 0.01f) {
			float spectOld = Paint.opacity(Paint.opacity() * spect);
			ListCard.badge(d, hx, cy, "eye-off", Paint.fade(PINK, 0.85f));
			Paint.opacity(spectOld);
		}

		float tagOld = Paint.opacity(Paint.opacity() * dim);
		float tagLeft = ListCard.tag(d, staff.rank(), left + ListCard.W - ListCard.PAD + shift, cy, staff.color());
		Paint.opacity(tagOld);
		ListCard.name(d, staff.name(), ListCard.nameX(left) + shift, tagLeft, cy, Draw.mix(Tokens.TEXT, Tokens.TEXT_3, spect), shadow);
		Paint.opacity(old);
	}

	/** One staff member's animation state. */
	private static final class RowState {
		final Anim in = new Anim(0, 420, Ease.OUT);
		final Anim slot;
		final Anim spectating = new Anim(0, 420, Ease.OUT);
		long joinedAt = 0;
		boolean leaving = false;
		Staff staff;

		RowState(int index) {
			slot = new Anim(index, 380, Ease.OUT);
		}
	}
}
