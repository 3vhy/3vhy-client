package dev.threevhy.client.compat;

import dev.threevhy.client.Draw;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** A screen that draws through Draw. Minecraft 1.21.11 version. */
public abstract class CompatScreen extends Screen {
	protected CompatScreen(Component title) {
		super(title);
	}

	protected abstract void draw(Draw d, int mouseX, int mouseY, float delta);

	protected boolean dimBackground() {
		return true;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		draw(new DrawImpl(graphics), mouseX, mouseY, delta);
	}

	/**
	 * Drawn over the background. In a world the game behind is already blurred (Minecraft's own
	 * menu blur), so this only tints it.
	 */
	protected void drawBackdrop(Draw d) {
		d.fill(0, 0, this.width, this.height, 0xB0040A20);
	}

	/** Whether the game behind is blurred, Minecraft's way; the Lunar-style menus leave it sharp. */
	protected boolean blurBackground() {
		return true;
	}

	/**
	 * Whether Minecraft's dark menu texture goes over the blurred game; 3vhy's own menus draw their
	 * violet tint instead, so they keep just the blur.
	 */
	protected boolean menuTexture() {
		return true;
	}

	/** Whether Minecraft's panorama shows behind this menu when there's no world, as on the main menu. */
	protected boolean panoramaBackground() {
		return false;
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		if (!dimBackground()) return;
		if (Minecraft.getInstance().level != null) {
			if (blurBackground() && menuTexture()) super.renderBackground(graphics, mouseX, mouseY, delta);
			else if (blurBackground()) renderBlurredBackground(graphics);
		} else if (panoramaBackground()) {
			renderPanorama(graphics, delta);
		}
		drawBackdrop(new DrawImpl(graphics));
	}
}
