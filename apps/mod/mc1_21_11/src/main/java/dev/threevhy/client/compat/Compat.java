package dev.threevhy.client.compat;

import java.util.function.Consumer;
import dev.threevhy.client.Draw;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Minecraft 1.21.11 / Fabric API (Yarn-era names) glue. */
public final class Compat {
	private Compat() {
	}

	public static KeyMapping registerKey(KeyMapping key) {
		return KeyBindingHelper.registerKeyBinding(key);
	}

	public static void registerHud(Identifier id, Consumer<Draw> drawer) {
		HudElementRegistry.addLast(id, (graphics, deltaTracker) -> drawer.accept(new DrawImpl(graphics)));
	}

	public static void addWidget(Screen screen, AbstractWidget widget) {
		Screens.getButtons(screen).add(widget);
	}

	public static void chat(Component message) {
		Minecraft.getInstance().gui.getChat().addMessage(message);
	}
}
