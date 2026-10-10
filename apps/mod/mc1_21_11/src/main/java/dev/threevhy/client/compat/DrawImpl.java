package dev.threevhy.client.compat;

import dev.threevhy.client.Draw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Draw for Minecraft 1.21.11 (GuiGraphics). */
public final class DrawImpl implements Draw {
	private final GuiGraphics graphics;

	public DrawImpl(GuiGraphics graphics) {
		this.graphics = graphics;
	}

	@Override
	public int width() {
		return graphics.guiWidth();
	}

	@Override
	public int height() {
		return graphics.guiHeight();
	}

	@Override
	public void fill(int x1, int y1, int x2, int y2, int argb) {
		graphics.fill(x1, y1, x2, y2, argb);
	}

	@Override
	public void text(String text, int x, int y, int argb, boolean shadow) {
		graphics.drawString(Minecraft.getInstance().font, text, x, y, argb, shadow);
	}

	@Override
	public void text(Component text, int x, int y, int argb, boolean shadow) {
		graphics.drawString(Minecraft.getInstance().font, text, x, y, argb, shadow);
	}

	public GuiGraphics graphics() {
		return graphics;
	}

	@Override
	public void item(ItemStack stack, int x, int y) {
		graphics.renderFakeItem(stack, x, y);
	}

	@Override
	public void itemOverlay(ItemStack stack, int x, int y, String countLabel) {
		graphics.renderItemDecorations(Minecraft.getInstance().font, stack, x, y, countLabel);
	}

	@Override
	public void push() {
		graphics.pose().pushMatrix();
	}

	@Override
	public void pop() {
		graphics.pose().popMatrix();
	}

	@Override
	public void translate(float x, float y) {
		graphics.pose().translate(x, y);
	}

	@Override
	public void scale(float scale) {
		graphics.pose().scale(scale, scale);
	}

	@Override
	public void enableScissor(int x1, int y1, int x2, int y2) {
		graphics.enableScissor(x1, y1, x2, y2);
	}

	@Override
	public void disableScissor() {
		graphics.disableScissor();
	}
}
