package dev.threevhy.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.threevhy.client.compat.Compat;
import dev.threevhy.client.discord.DiscordPresence;
import dev.threevhy.client.gui.MainMenuScreen;
import dev.threevhy.client.gui.SettingsScreen;
import dev.threevhy.client.hud.CpsTracker;
import dev.threevhy.client.hud.HudRenderer;
import dev.threevhy.client.module.CrystalOptimizer;
import dev.threevhy.client.module.Modules;
import dev.threevhy.client.module.PopTracker;
import dev.threevhy.client.module.Target;
import dev.threevhy.client.module.Zoom;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 3vhy Client, inside Minecraft. */
public final class ThreevhyClient implements ClientModInitializer {
	public static final String MOD_ID = "threevhy";
	public static final Logger LOG = LoggerFactory.getLogger("3vhy Client");

	public static KeyMapping openSettingsKey;
	public static KeyMapping zoomKey;

	private static String version = "?";
	private static String minecraftVersion = "";

	@Override
	public void onInitializeClient() {
		version = FabricLoader.getInstance().getModContainer(MOD_ID)
				.map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?");
		int plus = version.indexOf('+');
		if (plus > 0) version = version.substring(0, plus);
		minecraftVersion = FabricLoader.getInstance().getModContainer("minecraft")
				.map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("");

		Config.load();
		DiscordPresence.start();

		KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
		openSettingsKey = Compat.registerKey(new KeyMapping("key.threevhy.open_settings", InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_RIGHT_SHIFT, category));
		zoomKey = Compat.registerKey(new KeyMapping("key.threevhy.zoom", InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_C, category));

		Compat.registerHud(Identifier.fromNamespaceAndPath(MOD_ID, "hud"), HudRenderer::render);

		ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
			WindowBranding.apply();
			CpsTracker.install();
		});
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
			Zoom.restore(client);
			DiscordPresence.stop();
			Config.save();
		});
		ClientTickEvents.END_CLIENT_TICK.register(ThreevhyClient::tick);
		ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> {
			PopTracker.reset();
			Zoom.restore(client);
		});
		AttackEntityCallback.EVENT.register(CrystalOptimizer::onAttack);
		AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
			if (level.isClientSide()) Target.onAttack(entity);
			return InteractionResult.PASS;
		});

		// A "3vhy Settings" button in the pause menu
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (screen instanceof PauseScreen) {
				Compat.addWidget(screen, Button.builder(Component.literal("3vhy Settings"),
								button -> client.setScreen(new SettingsScreen(screen)))
						.pos(6, 6)
						.size(90, 20)
						.build());
			}
		});

		LOG.info("3vhy Client {} loaded on Minecraft {}", version, minecraftVersion);
	}

	private static void tick(Minecraft mc) {
		while (openSettingsKey.consumeClick()) {
			if (mc.screen == null) {
				mc.setScreen(new SettingsScreen(null));
			}
		}
		// Backup for the setScreen hook, and switches menus when the module is toggled
		if (mc.screen != null && mc.screen.getClass() == TitleScreen.class && Modules.MAIN_MENU.enabled()) {
			mc.setScreen(new MainMenuScreen());
		} else if (mc.screen instanceof MainMenuScreen && !Modules.MAIN_MENU.enabled()) {
			mc.setScreen(new TitleScreen());
		}
		Modules.tick(mc);
		DiscordPresence.tick(mc);
		Config.tick();
	}

	public static String version() {
		return version;
	}

	public static String minecraftVersion() {
		return minecraftVersion;
	}
}
