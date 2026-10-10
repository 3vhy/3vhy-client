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
import dev.threevhy.client.hud.HudOverride;
import java.util.List;
import java.util.UUID;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;

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

	public static List<AbstractWidget> widgets(Screen screen) {
		return Screens.getButtons(screen);
	}

	public static UUID profileId(GameProfile profile) {
		return profile == null ? null : profile.id();
	}

	public static String profileName(GameProfile profile) {
		return profile == null ? null : profile.name();
	}

	/**
	 * Uploads a PNG as a texture under {@code id}, replacing (and freeing) what was there. Returns the
	 * image's {width, height}, or null if it couldn't be read. Render thread only.
	 */
	public static int[] uploadTexture(Identifier id, byte[] png) {
		try {
			NativeImage image = NativeImage.read(png);
			int[] size = {image.getWidth(), image.getHeight()};
			Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> "3vhy " + id, image));
			return size;
		} catch (Exception e) {
			dev.threevhy.client.ThreevhyClient.LOG.warn("Couldn't load a texture for {}", id, e);
			return null;
		}
	}
}
