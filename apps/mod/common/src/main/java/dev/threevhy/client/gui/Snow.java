package dev.threevhy.client.gui;

import dev.threevhy.client.Draw;
import dev.threevhy.client.module.Modules;
import java.util.Random;

/**
 * Pixel snow, the same square flakes as the launcher's backdrop, falling with a gentle sway.
 * Flakes are kept as fractions of the screen so a resize doesn't bunch them up.
 */
public final class Snow {
	private static final int MAX_FLAKES = 220;
	private final float[] x = new float[MAX_FLAKES];
	private final float[] y = new float[MAX_FLAKES];
	private final float[] speed = new float[MAX_FLAKES];
	private final float[] sway = new float[MAX_FLAKES];
	private final int[] size = new int[MAX_FLAKES];
	private final int[] alpha = new int[MAX_FLAKES];
	private final Random random = new Random();
	private long lastFrame = 0;

	public Snow() {
		for (int i = 0; i < MAX_FLAKES; i++) {
			spawn(i, random.nextFloat());
		}
	}

	private void spawn(int i, float startY) {
		x[i] = random.nextFloat();
		y[i] = startY;
		speed[i] = 0.035f + random.nextFloat() * 0.07f;
		sway[i] = random.nextFloat() * 6.28f;
		size[i] = random.nextFloat() < 0.3f ? 2 : 1;
		alpha[i] = 70 + random.nextInt(140);
	}

	/** Moves and draws the flakes over the whole screen. Does nothing if the player turned snow off. */
	public void draw(Draw d, int width, int height) {
		long now = System.nanoTime();
		float seconds = lastFrame == 0 ? 0 : Math.min(0.1f, (now - lastFrame) / 1_000_000_000f);
		lastFrame = now;
		if (!Modules.THEME_SNOW.get() || width <= 0 || height <= 0) return;

		int count = Math.min(MAX_FLAKES, (int) (MAX_FLAKES * Modules.THEME_SNOW_AMOUNT.get() / 100.0));
		int color = MenuTheme.current().snow();
		double time = now / 1_000_000_000.0;
		for (int i = 0; i < count; i++) {
			y[i] += speed[i] * seconds * (size[i] == 2 ? 1.25f : 1f);
			if (y[i] > 1.02f) spawn(i, -0.02f);
			int px = Math.round(x[i] * width + (float) Math.sin(time * 0.8 + sway[i]) * 6);
			int py = Math.round(y[i] * height);
			int s = size[i];
			d.fill(px, py, px + s, py + s, MenuTheme.alpha(color, alpha[i]));
		}
	}
}
