package dev.threevhy.client.test;

import com.mojang.blaze3d.platform.InputConstants;
import dev.threevhy.client.Hooks;
import dev.threevhy.client.ThreevhyClient;
import dev.threevhy.client.gui.HudEditorScreen;
import dev.threevhy.client.gui.MainMenuScreen;
import dev.threevhy.client.gui.SettingsScreen;
import dev.threevhy.client.module.Modules;
import dev.threevhy.client.module.PopTracker;
import dev.threevhy.client.module.Target;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import org.lwjgl.glfw.GLFW;

/**
 * Runs only in CI: starts the real game with 3vhy loaded, plays a short scripted session and
 * takes screenshots of every 3vhy screen. Fails if one of 3vhy's features didn't work.
 */
public final class ThreevhyGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		List<String> failed = new ArrayList<>();

		context.waitTicks(40);
		context.takeScreenshot("3vhy-main-menu");
		check(failed, "3vhy main menu replaces the title screen",
				context.computeOnClient(client -> client.screen instanceof MainMenuScreen) && Hooks.mainMenuShown);
		context.setScreen(() -> new SettingsScreen(new MainMenuScreen()));
		context.waitTicks(10);
		context.takeScreenshot("3vhy-settings-from-menu");
		context.setScreen(MainMenuScreen::new);
		context.waitTicks(5);

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
			server.runCommand("damage @p 1000 minecraft:generic");
			context.waitTicks(30);
			context.takeScreenshot("3vhy-totem-pop");
			check(failed, "totem pop counted", context.computeOnClient(client -> !PopTracker.top(1).isEmpty()));

			// Face the zombie (south, slightly down) and hit it for the target HUD
			context.runOnClient(client -> {
				client.player.setYRot(0f);
				client.player.setXRot(10f);
			});
			context.waitTicks(5);
			context.getInput().pressMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
			context.waitTicks(10);
			context.takeScreenshot("3vhy-target-hud");
			check(failed, "target HUD picks up who you hit", context.computeOnClient(client -> Target.get() != null));

			int normalFov = context.computeOnClient(client -> client.options.fov().get());
			context.getInput().holdKey(ThreevhyClient.zoomKey);
			context.waitTicks(20);
			context.takeScreenshot("3vhy-zoom");
			int zoomedFov = context.computeOnClient(client -> client.options.fov().get());
			context.getInput().releaseKey(ThreevhyClient.zoomKey);
			context.waitTicks(20);
			int afterFov = context.computeOnClient(client -> client.options.fov().get());
			ThreevhyClient.LOG.info("3vhy-check: zoom FOV {} -> {} -> {}", normalFov, zoomedFov, afterFov);
			check(failed, "zoom lowers the FOV", zoomedFov < normalFov);
			check(failed, "zoom gives the FOV back", afterFov == normalFov);

			context.setScreen(() -> new SettingsScreen(null));
			context.waitTicks(10);
			context.takeScreenshot("3vhy-settings");
			context.getInput().typeChars("totem");
			context.waitTicks(5);
			context.takeScreenshot("3vhy-settings-search");

			context.setScreen(() -> new SettingsScreen(null).openOptions(Modules.ZOOM));
			context.waitTicks(10);
			context.takeScreenshot("3vhy-settings-zoom-page");

			context.setScreen(() -> new SettingsScreen(null).openGeneral());
			context.waitTicks(10);
			context.takeScreenshot("3vhy-settings-general");

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

		context.waitTicks(10);
		check(failed, "3vhy main menu after leaving a world",
				context.computeOnClient(client -> client.screen instanceof MainMenuScreen));
		check(failed, "window title hook (Minecraft.updateTitle)", Hooks.titleHookRan);
		check(failed, "totem pop hook (ClientPacketListener.handleEntityEvent)", Hooks.entityEventHookRan);
		if (!failed.isEmpty()) {
			throw new AssertionError("3vhy checks that failed: " + String.join(", ", failed));
		}
	}

	private static void check(List<String> failed, String name, boolean ok) {
		ThreevhyClient.LOG.info("3vhy-check: {} -> {}", name, ok ? "OK" : "MISSING");
		if (!ok) failed.add(name);
	}
}
