package dev.eventide.client.ui;

import dev.eventide.client.Draw;

/**
 * What's behind Eventide's menus: a flat near-black over the blurred game (or the ground colour itself
 * when there's no world yet), so the menu's surfaces read by lightness alone, and, if Theme's Stars is
 * on, a field of small stars that twinkle at their own pace. Star positions are fixed, so they never jump
 * between frames.
 */
public final class Sky {
	private static final int MAX_STARS = 260;
	private static final float[] STARS = new float[MAX_STARS * 6];

	static {
		long seed = 7;
		for (int i = 0; i < MAX_STARS; i++) {
			seed = next(seed);
			STARS[i * 6] = unit(seed);
			seed = next(seed);
			STARS[i * 6 + 1] = unit(seed);
			seed = next(seed);
			float size = unit(seed);
			STARS[i * 6 + 2] = 0.35f + size * size * size * size * 1.35f;
			seed = next(seed);
			STARS[i * 6 + 3] = 0.22f + unit(seed) * 0.62f;
			seed = next(seed);
			STARS[i * 6 + 4] = 0.4f + unit(seed) * 1.8f;
			seed = next(seed);
			STARS[i * 6 + 5] = unit(seed) * 6.2832f;
		}
	}

	private Sky() {
	}

	private static long next(long seed) {
		return seed * 6364136223846793005L + 1442695040888963407L;
	}

	private static float unit(long seed) {
		return ((seed >>> 40) & 0xFFFFFF) / (float) 0x1000000;
	}

	/**
	 * Covers a {@code w} x {@code h} screen: over a world, a near-black scrim that lets the blurred game show
	 * through faintly; without one, the ground colour. {@code fade} (0..1) fades it in.
	 */
	public static void backdrop(Draw d, float w, float h, boolean world, float fade) {
		float old = Paint.opacity(Paint.opacity() * fade);
		Paint.rect(d, 0, 0, w, h, 0, world ? Tokens.SCRIM : Tokens.BG_0);
		Paint.opacity(old);
	}

	/** Twinkling stars over a {@code w} x {@code h} area; {@code amount} is 0..1 of the most there can be. */
	public static void stars(Draw d, float w, float h, float amount, float scale, long nowMs) {
		int count = Math.round(MAX_STARS * Math.max(0, Math.min(1, amount)) * Math.min(1f, w * h / (1600f * 900f) * 1.4f + 0.3f));
		float t = nowMs / 1000f;
		for (int i = 0; i < count; i++) {
			float x = STARS[i * 6] * w;
			float y = STARS[i * 6 + 1] * h;
			float r = STARS[i * 6 + 2] * scale;
			float twinkle = 0.62f + 0.38f * (float) Math.sin(t * STARS[i * 6 + 4] + STARS[i * 6 + 5]);
			int alpha = Math.round(255 * STARS[i * 6 + 3] * twinkle);
			Paint.circle(d, x, y, Math.max(0.3f, r), Math.round(alpha * 0.7f) << 24 | 0xFFFFFF);
		}
	}
}
