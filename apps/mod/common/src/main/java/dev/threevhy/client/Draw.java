package dev.threevhy.client;

import net.minecraft.world.item.ItemStack;

/**
 * The few drawing calls 3vhy needs. Minecraft renamed its GUI drawing class between
 * 1.21.11 (GuiGraphics) and 26.1 (GuiGraphicsExtractor), so each version has its own
 * DrawImpl in compat/ and everything else draws through this interface.
 */
public interface Draw {
	int width();

	int height();

	void fill(int x1, int y1, int x2, int y2, int argb);

	void text(String text, int x, int y, int argb, boolean shadow);

	void item(ItemStack stack, int x, int y);

	/** Durability bar and count label; pass null to use the stack's own count. */
	void itemOverlay(ItemStack stack, int x, int y, String countLabel);

	void push();

	void pop();

	void translate(float x, float y);

	void scale(float scale);

	/** Only draw inside this rectangle until disableScissor(). */
	void enableScissor(int x1, int y1, int x2, int y2);

	void disableScissor();

	default void outline(int x1, int y1, int x2, int y2, int argb) {
		fill(x1, y1, x2, y1 + 1, argb);
		fill(x1, y2 - 1, x2, y2, argb);
		fill(x1, y1, x1 + 1, y2, argb);
		fill(x2 - 1, y1, x2, y2, argb);
	}

	/** A filled rectangle with pixel-rounded corners. Rows never overlap, so see-through colours stay even. */
	default void roundRect(int x1, int y1, int x2, int y2, int radius, int argb) {
		int r = Math.max(0, Math.min(radius, Math.min((x2 - x1) / 2, (y2 - y1) / 2)));
		if (r == 0) {
			fill(x1, y1, x2, y2, argb);
			return;
		}
		fill(x1, y1 + r, x2, y2 - r, argb);
		for (int i = 0; i < r; i++) {
			int inset = cornerInset(r, i);
			fill(x1 + inset, y1 + i, x2 - inset, y1 + i + 1, argb);
			fill(x1 + inset, y2 - i - 1, x2 - inset, y2 - i, argb);
		}
	}

	/** A one pixel rounded border, matching roundRect. */
	default void roundOutline(int x1, int y1, int x2, int y2, int radius, int argb) {
		int r = Math.max(0, Math.min(radius, Math.min((x2 - x1) / 2, (y2 - y1) / 2)));
		if (r == 0) {
			outline(x1, y1, x2, y2, argb);
			return;
		}
		int top = cornerInset(r, 0);
		fill(x1 + top, y1, x2 - top, y1 + 1, argb);
		fill(x1 + top, y2 - 1, x2 - top, y2, argb);
		for (int i = 1; i < r; i++) {
			int inset = cornerInset(r, i);
			int span = Math.max(1, cornerInset(r, i - 1) - inset);
			fill(x1 + inset, y1 + i, x1 + inset + span, y1 + i + 1, argb);
			fill(x2 - inset - span, y1 + i, x2 - inset, y1 + i + 1, argb);
			fill(x1 + inset, y2 - i - 1, x1 + inset + span, y2 - i, argb);
			fill(x2 - inset - span, y2 - i - 1, x2 - inset, y2 - i, argb);
		}
		fill(x1, y1 + r, x1 + 1, y2 - r, argb);
		fill(x2 - 1, y1 + r, x2, y2 - r, argb);
	}

	private static int cornerInset(int r, int row) {
		double dy = r - row - 0.5;
		return (int) Math.round(r - Math.sqrt(r * r - dy * dy));
	}
}
