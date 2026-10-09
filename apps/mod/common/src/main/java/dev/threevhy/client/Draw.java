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
}
