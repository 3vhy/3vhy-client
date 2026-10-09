package dev.threevhy.client.compat;

import dev.threevhy.client.Draw;
import net.minecraft.client.gui.GuiGraphics;
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

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		// A plain dim instead of Minecraft's blur
		if (dimBackground()) {
			graphics.fill(0, 0, this.width, this.height, 0xB0040A20);
		}
	}
}
