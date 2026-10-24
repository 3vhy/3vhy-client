package dev.threevhy.client.ui;

import dev.threevhy.client.Draw;
import dev.threevhy.client.ui.Type.Weight;

/**
 * The 3vhy logo: a lilac-to-magenta 3 inside a tilted orbit ring, with a sparkle that twinkles now and
 * then, and the letter-spaced 3VHY wordmark. Drawn from vector paths (assets/threevhy/icons/brand.txt),
 * so it's sharp at any size; the same design is the app icon and the website's logo.
 */
public final class Logo {
	private static final Grad THREE = new Grad(0xFFEDE2FF, 0xFFB43DF5, 152);
	private static final Grad RING = Grad.across(0xFF6A2CFF, 0xFFE05CF0);
	/** The wordmark's letter spacing, as a share of its capital height. */
	private static final float TRACKING = 0.46f;

	private Logo() {
	}

	/** The mark in a {@code size} square at (x, y); {@code alpha} fades it, {@code nowMs} drives the sparkle. */
	public static void mark(Draw d, float x, float y, float size, float alpha, long nowMs) {
		float old = Paint.opacity(Paint.opacity() * alpha);
		Paint.glow(d, x + size * 0.52f, y + size * 0.52f, size * 0.26f, size * 0.55f, 0x66B51FF4);
		Paint.icon(d, "3vhy-ring-back", x, y, size, 0.55f, false, 0, RING.withAlpha(0.55f));
		Paint.icon(d, "3vhy-three", x, y, size, 1.35f, false, 0, THREE);
		Paint.icon(d, "3vhy-ring-front", x, y, size, 0.65f, false, 0, RING);
		float t = (nowMs % 5200) / 5200f;
		float twinkle = t < 0.12f ? t / 0.12f : t < 0.3f ? 1 - (t - 0.12f) / 0.18f : 0;
		if (twinkle > 0) Paint.glow(d, x + size * 0.817f, y + size * 0.175f, size * 0.04f, size * 0.22f, Paint.fade(0xFFEBA2FF, twinkle * 0.8f));
		Paint.icon(d, "3vhy-spark", x, y, size, 0.4f, true, 0xFFFFFFFF, null);
		Paint.opacity(old);
	}

	/** The 3VHY wordmark with capitals {@code cap} tall; returns its width. */
	public static float wordmark(Draw d, float x, float capTop, float cap, int color) {
		return Type.spaced(d, "3VHY", x, capTop, cap, TRACKING, Weight.BOLD, color);
	}

	public static float wordmarkWidth(float cap) {
		return Type.spacedWidth("3VHY", cap, TRACKING, Weight.BOLD);
	}

	/** The mark and the wordmark side by side, like the menu's header; returns the total width. */
	public static float lockup(Draw d, float x, float y, float markSize, float cap, long nowMs) {
		mark(d, x, y, markSize, 1, nowMs);
		float tx = x + markSize + markSize * 0.42f;
		return tx - x + wordmark(d, tx, y + (markSize - cap) / 2f, cap, Tokens.TEXT);
	}
}
