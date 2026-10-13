package dev.threevhy.client.gui;

import dev.threevhy.client.compat.Compat;
import dev.threevhy.client.module.ConfirmDisconnect;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The "3vhy Mod Menu" button in the pause menu. It takes the row just above Disconnect (or Save and
 * Quit), and everything from Disconnect down moves one row lower.
 */
public final class PauseMenuButton {
	public static final String LABEL = "3vhy Mod Menu";
	/** Minecraft's pause menu leaves 4 pixels between rows. */
	private static final int ROW_GAP = 4;

	private PauseMenuButton() {
	}

	public static void add(Minecraft mc, Screen screen) {
		List<AbstractWidget> widgets = Compat.widgets(screen);
		AbstractWidget disconnect = null;
		for (AbstractWidget widget : widgets) {
			if (widget.visible && ConfirmDisconnect.isDisconnect(widget.getMessage())) {
				disconnect = widget;
				break;
			}
		}
		Button.Builder builder = Button.builder(Component.literal(LABEL), button -> mc.setScreen(ModMenuScreen.create(screen)));
		if (disconnect == null) {
			Compat.addWidget(screen, builder.pos(6, 6).size(90, 20).build());
			return;
		}
		int rowY = disconnect.getY();
		int shift = disconnect.getHeight() + ROW_GAP;
		for (AbstractWidget widget : widgets) {
			if (widget.getY() >= rowY) widget.setY(widget.getY() + shift);
		}
		Compat.addWidget(screen, builder.pos(disconnect.getX(), rowY).size(disconnect.getWidth(), disconnect.getHeight()).build());
	}
}
