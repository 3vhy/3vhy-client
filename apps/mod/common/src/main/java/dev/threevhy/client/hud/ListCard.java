package dev.threevhy.client.hud;

import dev.threevhy.client.Draw;
import dev.threevhy.client.gui.Accent;
import dev.threevhy.client.gui.menu.MenuScreen;
import dev.threevhy.client.ui.Ease;
import dev.threevhy.client.ui.Grad;
import dev.threevhy.client.ui.Paint;
import dev.threevhy.client.ui.Tokens;
import dev.threevhy.client.ui.Type;
import dev.threevhy.client.ui.Type.Weight;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.UUIDUtil;

/**
 * The card the Staff List and the Player List share, from the website's staff card: a glass card with
 * a violet icon tile and a title, a pill on the right of the header, a hairline, then a row per player
 * with their head ringed in a colour, their name and a small tag on the right. Sizes are the website
 * clip's pixels scaled by {@link #K}.
 */
final class ListCard {
	/** GUI pixels per pixel of the website's clip, where the icon tile is 54 pixels. */
	static final float K = 13 / 54f;
	static final int W = Math.round(445 * K);
	static final float HEADER = 100 * K;
	static final float ROW = 67 * K;
	static final float PAD = 18 * K;
	static final float RADIUS = 26 * K;
	/** The room between the hairline and the first row, and under the last one. */
	static final float LIST_PAD = 6 * K;
	static final float PILL_CAP = 4.2f;
	private static final float TILE = 54 * K;
	private static final float HEAD = 46 * K;
	private static final float PILL_H = 40 * K;
	private static final float TITLE_CAP = 22 * K;
	private static final float NAME_CAP = 20 * K;
	private static final float TAG_CAP = 3.1f;
	private static final float TAG_H = 32 * K;

	private ListCard() {
	}

	/** The card's height for {@code rows} rows plus {@code extra} below them (none at all when it's empty). */
	static float height(int rows, float extra) {
		return HEADER + (rows > 0 ? LIST_PAD * 2 + rows * ROW + extra : 0);
	}

	/** The icon tile in the theme's gradient and the title beside it, cut short before {@code maxRight}. */
	static void header(Draw d, float left, float y, String icon, String title, float maxRight, boolean shadow) {
		float tx = left + PAD;
		float cy = y + HEADER / 2f;
		float r = 12 * K;
		Paint.shadow(d, tx, cy - TILE / 2f, tx + TILE, cy + TILE / 2f, r, 0, 0, 10 * K, 0, Accent.glow(0.55f));
		Paint.rect(d, tx, cy - TILE / 2f, tx + TILE, cy + TILE / 2f, r, Accent.tile());
		Paint.stroke(d, tx, cy - TILE / 2f, tx + TILE, cy + TILE / 2f, r, Hud.hairline(d), Grad.down(0x4DFFFFFF, 0x00FFFFFF));
		float size = TILE * 0.56f;
		Paint.icon(d, icon, tx + (TILE - size) / 2f, cy - size / 2f, size, 0xFFFFFFFF);
		float x = tx + TILE + 21 * K;
		String fitted = Type.fit(title, maxRight - x - 4, TITLE_CAP, Weight.SEMIBOLD);
		Hud.text(d, fitted, x, cy - TITLE_CAP / 2f, TITLE_CAP, Weight.SEMIBOLD, Tokens.TEXT, shadow);
	}

	/** The hairline under the header, faded by {@code alpha}. */
	static void divider(Draw d, float left, float y, float alpha) {
		if (alpha > 0.01f) Paint.rect(d, left + PAD, y - Hud.hairline(d), left + W - PAD, y, 0, Paint.fade(0x14FFFFFF, alpha));
	}

	/** The header's pill, {@code w} wide with its right end at {@code right}; returns its left edge. */
	static float pill(Draw d, float right, float cy, float w, int fill, int stroke) {
		float x1 = right - w;
		float y1 = cy - PILL_H / 2f;
		float r = PILL_H / 2f;
		Paint.rect(d, x1, y1, right, y1 + PILL_H, r, fill);
		Paint.stroke(d, x1, y1, right, y1 + PILL_H, r, Math.max(Hud.hairline(d), 0.5f), stroke);
		return x1;
	}

	/** Clips drawing to the pill {@code w} wide ending at {@code right}, for numbers rolling inside it. */
	static void clipPill(Draw d, float right, float cy, float w) {
		d.enableScissor((int) Math.floor(right - w), (int) Math.floor(cy - PILL_H / 2f), (int) Math.ceil(right), (int) Math.ceil(cy + PILL_H / 2f));
	}

	/** A number that rolls up (or down) out of {@code previous} into {@code value} after it changes. */
	static void number(Draw d, int value, int previous, long changedAt, long now, float x, float cy, int color) {
		float roll = Ease.OUT.apply((now - changedAt) / 520.0);
		float dir = value >= previous ? 1 : -1;
		float capTop = cy - PILL_CAP / 2f;
		if (roll < 1) {
			float old = Paint.opacity(Paint.opacity() * (1 - roll));
			Type.draw(d, Integer.toString(previous), x, capTop - dir * 6 * roll, PILL_CAP, Weight.BOLD, color);
			Paint.opacity(old);
		}
		float old = Paint.opacity(Paint.opacity() * roll);
		Type.draw(d, Integer.toString(value), x, capTop + dir * 6 * (1 - roll), PILL_CAP, Weight.BOLD, color);
		Paint.opacity(old);
	}

	/** The soft highlight in {@code color} behind a row that just arrived, fading as {@code glow} goes to 0. */
	static void arrival(Draw d, float x1, float x2, float cy, float glow, int color) {
		if (glow <= 0.01f) return;
		float e = glow * glow;
		float y1 = cy - ROW / 2f + 2 * K;
		float y2 = cy + ROW / 2f - 2 * K;
		float r = ROW / 2f;
		Paint.shadow(d, x1, y1, x2, y2, r, 0, 0, 14 * K, 0, Paint.fade(color, 0.35f * e));
		Paint.rect(d, x1, y1, x2, y2, r, Paint.fade(color, 0.16f * e));
		Paint.stroke(d, x1, y1, x2, y2, r, Math.max(Hud.hairline(d), 0.5f), Paint.fade(color, 0.5f * e));
	}

	/** Where a row's head starts. */
	static float headX(float left) {
		return left + PAD;
	}

	/** Where a row's name starts. */
	static float nameX(float left) {
		return left + PAD + HEAD + 21 * K;
	}

	/**
	 * A player's head ringed in {@code color}, at {@code dim} opacity, with a ring spreading out from it
	 * while {@code glow} (1 to 0) runs down after they arrive.
	 */
	static void head(Draw d, PlayerInfo info, String name, float x, float cy, int color, float dim, float glow) {
		float y = cy - HEAD / 2f;
		float r = 9 * K;
		float old = Paint.opacity(Paint.opacity() * dim);
		MenuScreen.face(d, info != null ? info.getSkin() : DefaultPlayerSkin.get(UUIDUtil.createOfflinePlayerUUID(name)), x, y, HEAD, r);
		Paint.stroke(d, x - 0.6f, y - 0.6f, x + HEAD + 0.6f, y + HEAD + 0.6f, r + 0.6f, 0.8f, Paint.fade(color, 0.85f));
		if (glow > 0.01f) {
			float g = Ease.OUT.apply(1 - glow) * 2.5f;
			Paint.stroke(d, x - 1 - g, y - 1 - g, x + HEAD + 1 + g, y + HEAD + 1 + g, r + 1 + g, 0.7f, Paint.fade(color, 0.6f * glow));
		}
		Paint.opacity(old);
	}

	/** A small round badge with {@code icon} on the head's lower right corner. */
	static void badge(Draw d, float x, float cy, String icon, int color) {
		float br = HEAD * 0.3f;
		float bcx = x + HEAD - br * 0.4f;
		float bcy = cy + HEAD / 2f - br * 0.4f;
		Paint.circle(d, bcx, bcy, br + 0.6f, Tokens.GLASS_SOLID);
		Paint.circle(d, bcx, bcy, br, color);
		Paint.icon(d, icon, bcx - br * 0.62f, bcy - br * 0.62f, br * 1.24f, 0xFFFFFFFF);
	}

	static float tagWidth(String text) {
		return Type.spacedWidth(text, TAG_CAP, 0.08f, Weight.BOLD) + 24 * K;
	}

	/** A small spaced-out uppercase tag in {@code color}, its right end at {@code right}; returns its left edge. */
	static float tag(Draw d, String text, float right, float cy, int color) {
		float x1 = right - tagWidth(text);
		Paint.rect(d, x1, cy - TAG_H / 2f, right, cy + TAG_H / 2f, TAG_H / 2f, Paint.fade(color, 0.14f));
		Paint.stroke(d, x1, cy - TAG_H / 2f, right, cy + TAG_H / 2f, TAG_H / 2f, Math.max(Hud.hairline(d), 0.5f), Paint.fade(color, 0.6f));
		Type.spaced(d, text, x1 + 12 * K, cy - TAG_CAP / 2f, TAG_CAP, 0.08f, Weight.BOLD, color);
		return x1;
	}

	/** A player's name from {@code x}, cut short before {@code right}. */
	static void name(Draw d, String name, float x, float right, float cy, int color, boolean shadow) {
		String fitted = Type.fit(name, right - x - 4, NAME_CAP, Weight.SEMIBOLD);
		Hud.text(d, fitted, x, cy - NAME_CAP / 2f, NAME_CAP, Weight.SEMIBOLD, color, shadow);
	}
}
