package dev.threevhy.client.compat;

import dev.threevhy.client.Draw;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;

/**
 * A title screen that draws through Draw. It extends Minecraft's TitleScreen so everything
 * that looks for "the title screen" still finds it. Minecraft 1.21.11 version.
 */
public abstract class CompatTitleScreen extends TitleScreen {
	protected CompatTitleScreen() {
		super();
	}

	protected abstract void draw(Draw d, int mouseX, int mouseY, float delta);

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		draw(new DrawImpl(graphics), mouseX, mouseY, delta);
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		// The whole screen, background included, is drawn in draw()
	}
}
