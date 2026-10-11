package dev.threevhy.client.test;

import com.mojang.blaze3d.platform.InputConstants;
import dev.threevhy.client.Config;
import dev.threevhy.client.Hooks;
import dev.threevhy.client.ThreevhyClient;
import dev.threevhy.client.compat.Compat;
import dev.threevhy.client.gui.HubScreen;
import dev.threevhy.client.gui.MainMenuScreen;
import dev.threevhy.client.gui.ModMenuScreen;
import dev.threevhy.client.gui.PauseMenuButton;
import dev.threevhy.client.hud.HudWidgets;
import dev.threevhy.client.module.ChatTweaks;
import dev.threevhy.client.module.ConfirmDisconnect;
import dev.threevhy.client.module.Freelook;
import dev.threevhy.client.module.Modules;
import dev.threevhy.client.module.NameProtect;
import dev.threevhy.client.module.PlayerList;
import dev.threevhy.client.module.PopTracker;
import dev.threevhy.client.module.Target;
import dev.threevhy.client.music.MusicBridge;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.lwjgl.glfw.GLFW;

/**
 * Runs only in CI: starts the real game with 3vhy loaded, plays a short scripted session and
 * takes screenshots of every 3vhy screen. Fails if one of 3vhy's features didn't work.
 */
public final class ThreevhyGameTest implements FabricClientGameTest {
	/** A song as the Windows helper reports it, with a tiny 8x8 cover, since CI can't play real music. */
	private static final String SAMPLE_SONG = "{\"key\":\"test\",\"app\":\"Spotify.exe\",\"title\":\"Snowfall\","
			+ "\"artist\":\"3vhy Test Band\",\"album\":\"CI\",\"playing\":true,\"position\":61,\"duration\":235,"
			+ "\"art\":\"iVBORw0KGgoAAAANSUhEUgAAAAgAAAAICAYAAADED76LAAAAG0lEQVR4nGPUsDnxX5LhGwMuzMIgwoAXDA8FAKZeCj5VXRt0AAAAAElFTkSuQmCC\"}";

	@Override
	public void runTest(ClientGameTestContext context) {
		List<String> failed = new ArrayList<>();

		context.waitTicks(40);
		shot(context, "3vhy-main-menu");
		check(failed, "3vhy main menu replaces the title screen",
				context.computeOnClient(client -> client.screen instanceof MainMenuScreen) && Hooks.mainMenuShown);
		context.setScreen(() -> new ModMenuScreen(new MainMenuScreen()));
		context.waitTicks(10);
		shot(context, "3vhy-modmenu-from-menu");
		context.setScreen(() -> new HubScreen(new MainMenuScreen()));
		context.waitTicks(10);
		shot(context, "3vhy-hub-from-menu");
		context.setScreen(MainMenuScreen::new);
		context.waitTicks(5);

		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			context.waitTicks(200);
			context.runOnClient(client -> {
				Modules.COORDS.set(true);
				Modules.CLOCK.set(true);
				Modules.CRYSTAL_OPTIMIZER.set(true);
				Modules.SHINY_POTS.set(true);
				Modules.CROSSHAIR.set(true);
				Modules.NAMETAGS.set(true);
				Modules.CPS.sliderSetting("background").set(0);
				Modules.KEYSTROKES.sliderSetting("background").set(25);
				Modules.MUSIC.set(true);
				MusicBridge.simulate(SAMPLE_SONG);
				Modules.PLAYER_LIST_OFFLINE.set(true);
				Modules.PLAYER_LIST_TITLE.set("Friends");
				PlayerList.add(client.getUser().getName());
				PlayerList.add("Steve");
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
			server.runCommand("give @a splash_potion[potion_contents={potion:\"minecraft:strong_healing\"}] 3");
			server.runCommand("scoreboard objectives add kills dummy \"Kills\"");
			server.runCommand("scoreboard objectives setdisplay sidebar kills");
			server.runCommand("scoreboard objectives setdisplay list kills");
			server.runCommand("scoreboard players set Steve kills 29");
			server.runCommand("scoreboard players set Alex kills 17");
			server.runCommand("scoreboard players set @p kills 3");
			server.runCommand("execute at @p run summon zombie ~ ~ ~3 {NoAI:1b,Silent:1b,PersistenceRequired:1b,"
					+ "equipment:{head:{id:\"minecraft:diamond_helmet\",count:1},offhand:{id:\"minecraft:totem_of_undying\",count:1}}}");
			server.runCommand("tellraw @a \"3vhy chat test\"");
			server.runCommand("tellraw @a \"3vhy chat test\"");
			context.runOnClient(client -> {
				client.player.setYRot(0f);
				client.player.setXRot(10f);
				Config.Waypoint ahead = new Config.Waypoint();
				ahead.name = "Base";
				ahead.x = client.player.blockPosition().getX();
				ahead.y = client.player.blockPosition().getY();
				ahead.z = client.player.blockPosition().getZ() + 12;
				ahead.dimension = dev.threevhy.client.module.Waypoints.dimension(client);
				dev.threevhy.client.module.Waypoints.current(client).add(ahead);
			});
			context.waitTicks(40);
			shot(context, "3vhy-hud");
			check(failed, "chat stacks repeated messages", context.computeOnClient(client -> ChatTweaks.repeats() >= 2));
			check(failed, "music HUD shows the playing song", context.computeOnClient(client ->
					MusicBridge.current() != null && MusicBridge.current().title().equals("Snowfall")
							&& HudWidgets.MUSIC.visible(client)));
			check(failed, "music cover uploaded as a texture (NativeImage, DynamicTexture)", Hooks.musicArtUploaded);
			server.runCommand("bossbar add threevhy:test \"Boss\"");
			server.runCommand("bossbar set threevhy:test players @a");
			context.waitTicks(10);
			int musicY = context.computeOnClient(client -> HudWidgets.MUSIC.position(client,
					client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight())[1]);
			ThreevhyClient.LOG.info("3vhy-check: music HUD top with a boss bar: {}", musicY);
			check(failed, "music HUD moves below boss bars (BossHealthOverlay)", musicY > 17);
			shot(context, "3vhy-music-under-bossbar");
			server.runCommand("bossbar remove threevhy:test");
			check(failed, "player list sees who is online", context.computeOnClient(client ->
					Hooks.playerListTicked && PlayerList.isOnline(client.getUser().getName()) && !PlayerList.isOnline("Steve")
							&& HudWidgets.PLAYER_LIST.visible(client)));

			// Pop a totem: chat message, pop counter and the "no totem in offhand" warning
			server.runCommand("damage @p 1000 minecraft:generic");
			context.waitTicks(30);
			shot(context, "3vhy-totem-pop");
			check(failed, "totem pop counted", context.computeOnClient(client -> !PopTracker.top(1).isEmpty()));

			// Hit the zombie for the target HUD
			context.runOnClient(client -> {
				client.player.setYRot(0f);
				client.player.setXRot(10f);
			});
			context.waitTicks(5);
			context.getInput().pressMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
			context.waitTicks(10);
			shot(context, "3vhy-target-hud");
			check(failed, "target HUD picks up who you hit", context.computeOnClient(client -> Target.get() != null));

			// Name Protect and Skin Protect stay on through freelook, so the third person view shows them
			context.runOnClient(client -> {
				Modules.NAME_PROTECT.set(true);
				Modules.NAME_PROTECT_NAME.set("Streamer");
				Modules.SKIN_PROTECT.set(true);
				Compat.chat(net.minecraft.network.chat.Component.literal(client.getUser().getName() + " is streaming"));
			});
			context.waitTicks(10);
			shot(context, "3vhy-name-protect");
			check(failed, "name protect swaps your name", context.computeOnClient(client ->
					NameProtect.apply("<" + client.getUser().getName() + "> hi").equals("<Streamer> hi")
							&& NameProtect.apply(client.getUser().getName() + "son").equals(client.getUser().getName() + "son")));
			check(failed, "name protect hook (StringDecomposer)", Hooks.nameProtectRan);

			int normalFov = context.computeOnClient(client -> client.options.fov().get());
			context.getInput().holdKey(ThreevhyClient.zoomKey);
			context.waitTicks(20);
			shot(context, "3vhy-zoom");
			int zoomedFov = context.computeOnClient(client -> client.options.fov().get());
			context.getInput().releaseKey(ThreevhyClient.zoomKey);
			context.waitTicks(20);
			int afterFov = context.computeOnClient(client -> client.options.fov().get());
			ThreevhyClient.LOG.info("3vhy-check: zoom FOV {} -> {} -> {}", normalFov, zoomedFov, afterFov);
			check(failed, "zoom lowers the FOV", zoomedFov < normalFov);
			check(failed, "zoom gives the FOV back", afterFov == normalFov);

			// Freelook: third person while held, back to first person after
			context.getInput().holdKey(ThreevhyClient.freelookKey);
			context.waitTicks(10);
			shot(context, "3vhy-freelook");
			check(failed, "freelook switches to third person", context.computeOnClient(client ->
					Freelook.active() && client.options.getCameraType() == CameraType.THIRD_PERSON_BACK));
			context.getInput().releaseKey(ThreevhyClient.freelookKey);
			context.waitTicks(5);
			check(failed, "freelook puts the camera back", context.computeOnClient(client ->
					!Freelook.active() && client.options.getCameraType() == CameraType.FIRST_PERSON));
			check(failed, "skin protect hook (PlayerInfo.getSkin)", Hooks.skinProtectRan);
			context.runOnClient(client -> {
				Modules.NAME_PROTECT.set(false);
				Modules.SKIN_PROTECT.set(false);
			});

			// Tab list with ping numbers and the scoreboard objective
			context.getInput().holdKey(options -> options.keyPlayerList);
			context.waitTicks(5);
			shot(context, "3vhy-tab-list");
			context.getInput().releaseKey(options -> options.keyPlayerList);

			// The scoreboard made bigger and blue
			context.runOnClient(client -> {
				Modules.SCOREBOARD_SIZE.set(150);
				Modules.SCOREBOARD_BACKGROUND.set(65);
				Modules.SCOREBOARD_COLOR.set(0xFF1A4FBA);
			});
			context.waitTicks(5);
			shot(context, "3vhy-scoreboard-styled");
			context.runOnClient(client -> {
				Modules.SCOREBOARD_BACKGROUND.reset();
				Modules.SCOREBOARD_COLOR.reset();
			});

			// Inventory with highlighted PvP items
			context.setScreen(() -> new InventoryScreen(net.minecraft.client.Minecraft.getInstance().player));
			context.waitTicks(10);
			shot(context, "3vhy-inventory-highlight");
			context.setScreen(() -> null);
			context.waitTicks(5);

			// The hub: HUD drawn sharp over the blur, with the logo and mod menu button
			context.getInput().pressKey(ThreevhyClient.openSettingsKey);
			context.waitTicks(15);
			shot(context, "3vhy-hub");
			check(failed, "Right Shift opens the hub", context.computeOnClient(client -> client.screen instanceof HubScreen));
			double sizeBefore = context.computeOnClient(client -> Modules.SCOREBOARD_SIZE.get());
			context.runOnClient(client -> {
				var widget = HudWidgets.SCOREBOARD;
				float scale = dev.threevhy.client.hud.HudRenderer.scaleFor(widget);
				int screenW = client.getWindow().getGuiScaledWidth();
				int screenH = client.getWindow().getGuiScaledHeight();
				int[] pos = widget.position(client, Math.round(screenW / scale), Math.round(screenH / scale));
				double x = (pos[0] + widget.width(client) / 2.0) * scale;
				double y = (pos[1] + widget.height(client) / 2.0) * scale;
				client.screen.mouseScrolled(x, y, 0, -1);
			});
			check(failed, "scrolling over the scoreboard in the hub resizes it",
					context.computeOnClient(client -> Modules.SCOREBOARD_SIZE.get() < sizeBefore));
			context.runOnClient(client -> Modules.SCOREBOARD_SIZE.reset());

			context.setScreen(() -> new ModMenuScreen(null));
			context.waitTicks(10);
			shot(context, "3vhy-modmenu");
			context.getInput().typeChars("totem");
			context.waitTicks(5);
			shot(context, "3vhy-modmenu-search");
			context.setScreen(() -> new ModMenuScreen(null).openModule(Modules.CROSSHAIR));
			context.waitTicks(10);
			shot(context, "3vhy-modmenu-crosshair");
			context.setScreen(() -> new ModMenuScreen(null).openModule(Modules.PLAYER_LIST));
			context.waitTicks(10);
			shot(context, "3vhy-modmenu-player-list");
			context.setScreen(() -> new ModMenuScreen(null).openModule(Modules.NAME_PROTECT));
			context.waitTicks(10);
			shot(context, "3vhy-modmenu-name-protect");
			context.setScreen(() -> new ModMenuScreen(null).openModule(Modules.SCOREBOARD));
			context.waitTicks(10);
			shot(context, "3vhy-modmenu-scoreboard");
			context.runOnClient(client -> Config.get().createProfile("Crystal PvP"));
			context.setScreen(() -> new ModMenuScreen(null).openTab(ModMenuScreen.Tab.PROFILES));
			context.waitTicks(10);
			shot(context, "3vhy-modmenu-profiles");
			check(failed, "profiles can be created", context.computeOnClient(client ->
					Config.get().profile.equals("Crystal PvP") && Config.get().profileNames().contains(Config.DEFAULT_PROFILE)));
			context.runOnClient(client -> Config.get().loadProfile(Config.DEFAULT_PROFILE));
			context.setScreen(() -> new ModMenuScreen(null).openTab(ModMenuScreen.Tab.WAYPOINTS));
			context.waitTicks(10);
			shot(context, "3vhy-modmenu-waypoints");
			context.runOnClient(client -> Modules.THEME_MODE.next());
			context.setScreen(() -> new ModMenuScreen(null));
			context.waitTicks(10);
			shot(context, "3vhy-modmenu-light");
			context.runOnClient(client -> Modules.THEME_MODE.next());

			// Pause menu: the mod menu button, then Confirm Disconnect
			context.setScreen(() -> null);
			context.waitTicks(5);
			context.getInput().pressKey(InputConstants.KEY_ESCAPE);
			context.waitTicks(10);
			shot(context, "3vhy-pause-menu");
			int scale = context.computeOnClient(client -> client.getWindow().getGuiScale());
			int[] modMenuButton = context.computeOnClient(client -> {
				AbstractWidget ours = null;
				AbstractWidget disconnect = null;
				for (AbstractWidget widget : Compat.widgets(client.screen)) {
					if (widget.getMessage().getString().equals(PauseMenuButton.LABEL)) ours = widget;
					if (ConfirmDisconnect.isDisconnect(widget.getMessage())) disconnect = widget;
				}
				if (ours == null || disconnect == null) return null;
				boolean above = ours.getX() == disconnect.getX() && ours.getY() + ours.getHeight() + 4 == disconnect.getY();
				return new int[] {ours.getX() + ours.getWidth() / 2, ours.getY() + ours.getHeight() / 2, above ? 1 : 0};
			});
			check(failed, "3vhy Mod Menu button sits right above Disconnect", modMenuButton != null && modMenuButton[2] == 1);
			if (modMenuButton != null) {
				context.getInput().setCursorPos(modMenuButton[0] * scale, modMenuButton[1] * scale);
				context.waitTicks(2);
				context.getInput().pressMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
				context.waitTicks(15);
			}
			check(failed, "pause menu button opens the mod menu",
					context.computeOnClient(client -> client.screen instanceof ModMenuScreen));
			context.getInput().pressKey(InputConstants.KEY_ESCAPE);
			context.waitTicks(5);
			check(failed, "closing the mod menu goes back to the pause menu",
					context.computeOnClient(client -> client.screen instanceof PauseScreen));

			int[] quit = context.computeOnClient(client -> {
				for (AbstractWidget widget : Compat.widgets(client.screen)) {
					if (widget.getMessage().getContents() instanceof TranslatableContents key
							&& (key.getKey().equals("menu.returnToMenu") || key.getKey().equals("menu.disconnect"))) {
						return new int[] {widget.getX() + widget.getWidth() / 2, widget.getY() + widget.getHeight() / 2};
					}
				}
				return null;
			});
			if (quit != null) {
				context.getInput().setCursorPos(quit[0] * scale, quit[1] * scale);
				context.waitTicks(2);
				context.getInput().pressMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
				context.waitTicks(5);
				shot(context, "3vhy-confirm-disconnect");
				check(failed, "disconnect needs a second click", context.computeOnClient(client ->
						client.screen instanceof PauseScreen && client.level != null && ConfirmDisconnect.waiting()));
			} else {
				check(failed, "found the pause menu's quit button", false);
			}
			context.setScreen(() -> null);
			context.waitTicks(5);
		}

		context.waitTicks(10);
		check(failed, "3vhy main menu after leaving a world",
				context.computeOnClient(client -> client.screen instanceof MainMenuScreen));
		check(failed, "window title hook (Minecraft.updateTitle)", Hooks.titleHookRan);
		String title = dev.threevhy.client.WindowBranding.title();
		ThreevhyClient.LOG.info("3vhy-check: window title is \"{}\"", title);
		check(failed, "window title starts with Minecraft, for Medal", title.startsWith("Minecraft"));
		check(failed, "tab list ping numbers (PlayerTabOverlay)", Hooks.tabPingRan);
		check(failed, "item highlighter (AbstractContainerScreen slots)", Hooks.slotHighlightRan);
		check(failed, "freelook camera (Camera.setRotation)", Hooks.freelookCameraRan);
		check(failed, "own nametag in third person (shouldShowName)", Hooks.ownNametagRan);
		check(failed, "shiny pots (ItemStack.hasFoil)", Hooks.shinyPotRan);
		check(failed, "TPS from server time (handleSetTime)", Hooks.timePacketRan);
		check(failed, "custom crosshair replaces Minecraft's", Hooks.crosshairRan);
		check(failed, "scoreboard drawn through 3vhy", Hooks.scoreboardRan);
		check(failed, "totem pop hook (ClientPacketListener.handleEntityEvent)", Hooks.entityEventHookRan);
		if (!failed.isEmpty()) {
			throw new AssertionError("3vhy checks that failed: " + String.join(", ", failed));
		}
	}

	/**
	 * Sodium on 26.1 can't draw the world in the extra frame a test screenshot renders, so
	 * in-world screenshots are skipped there; everything else still runs.
	 */
	private static void shot(ClientGameTestContext context, String name) {
		boolean sodiumOn26 = FabricLoader.getInstance().isModLoaded("sodium")
				&& ThreevhyClient.minecraftVersion().startsWith("26.");
		if (sodiumOn26 && context.computeOnClient(client -> client.level != null)) {
			ThreevhyClient.LOG.info("3vhy-check: screenshot {} skipped (Sodium on 26.1)", name);
			return;
		}
		context.takeScreenshot(name);
	}

	private static void check(List<String> failed, String name, boolean ok) {
		ThreevhyClient.LOG.info("3vhy-check: {} -> {}", name, ok ? "OK" : "MISSING");
		if (!ok) failed.add(name);
	}
}
