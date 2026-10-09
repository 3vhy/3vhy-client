package dev.threevhy.client.gui;

import dev.threevhy.client.Draw;

/** Small pixel icons for 3vhy's menus, drawn from 7x7 bitmaps. */
public final class Icons {
	public static final int SIZE = 7;

	public static final String[] GEAR = {
			"...#...",
			".#####.",
			".##.##.",
			"##...##",
			".##.##.",
			".#####.",
			"...#...",
	};
	public static final String[] SEARCH = {
			".###...",
			"#...#..",
			"#...#..",
			"#...#..",
			".####..",
			"....##.",
			".....##",
	};
	public static final String[] BACK = {
			"...#...",
			"..#....",
			".#.....",
			"#######",
			".#.....",
			"..#....",
			"...#...",
	};
	public static final String[] CLOSE = {
			"#.....#",
			".#...#.",
			"..#.#..",
			"...#...",
			"..#.#..",
			".#...#.",
			"#.....#",
	};
	public static final String[] LAYOUT = {
			"###.###",
			"#.#.#.#",
			"###.###",
			".......",
			"###.###",
			"#.#.#.#",
			"###.###",
	};

	private Icons() {
	}

	public static void draw(Draw d, String[] icon, int x, int y, int color) {
		for (int row = 0; row < icon.length; row++) {
			String line = icon[row];
			int start = -1;
			for (int col = 0; col <= line.length(); col++) {
				boolean on = col < line.length() && line.charAt(col) == '#';
				if (on && start < 0) start = col;
				if (!on && start >= 0) {
					d.fill(x + start, y + row, x + col, y + row + 1, color);
					start = -1;
				}
			}
		}
	}
}
