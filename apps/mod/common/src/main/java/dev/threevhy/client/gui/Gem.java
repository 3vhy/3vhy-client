package dev.threevhy.client.gui;

import dev.threevhy.client.Draw;
import dev.threevhy.client.hud.Theme;
import net.minecraft.client.gui.components.AbstractWidget;

/**
 * The little cyan gem at the right end of 3vhy's Mods buttons on the main and pause menus, like
 * the one on Lunar's: 8 by 8 GUI pixels in 3vhy's ice, cyan and blue with a dark edge.
 */
public final class Gem {
	public static final int SIZE = 8;
	/** # edge, h highlight, c cyan, s shade. */
	private static final String[] SPRITE = {
			"..####..",
			".#hhcc#.",
			"#hhcccs#",
			"#hccccs#",
			"#cccccs#",
			"#cccsss#",
			".#csss#.",
			"..####..",
	};

	private Gem() {
	}

	public static void draw(Draw d, int x, int y) {
		for (int row = 0; row < SPRITE.length; row++) {
			String line = SPRITE[row];
			for (int col = 0; col < line.length(); col++) {
				int color = switch (line.charAt(col)) {
					case '#' -> Theme.DEEP;
					case 'h' -> Theme.ICE;
					case 'c' -> Theme.CYAN;
					case 's' -> 0xFF2B8FDB;
					default -> 0;
				};
				if (color != 0) d.fill(x + col, y + row, x + col + 1, y + row + 1, color);
			}
		}
	}

	/** Draws the gem at a button's right end, 6 pixels in, if the button is showing. */
	public static void onButton(Draw d, AbstractWidget button) {
		if (!button.visible) return;
		draw(d, button.getX() + button.getWidth() - 6 - SIZE, button.getY() + (button.getHeight() - SIZE) / 2);
	}
}
