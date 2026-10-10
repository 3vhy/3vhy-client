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
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.scores.Objective;
import dev.threevhy.client.hud.HudOverride;
import dev.threevhy.client.mixin.GuiAccessor;
import java.util.List;

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

	/** Minecraft's Options screen, opened from a menu (not from inside a world). */
	public static Screen optionsScreen(Screen parent) {
		return new OptionsScreen(parent, Minecraft.getInstance().options);
	}

	/** Draws in place of one of Minecraft's HUD pieces, e.g. the crosshair or the scoreboard. */
	public static void replaceHud(Identifier id, HudOverride override) {
		HudElementRegistry.replaceElement(id, original -> (graphics, deltaTracker) ->
				override.draw(new DrawImpl(graphics), () -> original.render(graphics, deltaTracker)));
	}

	/** Draws the sidebar scoreboard the way Minecraft does. */
	public static void drawScoreboard(Draw d, Objective objective) {
		((GuiAccessor) Minecraft.getInstance().gui).threevhy$displayScoreboardSidebar(((DrawImpl) d).graphics(), objective);
	}

	public static List<AbstractWidget> widgets(Screen screen) {
		return Screens.getButtons(screen);
	}
}
