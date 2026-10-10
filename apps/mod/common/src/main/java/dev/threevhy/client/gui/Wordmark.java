package dev.threevhy.client.gui;

import dev.threevhy.client.Draw;

/** The pixel "3vhy" wordmark from the banner, drawn with filled rectangles (43 x 15 cells). */
public final class Wordmark {
	public static final int COLUMNS = 43;
	public static final int ROWS = 15;

	/** Horizontal runs of filled cells: {x, y, length}. */
	private static final int[][] RUNS = {
			{2, 0, 6}, {23, 0, 2}, {1, 1, 8}, {22, 1, 4}, {0, 2, 4}, {6, 2, 4}, {11, 2, 4}, {17, 2, 4},
			{22, 2, 4}, {33, 2, 4}, {39, 2, 4}, {0, 3, 3}, {6, 3, 4}, {11, 3, 4}, {17, 3, 4}, {22, 3, 4},
			{33, 3, 4}, {39, 3, 4}, {6, 4, 4}, {11, 4, 4}, {17, 4, 4}, {22, 4, 4}, {27, 4, 4}, {33, 4, 4},
			{39, 4, 4}, {3, 5, 6}, {11, 5, 4}, {17, 5, 4}, {22, 5, 10}, {33, 5, 4}, {39, 5, 4}, {3, 6, 7},
			{12, 6, 4}, {17, 6, 4}, {22, 6, 4}, {28, 6, 4}, {33, 6, 4}, {39, 6, 4}, {6, 7, 4}, {12, 7, 9},
			{22, 7, 4}, {28, 7, 4}, {33, 7, 5}, {39, 7, 4}, {0, 8, 3}, {6, 8, 4}, {13, 8, 8}, {22, 8, 4},
			{28, 8, 4}, {34, 8, 9}, {0, 9, 4}, {5, 9, 5}, {13, 9, 7}, {22, 9, 4}, {28, 9, 4}, {35, 9, 8},
			{1, 10, 9}, {14, 10, 6}, {22, 10, 4}, {28, 10, 4}, {39, 10, 4}, {2, 11, 6}, {15, 11, 4},
			{23, 11, 3}, {29, 11, 3}, {33, 11, 3}, {39, 11, 4}, {33, 12, 4}, {38, 12, 4}, {34, 13, 8},
			{35, 14, 5}
	};

	private Wordmark() {
	}

	/** Column where each of the four letters (3, v, h, y) starts; a run belongs to the last start at or before it. */
	private static final int[] LETTER_STARTS = {0, 11, 22, 33};

	/**
	 * Draws the wordmark with each letter dropping in after the one before it, like the launcher and
	 * the installer do. {@code elapsedMs} is the time since the screen opened.
	 */
	public static void drawDropping(Draw d, int x, int y, int cell, int color, int shadowColor, long elapsedMs) {
		for (int pass = 0; pass < 2; pass++) {
			if (pass == 0 && shadowColor == 0) continue;
			for (int[] run : RUNS) {
				int letter = 0;
				for (int i = 0; i < LETTER_STARTS.length; i++) {
					if (run[0] >= LETTER_STARTS[i]) letter = i;
				}
				double t = Math.max(0, Math.min(1, (elapsedMs - 100 - letter * 90) / 620.0));
				if (t <= 0) continue;
				int drop = (int) Math.round((1 - easeOutBack(t)) * -3 * cell);
				int alpha = (int) Math.round(Math.min(1, t * 2.5) * ((pass == 0 ? shadowColor : color) >>> 24));
				int argb = (alpha << 24) | ((pass == 0 ? shadowColor : color) & 0xFFFFFF);
				int top = y + run[1] * cell + drop + (pass == 0 ? cell : 0);
				d.fill(x + run[0] * cell, top, x + (run[0] + run[2]) * cell, top + cell, argb);
			}
		}
	}

	/** Just the "3", 10 x 12 cells: the app icon's glyph. */
	public static void drawGlyph(Draw d, int x, int y, int cell, int color) {
		for (int[] run : RUNS) {
			if (run[0] >= LETTER_STARTS[1]) continue;
			d.fill(x + run[0] * cell, y + run[1] * cell, x + (run[0] + run[2]) * cell, y + (run[1] + 1) * cell, color);
		}
	}

	private static double easeOutBack(double t) {
		double c1 = 1.70158;
		double c3 = c1 + 1;
		return 1 + c3 * Math.pow(t - 1, 3) + c1 * Math.pow(t - 1, 2);
	}

	/** Draws the wordmark with its top-left at (x, y); each cell is {@code cell} pixels. */
	public static void draw(Draw d, int x, int y, int cell, int color, int shadowColor) {
		if (shadowColor != 0) {
			for (int[] run : RUNS) {
				d.fill(x + run[0] * cell, y + run[1] * cell + cell, x + (run[0] + run[2]) * cell,
						y + (run[1] + 1) * cell + cell, shadowColor);
			}
		}
		for (int[] run : RUNS) {
			d.fill(x + run[0] * cell, y + run[1] * cell, x + (run[0] + run[2]) * cell, y + (run[1] + 1) * cell, color);
		}
	}
}
