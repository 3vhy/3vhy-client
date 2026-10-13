package dev.threevhy.client.gui;

import dev.threevhy.client.Draw;
import dev.threevhy.client.module.Modules;
import java.time.LocalDate;
import java.util.Random;

/**
 * The October main menu: a pale moonlit sky, white pixel ghosts bobbing about, a few bats by the
 * moon, drifting mist, and a dark graveyard hill along the bottom with tombstones and a glowing
 * pumpkin (it keeps Minecraft's version and copyright text readable). It's on by itself in
 * October, or always / never with the Main Menu's Season setting.
 */
public final class October {
	public static final int INK = 0xFF2A2238;
	public static final int INK_SOFT = 0xFF6B6280;
	public static final int PUMPKIN = 0xFFFF8A2B;

	private static final int SKY_TOP = 0xFFFFFFFF;
	private static final int SKY_MID = 0xFFF1EEF7;
	private static final int SKY_LOW = 0xFFDCD5EA;
	private static final int HILL_BACK = 0xFFB9B0CE;
	private static final int HILL_FRONT = 0xFF2A2238;
	private static final int GHOST = 0xFFFFFFFF;
	private static final int GHOST_SHADE = 0xFFE3DDEF;
	private static final int GHOST_SHADOW = 0x337A6E98;
	private static final int BLUSH = 0xFFF7B9CB;

	/** A ghost, 12 by 14 cells: # body, o eyes and mouth, b blush, . empty. */
	private static final String[] GHOST_SPRITE = {
			"....####....",
			"..########..",
			".##########.",
			".##########.",
			"############",
			"##oo####oo##",
			"##oo####oo##",
			"#b########b#",
			"#####oo#####",
			"############",
			"############",
			"############",
			"##.###.###.#",
			"#...#...#...",
	};

	private static final String[] BAT_UP = {
			"#.........#",
			"##.......##",
			"###.#.#.###",
			".#########.",
			"...#####...",
	};

	private static final String[] BAT_DOWN = {
			"....#.#....",
			"..#######..",
			".#########.",
			"##.#####.##",
			"#.........#",
	};

	private static final int GHOSTS = 6;
	private static final float[] GHOST_X = new float[GHOSTS];
	private static final float[] GHOST_Y = new float[GHOSTS];
	private static final float[] GHOST_SPEED = new float[GHOSTS];
	private static final float[] GHOST_PHASE = new float[GHOSTS];
	private static final int[] GHOST_CELL = new int[GHOSTS];
	private static final int[] GHOST_ALPHA = new int[GHOSTS];
	private static long lastFrame = 0;

	static {
		Random random = new Random(1031);
		for (int i = 0; i < GHOSTS; i++) {
			GHOST_X[i] = (i + random.nextFloat() * 0.6f) / GHOSTS;
			GHOST_Y[i] = 0.12f + random.nextFloat() * 0.5f;
			GHOST_SPEED[i] = (0.006f + random.nextFloat() * 0.01f) * (random.nextBoolean() ? 1 : -1);
			GHOST_PHASE[i] = random.nextFloat() * 6.28f;
			GHOST_CELL[i] = i % 3 == 0 ? 3 : i % 3 == 1 ? 2 : 1;
			GHOST_ALPHA[i] = GHOST_CELL[i] == 1 ? 150 : GHOST_CELL[i] == 2 ? 215 : 255;
		}
	}

	private October() {
	}

	/** Whether the main menu wears its October look right now. */
	public static boolean active() {
		if (Modules.MAIN_MENU_SEASON.is("Always")) return true;
		if (Modules.MAIN_MENU_SEASON.is("Never")) return false;
		return LocalDate.now().getMonthValue() == 10;
	}

	/** The whole October backdrop, behind the title screen's emblem and buttons. */
	public static void drawBackdrop(Draw d, int w, int h) {
		long now = System.nanoTime();
		float seconds = lastFrame == 0 ? 0 : Math.min(0.1f, (now - lastFrame) / 1_000_000_000f);
		lastFrame = now;
		double time = now / 1_000_000_000.0;

		sky(d, w, h);
		moon(d, w, h);
		bats(d, w, h, time);
		for (int i = GHOSTS - 1; i >= 0; i--) {
			if (GHOST_CELL[i] == 1) ghost(d, w, h, i, seconds, time);
		}
		hills(d, w, h, time);
		for (int i = GHOSTS - 1; i >= 0; i--) {
			if (GHOST_CELL[i] > 1) ghost(d, w, h, i, seconds, time);
		}
	}

	private static void sky(Draw d, int w, int h) {
		int bands = Math.max(1, h / 2);
		for (int i = 0; i < bands; i++) {
			int y1 = i * h / bands;
			int y2 = (i + 1) * h / bands;
			double t = i / (double) bands;
			int color = t < 0.45 ? Draw.mix(SKY_TOP, SKY_MID, t / 0.45) : Draw.mix(SKY_MID, SKY_LOW, (t - 0.45) / 0.55);
			d.fill(0, y1, w, y2, color);
		}
	}

	/** A big pale moon with a soft lilac ring and a few craters, up and to the right. */
	private static void moon(Draw d, int w, int h) {
		int unit = Math.max(1, h / 120);
		int r = 11 * unit;
		int cx = w - Math.max(40, w / 7);
		int cy = Math.max(r + 8, h / 7);
		disc(d, cx, cy, r + 5 * unit, 0x14857AA6);
		disc(d, cx, cy, r + 2 * unit, 0x22857AA6);
		disc(d, cx, cy, r, 0xFFFBF8FF);
		disc(d, cx - 4 * unit, cy - 3 * unit, 3 * unit, 0xFFECE6F4);
		disc(d, cx + 4 * unit, cy + 3 * unit, 2 * unit, 0xFFECE6F4);
		disc(d, cx + 3 * unit, cy - 5 * unit, unit, 0xFFECE6F4);
		disc(d, cx - 2 * unit, cy + 6 * unit, unit, 0xFFECE6F4);
	}

	private static void disc(Draw d, int cx, int cy, int r, int color) {
		for (int dy = -r; dy < r; dy++) {
			double y = dy + 0.5;
			int half = (int) Math.round(Math.sqrt(Math.max(0, r * r - y * y)));
			if (half > 0) d.fill(cx - half, cy + dy, cx + half, cy + dy + 1, color);
		}
	}

	/** Three little bats looping round the moon, flapping. */
	private static void bats(Draw d, int w, int h, double time) {
		int unit = Math.max(1, h / 240);
		int cx = w - Math.max(40, w / 7);
		int cy = Math.max(20, h / 7);
		for (int i = 0; i < 3; i++) {
			double angle = time * (0.35 + i * 0.08) + i * 2.1;
			int x = cx + (int) (Math.cos(angle) * (30 + i * 9) * unit * 1.6);
			int y = cy + (int) (Math.sin(angle * 1.3) * (12 + i * 4) * unit);
			boolean up = ((int) (time * 6 + i) & 1) == 0;
			sprite(d, up ? BAT_UP : BAT_DOWN, x, y, unit, INK);
		}
	}

	private static void ghost(Draw d, int w, int h, int i, float seconds, double time) {
		int cell = Math.max(1, GHOST_CELL[i] * Math.max(1, h / 240));
		int gw = 12 * cell;
		GHOST_X[i] += GHOST_SPEED[i] * seconds;
		if (GHOST_X[i] > 1.1f) GHOST_X[i] = -0.1f;
		if (GHOST_X[i] < -0.1f) GHOST_X[i] = 1.1f;
		int x = Math.round(GHOST_X[i] * (w + gw)) - gw;
		int bob = (int) Math.round(Math.sin(time * 1.4 + GHOST_PHASE[i]) * 3 * cell);
		int y = Math.round(GHOST_Y[i] * h) + bob;
		int alpha = GHOST_ALPHA[i];
		sprite(d, GHOST_SPRITE, x + cell, y + cell, cell, MenuTheme.alpha(GHOST_SHADOW, (GHOST_SHADOW >>> 24) * alpha / 255));
		boolean facingLeft = GHOST_SPEED[i] < 0;
		for (int row = 0; row < GHOST_SPRITE.length; row++) {
			String line = GHOST_SPRITE[row];
			for (int col = 0; col < line.length(); col++) {
				char c = line.charAt(facingLeft ? line.length() - 1 - col : col);
				if (c == '.') continue;
				int color = switch (c) {
					case 'o' -> INK;
					case 'b' -> BLUSH;
					default -> col >= line.length() - 2 || row >= GHOST_SPRITE.length - 3 ? GHOST_SHADE : GHOST;
				};
				int px = x + col * cell;
				int py = y + row * cell;
				d.fill(px, py, px + cell, py + cell, MenuTheme.alpha(color, alpha));
			}
		}
	}

	/** The misty back hill with tombstones, drifting mist, then the dark front hill and its pumpkins. */
	private static void hills(Draw d, int w, int h, double time) {
		int unit = Math.max(1, h / 240);
		int[][] back = {{0, 30}, {8, 34}, {19, 29}, {30, 35}, {42, 28}, {54, 33}, {65, 27}, {77, 32}, {88, 29}};
		int[][] front = {{0, 17}, {11, 20}, {24, 16}, {36, 19}, {49, 16}, {61, 21}, {74, 17}, {87, 19}};
		steps(d, w, h, back, unit, HILL_BACK);
		int[][] stones = {{6, 0}, {14, 1}, {33, 0}, {47, 1}, {58, 0}, {71, 1}, {83, 0}, {94, 1}};
		for (int[] stone : stones) {
			int x = stone[0] * w / 100;
			int top = h - heightAt(back, stone[0]) * unit;
			tombstone(d, x, top, unit * 2, stone[1] == 1);
		}
		for (int band = 0; band < 3; band++) {
			int y = h - (24 + band * 7) * unit;
			int drift = (int) ((time * (6 + band * 3)) % (w / 2.0));
			for (int x = -w / 2 + drift; x < w; x += w / 2) {
				d.fill(x, y, x + w / 3, y + 2 * unit, 0x30FFFFFF);
				d.fill(x + w / 12, y - unit, x + w / 4, y, 0x20FFFFFF);
			}
		}
		steps(d, w, h, front, unit, HILL_FRONT);
		int[] pumpkins = {18, 67};
		for (int i = 0; i < pumpkins.length; i++) {
			int x = pumpkins[i] * w / 100;
			int top = h - heightAt(front, pumpkins[i]) * unit;
			pumpkin(d, x, top, unit * 2, time + i);
		}
	}

	private static int heightAt(int[][] steps, int percent) {
		int height = steps[0][1];
		for (int[] step : steps) {
			if (step[0] <= percent) height = step[1];
		}
		return height;
	}

	private static void steps(Draw d, int w, int h, int[][] steps, int unit, int color) {
		for (int i = 0; i < steps.length; i++) {
			int x1 = steps[i][0] * w / 100;
			int x2 = i + 1 < steps.length ? steps[i + 1][0] * w / 100 : w;
			d.fill(x1, h - steps[i][1] * unit, x2, h, color);
		}
	}

	/** A small tombstone standing on {@code top}: rounded, or with a cross. */
	private static void tombstone(Draw d, int x, int top, int cell, boolean cross) {
		int color = 0xFF8E84A6;
		if (cross) {
			d.fill(x + cell, top - 5 * cell, x + 2 * cell, top, color);
			d.fill(x, top - 4 * cell, x + 3 * cell, top - 3 * cell, color);
		} else {
			d.fill(x + cell, top - 5 * cell, x + 3 * cell, top - 4 * cell, color);
			d.fill(x, top - 4 * cell, x + 4 * cell, top, color);
			d.fill(x + cell, top - 3 * cell, x + 3 * cell, top - 3 * cell + Math.max(1, cell / 2), 0xFF7A7092);
		}
	}

	/** A pumpkin sitting on {@code top}, its face flickering like there's a candle inside. */
	private static void pumpkin(Draw d, int x, int top, int cell, double time) {
		int y = top - 5 * cell;
		d.fill(x + 2 * cell, y - cell, x + 3 * cell, y, 0xFF4E7A3A);
		d.fill(x + cell, y, x + 5 * cell, y + 5 * cell, PUMPKIN);
		d.fill(x, y + cell, x + 6 * cell, y + 4 * cell, PUMPKIN);
		d.fill(x + 2 * cell, y + cell, x + 3 * cell, y + 4 * cell, 0xFFE8731B);
		int glow = 0xFFFFD45C;
		if (Math.sin(time * 9) > 0.6) glow = 0xFFFFE9A3;
		d.fill(x + cell, y + 2 * cell, x + 2 * cell, y + 3 * cell, glow);
		d.fill(x + 4 * cell, y + 2 * cell, x + 5 * cell, y + 3 * cell, glow);
		d.fill(x + 2 * cell, y + 3 * cell + cell / 2, x + 4 * cell, y + 4 * cell, glow);
	}

	private static void sprite(Draw d, String[] rows, int x, int y, int cell, int color) {
		for (int row = 0; row < rows.length; row++) {
			String line = rows[row];
			for (int col = 0; col < line.length(); col++) {
				if (line.charAt(col) == '.') continue;
				d.fill(x + col * cell, y + row * cell, x + (col + 1) * cell, y + (row + 1) * cell, color);
			}
		}
	}
}
