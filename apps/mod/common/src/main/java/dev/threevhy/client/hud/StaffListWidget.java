package dev.threevhy.client.hud;

import dev.threevhy.client.Draw;
import dev.threevhy.client.Hooks;
import dev.threevhy.client.gui.Accent;
import dev.threevhy.client.gui.menu.MenuScreen;
import dev.threevhy.client.module.Modules;
import dev.threevhy.client.module.StaffList;
import dev.threevhy.client.module.StaffList.Staff;
import dev.threevhy.client.ui.Anim;
import dev.threevhy.client.ui.Ease;
import dev.threevhy.client.ui.Grad;
import dev.threevhy.client.ui.Paint;
import dev.threevhy.client.ui.Tokens;
import dev.threevhy.client.ui.Type;
import dev.threevhy.client.ui.Type.Weight;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.UUIDUtil;

/**
 * The Staff List, like the website's: a glass card with a violet shield tile and "Staff", and a pill
 * that says "All clear" in mint while no staff are on, or "N online" in pink with the number rolling as
 * it changes. Under it, each staff member's head ringed in their rank's colour, their name and a rank
 * pill. Someone joining arrives with a glow in their rank's colour, someone spectating dims with an
 * eye-off badge on their head, and someone leaving slides away as the card shrinks.
 */
final class StaffListWidget extends HudWidget {
	/** GUI pixels per pixel of the website's clip, where the shield tile is 54 pixels. */
	private static final float K = 13 / 54f;
	private static final int W = Math.round(445 * K);
	private static final float HEADER = 100 * K;
	private static final float ROW = 67 * K;
	private static final float PAD = 18 * K;
	private static final float TILE = 54 * K;
	private static final float HEAD = 46 * K;
	private static final float PILL_H = 40 * K;
	private static final float TITLE_CAP = 22 * K;
	private static final float NAME_CAP = 20 * K;
	private static final float PILL_CAP = 4.2f;
	private static final float RANK_CAP = 3.1f;
	private static final int MINT = Tokens.MINT;
	private static final int PINK = 0xFFFF6EB4;
	private static final Staff[] SAMPLE = {new Staff("Alex", "ADMIN", StaffList.colorOf("admin"), null, false),
			new Staff("Kai", "MOD", StaffList.colorOf("mod"), null, false)};

	private final Map<String, RowState> rows = new LinkedHashMap<>();
	private final Anim appear = new Anim(0, 420, Ease.OUT);
	private final Anim height = new Anim(HEADER, 420, Ease.OUT);
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
		return W;
	}

	private static float targetHeight(int count) {
		return HEADER + (count > 0 ? count * ROW + 12 * K : 0);
	}

	@Override
	public int height(Minecraft mc) {
		if (!primed) return (int) Math.ceil(targetHeight(staff(mc, true).size()));
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
		height.set(targetHeight(list.size()));
		busy.set(shownCount > 0);
		appear.set(onServer(mc) || !list.isEmpty() || preview);
		if (quiet) {
			height.snap(targetHeight(list.size()));
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
		glass(d, left, y, W, h, 26 * K);

		float tx = left + PAD;
		float cy = y + HEADER / 2f;
		Paint.shadow(d, tx, cy - TILE / 2f, tx + TILE, cy + TILE / 2f, 12 * K, 0, 0, 10 * K, 0, Accent.glow(0.55f));
		Paint.rect(d, tx, cy - TILE / 2f, tx + TILE, cy + TILE / 2f, 12 * K, Accent.tile());
		Paint.stroke(d, tx, cy - TILE / 2f, tx + TILE, cy + TILE / 2f, 12 * K, Hud.hairline(d), Grad.down(0x4DFFFFFF, 0x00FFFFFF));
		float icon = TILE * 0.56f;
		Paint.icon(d, "shield-user", tx + (TILE - icon) / 2f, cy - icon / 2f, icon, 0xFFFFFFFF);
		Hud.text(d, "Staff", tx + TILE + 21 * K, cy - TITLE_CAP / 2f, TITLE_CAP, Weight.SEMIBOLD, Tokens.TEXT, shadow);
		pill(d, left + W - PAD, cy, now);

		float listTop = y + HEADER;
		if (!rows.isEmpty()) {
			float line = Math.min(1, (h - HEADER) / (ROW * 0.5f));
			if (line > 0.01f) Paint.rect(d, left + PAD, listTop - Hud.hairline(d), left + W - PAD, listTop, 0, Paint.fade(0x14FFFFFF, line));
			d.enableScissor((int) Math.floor(left), (int) Math.floor(listTop), (int) Math.ceil(left + W), (int) Math.ceil(y + h));
			for (RowState state : rows.values()) drawRow(d, state, left, listTop + 6 * K, shadow, now);
			d.disableScissor();
		}
		Paint.opacity(old);
	}

	/** "All clear" in mint, or "N online" in pink with the number rolling up or down as it changes. */
	private void pill(Draw d, float right, float cy, long now) {
		float b = busy.get();
		String count = Integer.toString(shownCount);
		float onlineW = 12 * K + 3 + 4 + Type.width(count, PILL_CAP, Weight.BOLD) + 2.5f + Type.width("online", PILL_CAP, Weight.MEDIUM) + 14 * K;
		float clearW = 12 * K + 6 + 3.5f + Type.width("All clear", PILL_CAP, Weight.SEMIBOLD) + 16 * K;
		float w = clearW + (onlineW - clearW) * b;
		float x1 = right - w;
		float y1 = cy - PILL_H / 2f;
		float r = PILL_H / 2f;
		int stroke = Draw.mix(Paint.fade(MINT, 0.45f), Paint.fade(PINK, 0.55f), b);
		int fill = Draw.mix(Paint.fade(MINT, 0.12f), Paint.fade(PINK, 0.12f), b);
		Paint.rect(d, x1, y1, right, y1 + PILL_H, r, fill);
		Paint.stroke(d, x1, y1, right, y1 + PILL_H, r, Math.max(Hud.hairline(d), 0.5f), stroke);
		d.enableScissor((int) Math.floor(x1), (int) Math.floor(y1), (int) Math.ceil(right), (int) Math.ceil(y1 + PILL_H));
		if (b < 0.99f) {
			float old = Paint.opacity(Paint.opacity() * (1 - b));
			float cx = x1 + 12 * K;
			Paint.icon(d, "check", cx, cy - 3, 6, MINT);
			Type.draw(d, "All clear", cx + 6 + 3.5f, cy - PILL_CAP / 2f, PILL_CAP, Weight.SEMIBOLD, MINT);
			Paint.opacity(old);
		}
		if (b > 0.01f) {
			float old = Paint.opacity(Paint.opacity() * b);
			float cx = x1 + 12 * K;
			Paint.glow(d, cx + 1.5f, cy, 1.5f, 4, Paint.fade(PINK, 0.5f));
			Paint.circle(d, cx + 1.5f, cy, 1.5f, PINK);
			float nx = cx + 3 + 4;
			float roll = Ease.OUT.apply((now - countAt) / 520.0);
			float dir = shownCount >= oldCount ? 1 : -1;
			if (roll < 1) {
				float out = Paint.opacity(Paint.opacity() * (1 - roll));
				Type.draw(d, Integer.toString(oldCount), nx, cy - PILL_CAP / 2f - dir * 6 * roll, PILL_CAP, Weight.BOLD, Tokens.TEXT);
				Paint.opacity(out);
			}
			float in = Paint.opacity(Paint.opacity() * roll);
			Type.draw(d, count, nx, cy - PILL_CAP / 2f + dir * 6 * (1 - roll), PILL_CAP, Weight.BOLD, Tokens.TEXT);
			Paint.opacity(in);
			Type.draw(d, "online", nx + Type.width(count, PILL_CAP, Weight.BOLD) + 2.5f, cy - PILL_CAP / 2f, PILL_CAP, Weight.MEDIUM, 0xFFF3D4E8);
			Paint.opacity(old);
		}
		d.disableScissor();
	}

	private void drawRow(Draw d, RowState state, float left, float top, boolean shadow, long now) {
		float in = state.in.get();
		if (in <= 0.01f) return;
		Staff staff = state.staff;
		float spect = state.spectating.get();
		float cy = top + state.slot.get() * ROW + ROW / 2f;
		float shift = state.leaving ? -(1 - in) * 14 : (1 - in) * 6;
		float old = Paint.opacity(Paint.opacity() * in);
		float rx1 = left + 8 * K + shift;
		float rx2 = left + W - 8 * K + shift;
		float glow = Math.max(0, 1 - (now - state.joinedAt) / 1600f);
		if (glow > 0.01f) {
			float e = Ease.OUT.apply(glow);
			Paint.shadow(d, rx1, cy - ROW / 2f + 2 * K, rx2, cy + ROW / 2f - 2 * K, ROW / 2f, 0, 0, 14 * K, 0, Paint.fade(staff.color(), 0.35f * e));
			Paint.rect(d, rx1, cy - ROW / 2f + 2 * K, rx2, cy + ROW / 2f - 2 * K, ROW / 2f, Paint.fade(staff.color(), 0.16f * e));
			Paint.stroke(d, rx1, cy - ROW / 2f + 2 * K, rx2, cy + ROW / 2f - 2 * K, ROW / 2f, Math.max(Hud.hairline(d), 0.5f),
					Paint.fade(staff.color(), 0.5f * e));
		}

		float hx = left + PAD + shift;
		float hy = cy - HEAD / 2f;
		float dim = 1 - 0.45f * spect;
		float faceOld = Paint.opacity(Paint.opacity() * dim);
		MenuScreen.face(d, staff.info() != null ? staff.info().getSkin() : DefaultPlayerSkin.get(UUIDUtil.createOfflinePlayerUUID(staff.name())), hx,
				hy, HEAD, 9 * K);
		Paint.stroke(d, hx - 0.6f, hy - 0.6f, hx + HEAD + 0.6f, hy + HEAD + 0.6f, 9 * K + 0.6f, 0.8f, Paint.fade(staff.color(), 0.85f));
		if (glow > 0.01f) {
			float ring = Ease.OUT.apply(1 - glow);
			Paint.stroke(d, hx - 1 - ring * 2.5f, hy - 1 - ring * 2.5f, hx + HEAD + 1 + ring * 2.5f, hy + HEAD + 1 + ring * 2.5f, 9 * K + 1 + ring * 2.5f,
					0.7f, Paint.fade(staff.color(), 0.6f * glow));
		}
		Paint.opacity(faceOld);
		if (spect > 0.01f) {
			float spectOld = Paint.opacity(Paint.opacity() * spect);
			float br = HEAD * 0.3f;
			float bcx = hx + HEAD - br * 0.4f;
			float bcy = hy + HEAD - br * 0.4f;
			Paint.circle(d, bcx, bcy, br + 0.6f, Tokens.GLASS_SOLID);
			Paint.circle(d, bcx, bcy, br, Paint.fade(PINK, 0.85f));
			Paint.icon(d, "eye-off", bcx - br * 0.62f, bcy - br * 0.62f, br * 1.24f, 0xFFFFFFFF);
			Paint.opacity(spectOld);
		}

		float rankW = Type.spacedWidth(staff.rank(), RANK_CAP, 0.08f, Weight.BOLD) + 12 * K * 2;
		float rankH = 32 * K;
		float rkx2 = left + W - PAD + shift;
		float rkx1 = rkx2 - rankW;
		float rankOld = Paint.opacity(Paint.opacity() * dim);
		Paint.rect(d, rkx1, cy - rankH / 2f, rkx2, cy + rankH / 2f, rankH / 2f, Paint.fade(staff.color(), 0.14f));
		Paint.stroke(d, rkx1, cy - rankH / 2f, rkx2, cy + rankH / 2f, rankH / 2f, Math.max(Hud.hairline(d), 0.5f), Paint.fade(staff.color(), 0.6f));
		Type.spaced(d, staff.rank(), rkx1 + 12 * K, cy - RANK_CAP / 2f, RANK_CAP, 0.08f, Weight.BOLD, staff.color());
		Paint.opacity(rankOld);

		float nx = hx + HEAD + 21 * K;
		String name = Type.fit(staff.name(), rkx1 - nx - 4, NAME_CAP, Weight.SEMIBOLD);
		Hud.text(d, name, nx, cy - NAME_CAP / 2f, NAME_CAP, Weight.SEMIBOLD, Draw.mix(Tokens.TEXT, Tokens.TEXT_3, spect), shadow);
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
