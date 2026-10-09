package dev.threevhy.client.test;

import com.mojang.blaze3d.platform.InputConstants;
import dev.threevhy.client.Hooks;
import dev.threevhy.client.ThreevhyClient;
import dev.threevhy.client.gui.HudEditorScreen;
import dev.threevhy.client.gui.SettingsScreen;
import dev.threevhy.client.module.Modules;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import org.lwjgl.glfw.GLFW;

/**
 * Runs only in CI: starts the real game with 3vhy loaded, plays a short scripted session and
 * takes screenshots of every 3vhy screen. Fails if one of 3vhy's hooks into Minecraft never ran.
 */
public final class ThreevhyGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		context.waitTicks(40);
		context.takeScreenshot("3vhy-title-screen");

		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			context.waitTicks(200);
			context.runOnClient(client -> {
				Modules.COORDS.set(true);
				Modules.CRYSTAL_OPTIMIZER.set(true);
			});

			var server = singleplayer.getServer();
			server.runCommand("gamemode survival @a");
			server.runCommand("time set day");
			server.runCommand("item replace entity @a armor.head with diamond_helmet");
			server.runCommand("item replace entity @a armor.chest with diamond_chestplate[damage=420]");
			server.runCommand("item replace entity @a armor.legs with netherite_leggings");
			server.runCommand("item replace entity @a armor.feet with diamond_boots[damage=380]");
			server.runCommand("item replace entity @a weapon.mainhand with netherite_sword");
			server.runCommand("item replace entity @a weapon.offhand with totem_of_undying");
			server.runCommand("give @a totem_of_undying 4");
			server.runCommand("give @a end_crystal 64");
			server.runCommand("give @a obsidian 64");
			server.runCommand("give @a experience_bottle 64");
			server.runCommand("give @a enchanted_golden_apple 3");
			server.runCommand("execute at @p run summon zombie ~ ~ ~3 {NoAI:1b,Silent:1b,PersistenceRequired:1b,"
					+ "equipment:{head:{id:\"minecraft:diamond_helmet\",count:1},offhand:{id:\"minecraft:totem_of_undying\",count:1}}}");
			context.waitTicks(40);
			context.takeScreenshot("3vhy-hud");

			// Pop a totem: chat message, pop counter and the "no totem in offhand" warning
			server.runCommand("damage @a 1000 minecraft:generic");
			context.waitTicks(30);
			context.takeScreenshot("3vhy-totem-pop");

			// Look at the zombie and hit it for the target HUD
			context.getInput().lookAt(0f, 10f);
			context.waitTicks(5);
			context.getInput().pressMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
			context.waitTicks(10);
			context.takeScreenshot("3vhy-target-hud");

			context.getInput().holdKey(ThreevhyClient.zoomKey);
			context.waitTicks(20);
			context.takeScreenshot("3vhy-zoom");
			context.getInput().releaseKey(ThreevhyClient.zoomKey);
			context.waitTicks(10);

			context.setScreen(() -> new SettingsScreen(null));
			context.waitTicks(10);
			context.takeScreenshot("3vhy-settings");

			context.setScreen(() -> new HudEditorScreen(null));
			context.waitTicks(10);
			context.takeScreenshot("3vhy-hud-editor");

			context.setScreen(() -> null);
			context.waitTicks(5);
			context.getInput().pressKey(InputConstants.KEY_ESCAPE);
			context.waitTicks(10);
			context.takeScreenshot("3vhy-pause-menu");
			context.setScreen(() -> null);
			context.waitTicks(5);
		}

		List<String> failed = new ArrayList<>();
		check(failed, "window title hook (Minecraft.updateTitle)", Hooks.titleHookRan);
		check(failed, "zoom hook (GameRenderer.getFov)", Hooks.fovHookRan);
		check(failed, "totem pop hook (ClientPacketListener.handleEntityEvent)", Hooks.entityEventHookRan);
		if (!failed.isEmpty()) {
			throw new AssertionError("3vhy hooks that never ran: " + String.join(", ", failed));
		}
	}

	private static void check(List<String> failed, String name, boolean ran) {
		ThreevhyClient.LOG.info("3vhy-check: {} -> {}", name, ran ? "OK" : "MISSING");
		if (!ran) failed.add(name);
	}
}
