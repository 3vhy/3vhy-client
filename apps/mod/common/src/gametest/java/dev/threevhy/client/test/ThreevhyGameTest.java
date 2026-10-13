package dev.threevhy.client.test;

import com.mojang.blaze3d.platform.InputConstants;
import dev.threevhy.client.Config;
import dev.threevhy.client.Draw;
import dev.threevhy.client.Hooks;
import dev.threevhy.client.ThreevhyClient;
import dev.threevhy.client.compat.Compat;
import dev.threevhy.client.compat.CompatScreen;
import dev.threevhy.client.gui.ClickGuiScreen;
import dev.threevhy.client.gui.HubScreen;
import dev.threevhy.client.gui.MainMenuScreen;
import dev.threevhy.client.gui.ModMenuScreen;
import dev.threevhy.client.gui.TitleScreenStyle;
import dev.threevhy.client.gui.PauseMenuButton;
import dev.threevhy.client.FreezeWatch;
import dev.threevhy.client.hud.CpsTracker;
import dev.threevhy.client.hud.HudWidget;
import dev.threevhy.client.hud.HudWidgets;
import dev.threevhy.client.module.ChatTweaks;
import dev.threevhy.client.module.ConfirmDisconnect;
import dev.threevhy.client.module.CustomSkins;
import dev.threevhy.client.module.TextRules;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Runs only in CI: starts the real game with 3vhy loaded, plays a short scripted session and
 * takes screenshots of every 3vhy screen. Fails if one of 3vhy's features didn't work.
 */
public final class ThreevhyGameTest implements FabricClientGameTest {
	/** A pixel-art cover and app icon, standing in for what Windows hands the music helper. */
	private static final String SAMPLE_ART = "iVBORw0KGgoAAAANSUhEUgAAACgAAAAoCAYAAACM/rhtAAADBklEQVR4nO1XXUgUURg9d2asl6DHngoxDAtNa1ERXVxdMPvDyPIhDPpdwxSpBC2wCMk0ykgN08oW7EE0IzJSBDUxKQ1jSyhDiiAIfAkFCXwQe5p1Z+69M/furroPHfhg9/s535l755u5Q3ZsKVxCBEMhREEwNvy5XTpXtMaQFx99cgkAhnxtAICspFPCVzfka5PKDwYkIeaM8BYPfnyI7N1nV0SIzm3uQXZuLQrrPTgw8QBuxzlhvx1IYmzxqg/Jh09XkJxYI5RLdm0rXRWBY74Kbiw1qY4bU4iiItD6xxtg9okar9ZKnC6ex6kRohqSc9PKYfaJIpRaXh21gmbre3c76BXVTUggr19y/OUVvQdHx88L56an3Kd8ikIUyNjrtzVS+c7UZiFxztRmZr0mugU6DmZeE942GfA4FUJUrLSJoGf4OrPWdkhk7eWbq5TPleG1FOfK8CIv+wabM91xU3pIXgxU4pC7VrbMj6GR48hytgvxEWfyrYg+D1IPagDo7r8IAMjPqV9tPRSoKX7WV4YjufcA8CdLFIFcMjUAljW40hr9W9zVW4Kje5tCEhVuGKa4YH+z8LR29ZaELfeH2sOPuzNa1nxIptGNWOQzY1IfTR2vPEF9YFnZNLr9IllxqVfdsbwn4X/NLS7/ZHFriqYFzf114Sm2ry8MqT4Q3xY7KD5Ni2ILnJz3ImHDCesOCwCvXggLtMvMp6jrNLAMANOv2+S8138hVnl29WaY+TQ1it73iZkWAIDvzyM4NhXZLgSLQ0dFtZsTyTH8q6saQOvjw/Ccfm7wa/pqcZsz4uO/jCffiZkWpGxePjmXV2VacrJQUe3G999zVD9NDdjzpoZ9cBzYQ4lJiymzbaBGabhQmS4tzAz94u7WjgIAyPsvs/4HdWP9GEam7lBFzrhL/t+suI7O1s6QBZphEFjgKeAmuhLob9viUkfYBZlhEGhGXPRGW4Kpn3MhCbDrwZwQEWHmXFmhoj0MAmWE8RraCZXtQWb/Lq35acYKyloLsMN/gaEi4gX+A2HP4KtuJtSoAAAAAElFTkSuQmCC";
	private static final String SAMPLE_ICON = "iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAb0lEQVR4nMWTQQ6AIAwEp8SHyi/kBfIL+SleIBJojJSDc2pId9k2IBTyfmQmkBQFwFnErUYs4ha3IgbYPnVd51P7sJigNZsy8GG43ZZgeYQOfYndnG+MCSbEuoGyKPWs8P9LdPVXWZAUxdXCIga4AUM9HdZO1//vAAAAAElFTkSuQmCC";
	/** A song as the Windows helper reports it, since CI can't play real music. */
	private static final String SAMPLE_SONG = "{\"key\":\"test\",\"app\":\"Spotify.exe\",\"title\":\"Snowfall\","
			+ "\"artist\":\"3vhy Test Band\",\"album\":\"CI\",\"playing\":true,\"position\":61,\"duration\":235,\"volume\":64,\"muted\":false,"
			+ "\"art\":\"" + SAMPLE_ART + "\",\"icon\":\"" + SAMPLE_ICON + "\"}";
	/** The same, paused with the sound muted, for the paused close-up. */
	private static final String PAUSED_SONG = "{\"key\":\"test2\",\"app\":\"Spotify.exe\",\"title\":\"Midnight Drive\","
			+ "\"artist\":\"Pixel Lights\",\"album\":\"CI\",\"playing\":false,\"position\":134,\"duration\":201,\"volume\":0,\"muted\":true,"
			+ "\"art\":\"" + SAMPLE_ART + "\",\"icon\":\"" + SAMPLE_ICON + "\"}";
	/** An old-style 64x32 skin, to check Skin Protect converts it like Minecraft does. */
	private static final String LEGACY_SKIN = "iVBORw0KGgoAAAANSUhEUgAAAEAAAAAgCAYAAACinX6EAAAASUlEQVR42u3QAQkAIBAEwRPNYSbDGu9Bg/wsbIEZ+9w3k3R9JZXOAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACADn2s5CHgcZHXdQAAAABJRU5ErkJggg==";

	/** Just the Music HUD, half again its size, low on the 3vhy sky (clear of toasts), for close-up screenshots. */
	private static final class MusicShowcase extends CompatScreen {
		MusicShowcase() {
			super(Component.literal("Music HUD"));
		}

		@Override
		protected void drawBackdrop(Draw d) {
			MainMenuScreen.drawBackdrop(d, this.width, this.height);
		}

		@Override
		protected void draw(Draw d, int mouseX, int mouseY, float delta) {
			Minecraft mc = Minecraft.getInstance();
			HudWidget widget = HudWidgets.MUSIC;
			float scale = 1.5f;
			int w = Math.round(widget.width(mc) * scale);
			int h = Math.round(widget.height(mc) * scale);
			d.push();
			d.translate((this.width - w) / 2f, Math.max(4, this.height - h - 10));
			d.scale(scale);
			widget.draw(d, 0, 0, mc, false);
			d.pop();
		}
	}

	/** How many of the chat's wrapped lines contain {@code text}. */
	private static int chatLinesWith(net.minecraft.client.Minecraft client, String text) {
		int count = 0;
		for (String line : ((ChatTweaks.ShownLines) client.gui.getChat()).threevhy$shownLines()) {
			if (line.contains(text)) count++;
		}
		return count;
	}

	/**
	 * Wraps, cuts and measures a line holding the player's name at many widths, the way chat,
	 * tooltips and floating text do. Returns what went wrong, or null.
	 */
	private static String nameWrapProblem(net.minecraft.client.Minecraft client) {
		String own = client.getUser().getName();
		String text = "§6[Practice] §f" + own + " §7- §a12 kills §7- best friends: " + own + ", Steve, " + own;
		net.minecraft.network.chat.Style plain = net.minecraft.network.chat.Style.EMPTY;
		try {
			for (int width = 1; width <= 420; width += 3) {
				client.font.split(net.minecraft.network.chat.Component.literal(text), width);
				for (var line : client.font.getSplitter().splitLines(net.minecraft.network.chat.Component.literal(text), width, plain)) {
					if (line.getString().contains(own)) return "your real name in a wrapped line at width " + width + ": " + line.getString();
				}
				client.font.getSplitter().splitLines(text, width, plain);
				client.font.getSplitter().findLineBreak(text, width, plain);
				if (client.font.substrByWidth(net.minecraft.network.chat.Component.literal(text), width).getString().contains(own)) {
					return "your real name in cut text at width " + width;
				}
				String head = client.font.plainSubstrByWidth(text, width);
				if (!text.startsWith(head)) return "cut text isn't the start of the text at width " + width;
			}
			for (int skip = 0; skip <= text.length(); skip++) {
				int from = skip;
				int[] bad = {-1};
				net.minecraft.util.StringDecomposer.iterateFormatted(text, skip, plain, plain, (index, style, codepoint) -> {
					if (index < from || index + Character.charCount(codepoint) > text.length()) bad[0] = index;
					return true;
				});
				if (bad[0] >= 0) return "position " + bad[0] + " reported when reading from " + skip;
			}
		} catch (RuntimeException e) {
			return e.toString();
		}
		return null;
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		List<String> failed = new ArrayList<>();

		context.waitTicks(40);
		context.runOnClient(client -> Modules.MAIN_MENU_SEASON.set("Always"));
		context.waitTicks(3);
		shot(context, "3vhy-main-menu-october");
		context.runOnClient(client -> Modules.MAIN_MENU_SEASON.set("Never"));
		context.waitTicks(3);
		shot(context, "3vhy-main-menu");
		context.runOnClient(client -> Modules.MAIN_MENU_SEASON.set("Auto"));
		check(failed, "3vhy main menu styles the title screen",
				context.computeOnClient(client -> client.screen instanceof TitleScreen) && Hooks.mainMenuShown);
		check(failed, "main menu hides Minecraft's logo (LogoRenderer)", Hooks.minecraftLogoHidden);
		check(failed, "main menu buttons drawn as 3vhy glass (AbstractWidget)", Hooks.titleButtonsStyled);
		int menuScale = context.computeOnClient(client -> client.getWindow().getGuiScale());
		int[] modsButton = context.computeOnClient(client -> {
			for (AbstractWidget widget : Compat.widgets(client.screen)) {
				if (widget.getMessage().getString().equals(TitleScreenStyle.MODS_LABEL)) {
					return new int[] {widget.getX() + widget.getWidth() / 2, widget.getY() + widget.getHeight() / 2};
				}
			}
			return null;
		});
		check(failed, "main menu has the 3vhy Mods button", modsButton != null);
		check(failed, "3vhy Mods takes Minecraft Realms' place", context.computeOnClient(client -> {
			boolean realmsShown = false;
			int modsRow = -1;
			int multiplayerRow = -1;
			for (AbstractWidget widget : Compat.widgets(client.screen)) {
				if (widget.getMessage().getString().equals(TitleScreenStyle.MODS_LABEL)) modsRow = widget.getY();
				if (widget.getMessage().getContents() instanceof TranslatableContents key) {
					if (key.getKey().equals("menu.online") && widget.visible) realmsShown = true;
					if (key.getKey().equals("menu.multiplayer")) multiplayerRow = widget.getY();
				}
			}
			ThreevhyClient.LOG.info("3vhy-check: main menu rows: multiplayer {}, 3vhy Mods {}, Realms shown {}", multiplayerRow, modsRow,
					realmsShown);
			return !realmsShown && multiplayerRow >= 0 && modsRow > multiplayerRow;
		}));
		if (modsButton != null) {
			context.getInput().setCursorPos(modsButton[0] * menuScale, modsButton[1] * menuScale);
			context.waitTicks(2);
			context.getInput().pressMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
			context.waitTicks(10);
		}
		check(failed, "3vhy Mods button opens the mod menu",
				context.computeOnClient(client -> client.screen instanceof ModMenuScreen || client.screen instanceof ClickGuiScreen));
		context.getInput().pressKey(InputConstants.KEY_ESCAPE);
		context.waitTicks(5);
		check(failed, "closing the mod menu goes back to the main menu",
				context.computeOnClient(client -> client.screen instanceof TitleScreen));
		context.getInput().pressKey(ThreevhyClient.openSettingsKey);
		context.waitTicks(5);
		check(failed, "Right Shift on the main menu opens the mod menu",
				context.computeOnClient(client -> client.screen instanceof ModMenuScreen || client.screen instanceof ClickGuiScreen));
		context.setScreen(() -> new ModMenuScreen(new TitleScreen()));
		context.waitTicks(10);
		shot(context, "3vhy-modmenu-from-menu");
		context.setScreen(() -> new HubScreen(new TitleScreen()));
		context.waitTicks(10);
		shot(context, "3vhy-hub-from-menu");
		context.setScreen(() -> new ClickGuiScreen(new TitleScreen()).expand(Modules.FREELOOK));
		context.waitTicks(10);
		shot(context, "3vhy-panels-from-menu");
		context.setScreen(TitleScreen::new);
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
			String longLine = "3vhy-long-start " + "the quick brown fox jumps over the lazy dog ".repeat(8) + "3vhy-long-end";
			server.runCommand("tellraw @a \"" + longLine + "\"");
			server.runCommand("tellraw @a \"" + longLine + "\"");
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
			check(failed, "a stacked repeat takes only its own lines off the chat", context.computeOnClient(client ->
					chatLinesWith(client, "3vhy chat test") == 1 && chatLinesWith(client, "3vhy-long-start") == 1
							&& chatLinesWith(client, "3vhy-long-end") == 1));
			check(failed, "music HUD shows the playing song", context.computeOnClient(client ->
					MusicBridge.current() != null && MusicBridge.current().title().equals("Snowfall")
							&& HudWidgets.MUSIC.visible(client)));
			check(failed, "music cover uploaded as a texture (NativeImage, DynamicTexture)", Hooks.musicArtUploaded);
			check(failed, "music app icon uploaded as a texture", Hooks.musicIconUploaded);
			server.runCommand("bossbar add threevhy:test \"Boss\"");
			server.runCommand("bossbar set threevhy:test players @a");
			context.waitTicks(10);
			int musicY = context.computeOnClient(client -> HudWidgets.MUSIC.position(client,
					client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight())[1]);
			ThreevhyClient.LOG.info("3vhy-check: music HUD top with a boss bar: {}", musicY);
			check(failed, "music HUD moves below boss bars (BossHealthOverlay)", musicY > 17);
			shot(context, "3vhy-music-under-bossbar");
			server.runCommand("bossbar remove threevhy:test");
			context.setScreen(MusicShowcase::new);
			context.waitTicks(10);
			shot(context, "3vhy-music-closeup");
			context.runOnClient(client -> MusicBridge.simulate(PAUSED_SONG));
			context.waitTicks(10);
			shot(context, "3vhy-music-closeup-paused");
			context.runOnClient(client -> {
				MusicBridge.simulate(SAMPLE_SONG);
				Modules.MUSIC_COLOR.set(0xFFF58AC8);
			});
			context.waitTicks(10);
			shot(context, "3vhy-music-closeup-pink");
			context.runOnClient(client -> Modules.MUSIC_COLOR.set(0xFF8CC4EA));
			context.setScreen(() -> null);
			context.waitTicks(5);
			check(failed, "player list sees who is online", context.computeOnClient(client ->
					Hooks.playerListTicked && PlayerList.isOnline(client.getUser().getName()) && !PlayerList.isOnline("Steve")
							&& HudWidgets.PLAYER_LIST.visible(client)));
			context.runOnClient(client -> PlayerList.askAbout(client.getConnection(), "msg", client.getUser().getName()));
			context.waitTicks(20);
			String checks = context.computeOnClient(client -> PlayerList.describeChecks());
			ThreevhyClient.LOG.info("3vhy-check: player list server checks: {}", checks);
			check(failed, "player list asks the server who's online (/msg suggestions)", context.computeOnClient(client ->
					Hooks.playerListAskedServer && PlayerList.checkedWithServer("Steve") && !PlayerList.seenByServer("Steve")
							&& PlayerList.seenByServer(client.getUser().getName())));

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
			check(failed, "CPS counts clicks (MouseHandler.onButton)", context.computeOnClient(client -> CpsTracker.left() >= 1));
			context.runOnClient(client -> Modules.SWING_SPEED.set(true));
			context.getInput().pressMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
			context.waitTicks(5);
			check(failed, "swing speed changes your arm swing (LivingEntity.getCurrentSwingDuration)", Hooks.swingSpeedRan);
			context.runOnClient(client -> Modules.SWING_SPEED.set(false));
			shot(context, "3vhy-target-hud");
			check(failed, "target HUD picks up who you hit", context.computeOnClient(client -> Target.get() != null));

			// Name Protect (with text changes) and Skin Protect (with a skin file) stay on through freelook,
			// so the third person view shows them
			context.runOnClient(client -> {
				try {
					java.nio.file.Files.createDirectories(CustomSkins.folder());
					java.nio.file.Files.write(CustomSkins.folder().resolve("ci-skin.png"),
							java.util.Base64.getDecoder().decode(LEGACY_SKIN));
				} catch (java.io.IOException e) {
					throw new RuntimeException(e);
				}
				Config.get().skinFile = "ci-skin.png";
				while (!Modules.SKIN_PROTECT_SOURCE.is(CustomSkins.SOURCE_FILE)) Modules.SKIN_PROTECT_SOURCE.next();
				Modules.NAME_PROTECT.set(true);
				Modules.NAME_PROTECT_NAME.set("Streamer");
				TextRules.add("Money: #", "Money: 1T");
				TextRules.add("vhys", "team");
				Modules.SKIN_PROTECT.set(true);
				Compat.chat(net.minecraft.network.chat.Component.literal(client.getUser().getName() + " is streaming"));
				Compat.chat(net.minecraft.network.chat.Component.literal("Money: 500.9T  Team: vhys"));
			});
			context.waitTicks(10);
			shot(context, "3vhy-name-protect");
			check(failed, "name protect swaps your name", context.computeOnClient(client ->
					NameProtect.apply("<" + client.getUser().getName() + "> hi").equals("<Streamer> hi")
							&& NameProtect.apply(client.getUser().getName() + "son").equals(client.getUser().getName() + "son")));
			check(failed, "name protect hook (StringDecomposer)", Hooks.nameProtectRan);
			check(failed, "text changes apply to plain text", context.computeOnClient(client ->
					dev.threevhy.client.module.NameProtect.apply("Money: 500.9T, Team: vhys").equals("Money: 1T, Team: team")));
			check(failed, "text changes apply across coloured pieces", context.computeOnClient(client -> {
				var line = net.minecraft.network.chat.Component.literal("Money: ").withStyle(net.minecraft.ChatFormatting.YELLOW)
						.append(net.minecraft.network.chat.Component.literal("500.9T").withStyle(net.minecraft.ChatFormatting.GREEN));
				return TextRules.apply(line).getString().equals("Money: 1T") && Hooks.textRuleRan;
			}));
			// A swapped name longer or shorter than the real one, in text that gets wrapped and cut, like
			// the text floating above players on CrystalPractice (which crashed 3vhy 0.4.1)
			for (String shownAs : new String[] {"A much longer streamer name", "Me"}) {
				context.runOnClient(client -> Modules.NAME_PROTECT_NAME.set(shownAs));
				context.waitTicks(2);
				String problem = context.computeOnClient(ThreevhyGameTest::nameWrapProblem);
				ThreevhyClient.LOG.info("3vhy-check: wrapping text with your name shown as \"{}\": {}", shownAs,
						problem == null ? "fine" : problem);
				check(failed, "name protect with a " + (shownAs.length() > 8 ? "longer" : "shorter") + " name wraps and cuts text safely",
						problem == null);
			}
			context.runOnClient(client -> Modules.NAME_PROTECT_NAME.set("A much longer streamer name"));
			context.runOnClient(client -> client.player.setXRot(0f));
			context.waitTicks(2);
			String ownName = context.computeOnClient(client -> client.getUser().getName());
			server.runCommand("execute at @p run summon text_display ^ ^1.6 ^3 {line_width:40,billboard:\"center\",text:\"[Practice] "
					+ ownName + " - 12 kills - " + ownName + "\"}");
			context.waitTicks(10);
			shot(context, "3vhy-name-protect-wrapped");
			check(failed, "floating text with your name in it draws (text display, line wrapping)",
					context.computeOnClient(client -> Hooks.wrapProtectRan && client.level != null));
			server.runCommand("kill @e[type=text_display]");
			context.runOnClient(client -> {
				Modules.NAME_PROTECT_NAME.set("Streamer");
				client.player.setXRot(10f);
			});
			context.waitTicks(2);
			check(failed, "skin protect wears a 64x32 skin file", context.computeOnClient(client ->
					Hooks.customSkinLoaded && CustomSkins.current() != null));

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
				Modules.SKIN_PROTECT_PLAYER.set("jeb_");
				while (!Modules.SKIN_PROTECT_SOURCE.is(CustomSkins.SOURCE_PLAYER)) Modules.SKIN_PROTECT_SOURCE.next();
			});
			context.waitTicks(100);
			String playerSkinStatus = context.computeOnClient(client -> CustomSkins.status());
			ThreevhyClient.LOG.info("3vhy-check: skin by player name: {}", playerSkinStatus);
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
			check(failed, "name tag in the corner of the inventory", Hooks.menuNameDrawn);
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
			context.runOnClient(client -> TextRules.add("Kills: #", "Kills: 9999"));
			context.setScreen(() -> new ModMenuScreen(null).openModule(Modules.NAME_PROTECT));
			context.waitTicks(10);
			shot(context, "3vhy-modmenu-name-protect");
			context.setScreen(() -> new ModMenuScreen(null).openModule(Modules.SKIN_PROTECT));
			context.waitTicks(10);
			shot(context, "3vhy-modmenu-skin-protect");
			context.setScreen(() -> new ModMenuScreen(null).openModule(Modules.MUSIC));
			context.waitTicks(10);
			shot(context, "3vhy-modmenu-music");
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

			// The Panels style of the mod menu
			context.runOnClient(client -> Modules.THEME_MENU_STYLE.set("Panels"));
			check(failed, "Theme > Mod menu > Panels opens the panels menu",
					context.computeOnClient(client -> ModMenuScreen.create(null) instanceof ClickGuiScreen));
			// GUI scale 1 in this small test window looks like a 1080p screen at GUI scale 2
			int guiScaleBefore = context.computeOnClient(client -> client.options.guiScale().get());
			context.runOnClient(client -> {
				client.options.guiScale().set(1);
				Compat.resizeGui(client);
			});
			context.setScreen(() -> new ClickGuiScreen(null));
			context.waitTicks(10);
			shot(context, "3vhy-panels");
			context.setScreen(() -> new ClickGuiScreen(null).expand(Modules.FREELOOK).expand(Modules.CROSSHAIR));
			context.waitTicks(10);
			shot(context, "3vhy-panels-settings");
			context.setScreen(() -> new ClickGuiScreen(null).search("totem"));
			context.waitTicks(5);
			shot(context, "3vhy-panels-search");
			context.runOnClient(client -> Modules.THEME_PANEL_COLOR.set(0xFF5BC8E6));
			context.setScreen(() -> new ClickGuiScreen(null).expand(Modules.SWING_SPEED));
			context.waitTicks(10);
			shot(context, "3vhy-panels-cyan");
			context.runOnClient(client -> {
				Modules.THEME_PANEL_COLOR.reset();
				client.options.guiScale().set(guiScaleBefore);
				Compat.resizeGui(client);
			});
			context.runOnClient(client -> Modules.THEME_MENU_STYLE.set("Classic"));

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
				context.computeOnClient(client -> client.screen instanceof TitleScreen));
		int freezesBefore = FreezeWatch.freezes();
		context.runOnClient(client -> {
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});
		context.waitTicks(10);
		check(failed, "freeze watch logs what a frozen game was doing", FreezeWatch.freezes() > freezesBefore);
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
