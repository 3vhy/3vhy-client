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
