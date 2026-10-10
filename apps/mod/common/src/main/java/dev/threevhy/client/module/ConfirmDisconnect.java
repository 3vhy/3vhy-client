package dev.threevhy.client.module;

import dev.threevhy.client.compat.Compat;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

/** The pause menu's Disconnect / Save and Quit button needs a second click within three seconds. */
public final class ConfirmDisconnect {
	private static final long WINDOW_MS = 3000;
	private static AbstractWidget armed = null;
	private static Component originalMessage = null;
	private static long armedAt = 0;

	private ConfirmDisconnect() {
	}

	public static void install() {
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (screen instanceof PauseScreen) {
				disarm();
				ScreenMouseEvents.allowMouseClick(screen).register(ConfirmDisconnect::allowClick);
			}
		});
	}

	private static boolean allowClick(Screen screen, MouseButtonEvent event) {
		if (!Modules.CONFIRM_DISCONNECT.enabled() || event.button() != 0) return true;
		for (AbstractWidget widget : Compat.widgets(screen)) {
			Component message = widget == armed ? originalMessage : widget.getMessage();
			if (!widget.visible || !widget.active || !isDisconnect(message)) continue;
			if (!widget.isMouseOver(event.x(), event.y())) continue;
			if (widget == armed && System.currentTimeMillis() - armedAt < WINDOW_MS) {
				disarm();
				return true;
			}
			arm(widget);
			return false;
		}
		return true;
	}

	static boolean isDisconnect(Component message) {
		if (message == null || !(message.getContents() instanceof TranslatableContents translatable)) return false;
		String key = translatable.getKey();
		return key.equals("menu.disconnect") || key.equals("menu.returnToMenu") || key.contains("saveAndQuit")
				|| key.contains("save_and_quit");
	}

	private static void arm(AbstractWidget widget) {
		disarm();
		armed = widget;
		originalMessage = widget.getMessage();
		armedAt = System.currentTimeMillis();
		widget.setMessage(Component.literal("Click again to confirm").withStyle(ChatFormatting.RED));
	}

	private static void disarm() {
		if (armed != null && originalMessage != null) armed.setMessage(originalMessage);
		armed = null;
		originalMessage = null;
	}

	/** Whether the button is waiting for its second click (for the in-game test). */
	public static boolean waiting() {
		return armed != null;
	}

	static void tick() {
		if (armed != null && System.currentTimeMillis() - armedAt > WINDOW_MS) disarm();
	}
}
