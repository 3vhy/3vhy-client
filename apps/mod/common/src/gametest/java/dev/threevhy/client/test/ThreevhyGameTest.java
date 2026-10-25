package dev.threevhy.client.test;

import com.mojang.blaze3d.platform.InputConstants;
import dev.threevhy.client.Config;
import dev.threevhy.client.Draw;
import dev.threevhy.client.Hooks;
import dev.threevhy.client.ThreevhyClient;
import dev.threevhy.client.compat.Compat;
import dev.threevhy.client.compat.CompatScreen;
import dev.threevhy.client.gui.HudEditorScreen;
import dev.threevhy.client.gui.TitleScreenStyle;
import dev.threevhy.client.gui.menu.MenuScreen;
import dev.threevhy.client.gui.menu.OrbitScreen;
import dev.threevhy.client.gui.UiFont;
import dev.threevhy.client.gui.PauseMenuButton;
import dev.threevhy.client.FreezeWatch;
import dev.threevhy.client.hud.CpsTracker;
import dev.threevhy.client.hud.HudWidget;
import dev.threevhy.client.hud.HudWidgets;
import dev.threevhy.client.module.ChatTweaks;
import dev.threevhy.client.module.ConfirmDisconnect;
import dev.threevhy.client.module.CustomSkins;
import dev.threevhy.client.module.TextHotkeys;
import dev.threevhy.client.module.TextRules;
import dev.threevhy.client.module.Freelook;
import dev.threevhy.client.module.Knives;
import dev.threevhy.client.knife.KnifeModel;
import dev.threevhy.client.knife.KnifeRenderer;
import dev.threevhy.client.module.Module;
import dev.threevhy.client.module.Module.Category;
import dev.threevhy.client.module.Modules;
import dev.threevhy.client.module.NameProtect;
import dev.threevhy.client.module.PlayerList;
import dev.threevhy.client.module.StaffList;
import dev.threevhy.client.module.PopTracker;
import dev.threevhy.client.module.Target;
import dev.threevhy.client.music.Lyrics;
import dev.threevhy.client.music.MusicBridge;
import dev.threevhy.client.ui.Sky;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
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

	/**
	 * One HUD widget on its own, half again its size, for close-up screenshots: the music HUD low on
	 * the 3vhy sky (clear of toasts), the others on plain grey like 67's pictures, the module list in
	 * the top right corner where it sits in game.
	 */
	private static final class MusicShowcase extends CompatScreen {
		private final HudWidget widget;
		private final List<HudWidget> row;

		MusicShowcase() {
			this(HudWidgets.MUSIC);
		}

		MusicShowcase(HudWidget widget, HudWidget... more) {
			super(Component.literal("HUD close-up"));
			this.widget = widget;
			List<HudWidget> all = new ArrayList<>();
			all.add(widget);
			all.addAll(List.of(more));
			this.row = all;
		}

		@Override
		protected void drawBackdrop(Draw d) {
			if (widget == HudWidgets.MUSIC) {
				float unit = Math.min(this.width / 1600f, this.height / 900f);
				d.push();
				d.scale(unit);
				Sky.backdrop(d, this.width / unit, this.height / unit, false, 1);
				d.pop();
			} else {
				d.fill(0, 0, this.width, this.height, 0xFF4B4C4F);
			}
		}

		@Override
		protected void draw(Draw d, int mouseX, int mouseY, float delta) {
			Minecraft mc = Minecraft.getInstance();
			float scale = 1.5f;
			if (row.size() > 1) {
				int gap = 4;
				float total = -gap;
				for (HudWidget each : row) total += each.width(mc) + gap;
				d.push();
				d.translate((this.width - total * scale) / 2f, (this.height - widget.height(mc) * scale) / 2f);
				d.scale(scale);
				int x = 0;
				for (HudWidget each : row) {
					each.draw(d, x, 0, mc, true);
					x += each.width(mc) + gap;
				}
				d.pop();
				return;
			}
			int w = Math.round(widget.width(mc) * scale);
			int h = Math.round(widget.height(mc) * scale);
			d.push();
			if (widget == HudWidgets.MODULE_LIST) {
				d.translate(this.width - widget.width(mc) * scale - 6, 6);
			} else if (widget == HudWidgets.MUSIC) {
				d.translate((this.width - w) / 2f, Math.max(4, this.height - h - 10));
			} else if (widget == HudWidgets.STAFF_LIST || widget == HudWidgets.PLAYER_LIST || widget == HudWidgets.POPS) {
				d.translate((this.width - w) / 2f, 40);
			} else {
				d.translate((this.width - w) / 2f, (this.height - h) / 2f);
			}
			d.scale(scale);
			widget.draw(d, 0, 0, mc, widget != HudWidgets.MUSIC);
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
		String focus = System.getProperty("threevhy.testFocus", "");
		if (!focus.isEmpty()) {
			runFocused(context, focus, failed);
			if (!failed.isEmpty()) throw new AssertionError("3vhy checks that failed: " + String.join(", ", failed));
			return;
		}

		context.waitTicks(40);
		shot(context, "3vhy-main-menu");
		int[] fontWidths = context.computeOnClient(client -> new int[] {
				client.font.width(UiFont.bold("PANELS AND MUSIC")),
				client.font.width(Component.literal("PANELS AND MUSIC").withStyle(net.minecraft.ChatFormatting.BOLD))});
		ThreevhyClient.LOG.info("3vhy-check: \"PANELS AND MUSIC\" is {} wide in the clean font, {} in Minecraft's bold", fontWidths[0],
				fontWidths[1]);
		check(failed, "clean font (Inter) loads from the mod's resources", fontWidths[0] > 20 && fontWidths[0] != fontWidths[1]);
		check(failed, "3vhy main menu styles the title screen",
				context.computeOnClient(client -> client.screen instanceof TitleScreen) && Hooks.mainMenuShown);
		check(failed, "3vhy logo in place of Minecraft's (LogoRenderer)", Hooks.logoDrawn);
		check(failed, "Minecraft's buttons drawn as violet glass pills (AbstractWidget)", Hooks.buttonsStyled);
		check(failed, "main menu's round buttons drawn by 3vhy", Hooks.titleButtonsStyled);
		int menuScale = context.computeOnClient(client -> client.getWindow().getGuiScale());
		check(failed, "main menu keeps Singleplayer, Multiplayer, Realms, Options and Quit", context.computeOnClient(client -> {
			int found = 0;
			for (AbstractWidget widget : Compat.widgets(client.screen)) {
				if (widget.visible && widget.getMessage().getContents() instanceof TranslatableContents key
						&& List.of("menu.singleplayer", "menu.multiplayer", "menu.online", "menu.options", "menu.quit").contains(key.getKey())) {
					found++;
				}
			}
			return found == 5;
		}));
		click(context, menuScale, "Scene");
		check(failed, "the Scene button opens the scene panel", context.computeOnClient(client -> find(client, "Time of day: Dusk") != null));
		shot(context, "3vhy-main-menu-scene-panel");
		click(context, menuScale, "Time of day: Dusk");
		check(failed, "the scene panel picks the time of day", context.computeOnClient(client -> Modules.MAIN_MENU_TIME.is("Dusk")));
		context.waitTicks(10);
		shot(context, "3vhy-main-menu-dusk");
		click(context, menuScale, "Scene: Night sky");
		context.waitTicks(10);
		check(failed, "the scene panel picks the scene", context.computeOnClient(client -> Modules.MAIN_MENU_SCENE.is("Night sky")));
		shot(context, "3vhy-main-menu-night-sky");
		click(context, menuScale, "Scene: October");
		context.waitTicks(10);
		shot(context, "3vhy-main-menu-october");
		click(context, menuScale, "Scene: Panorama");
		click(context, menuScale, "Time of day: Night");
		click(context, menuScale, "Camera: Still");
		context.waitTicks(10);
		check(failed, "Camera Still stops the panorama turning", context.computeOnClient(client -> !TitleScreenStyle.panoramaSpins()));
		shot(context, "3vhy-main-menu-night");
		click(context, menuScale, "Scene");
		context.runOnClient(client -> {
			Modules.MAIN_MENU_TIME.reset();
			Modules.MAIN_MENU_CAMERA.reset();
			Modules.MAIN_MENU_SCENE.reset();
		});
		context.waitTicks(10);
		check(failed, "closing the scene panel hides its buttons", context.computeOnClient(client -> find(client, "Time of day: Dusk") == null));

		click(context, menuScale, "3vhy Client");
		context.waitTicks(10);
		check(failed, "the 3vhy button on the main menu opens the Orbit", context.computeOnClient(client -> client.screen instanceof OrbitScreen));
		context.waitTicks(30);
		shot(context, "3vhy-orbit-from-menu");
		context.getInput().pressKey(InputConstants.KEY_RETURN);
		context.waitTicks(30);
		check(failed, "Enter in the Orbit opens the menu at that tab", context.computeOnClient(client ->
				client.screen instanceof MenuScreen menu && menu.view().category() == Category.RENDER));
		shot(context, "3vhy-menu-from-menu");
		context.getInput().pressKey(InputConstants.KEY_ESCAPE);
		context.waitTicks(5);
		check(failed, "Esc in the menu goes back to the Orbit", context.computeOnClient(client -> client.screen instanceof OrbitScreen));
		context.getInput().pressKey(InputConstants.KEY_ESCAPE);
		context.waitTicks(5);
		check(failed, "Esc in the Orbit goes back to the main menu", context.computeOnClient(client -> client.screen instanceof TitleScreen));
		context.getInput().pressKey(ThreevhyClient.openSettingsKey);
		context.waitTicks(5);
		check(failed, "Right Shift on the main menu opens the Orbit", context.computeOnClient(client -> client.screen instanceof OrbitScreen));
		context.setScreen(() -> new HudEditorScreen(new TitleScreen()));
		context.waitTicks(10);
		shot(context, "3vhy-edit-hud-from-menu");
		context.setScreen(() -> Compat.optionsScreen(new TitleScreen()));
		context.waitTicks(10);
		shot(context, "3vhy-options-buttons");
		context.setScreen(TitleScreen::new);
		context.waitTicks(5);
		glassTitleScreens(context, failed);

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
				Modules.MENU_NAME_HUD.set(true);
				Modules.POTIONS.set(true);
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
			server.runCommand("effect give @a speed 90 1");
			server.runCommand("effect give @a strength 40 0");
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
			check(failed, "menu logo on the HUD with Show in game, above the music HUD", context.computeOnClient(client -> {
				int w = client.getWindow().getGuiScaledWidth();
				int h = client.getWindow().getGuiScaledHeight();
				int logoBottom = HudWidgets.MENU_LOGO.position(client, w, h)[1] + HudWidgets.MENU_LOGO.height(client);
				return HudWidgets.MENU_LOGO.shown() && HudWidgets.MENU_LOGO.visible(client)
						&& logoBottom <= HudWidgets.MUSIC.position(client, w, h)[1];
			}));
			server.runCommand("bossbar add threevhy:test \"Boss\"");
			server.runCommand("bossbar set threevhy:test players @a");
			context.waitTicks(10);
			int musicY = context.computeOnClient(client -> HudWidgets.MUSIC.position(client,
					client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight())[1]);
			ThreevhyClient.LOG.info("3vhy-check: music HUD top with a boss bar: {}", musicY);
			check(failed, "music HUD moves below boss bars (BossHealthOverlay)", musicY > 17);
			shot(context, "3vhy-music-under-bossbar");
			server.runCommand("bossbar remove threevhy:test");
			context.setScreen(() -> new MusicShowcase(HudWidgets.MENU_LOGO));
			context.waitTicks(5);
			shot(context, "3vhy-menu-logo-closeup");
			context.runOnClient(client -> Modules.MENU_NAME_SHOW.set("Logo and name"));
			context.waitTicks(5);
			shot(context, "3vhy-menu-logo-closeup-name");
			context.runOnClient(client -> {
				Modules.MENU_NAME_SHOW.reset();
				Modules.MENU_NAME_HUD.reset();
			});
			check(failed, "menu logo leaves the HUD when Show in game is off", context.computeOnClient(client ->
					!HudWidgets.MENU_LOGO.shown()));
			context.runOnClient(client -> {
				Modules.CPS.sliderSetting("background").reset();
				Modules.THEME_PANEL_COLOR.set(0xFFFF5A5F);
			});
			context.setScreen(() -> new MusicShowcase(HudWidgets.FPS, HudWidgets.CPS, HudWidgets.PING));
			context.waitTicks(5);
			shot(context, "3vhy-stats-closeup");
			context.runOnClient(client -> Modules.THEME_PANEL_COLOR.reset());
			context.setScreen(() -> new MusicShowcase(HudWidgets.FPS, HudWidgets.CPS, HudWidgets.PING));
			context.waitTicks(5);
			shot(context, "3vhy-stats-closeup-violet");
			context.setScreen(() -> new MusicShowcase(HudWidgets.COORDS, HudWidgets.CLOCK, HudWidgets.TPS));
			context.waitTicks(5);
			shot(context, "3vhy-coords-closeup");
			context.setScreen(() -> new MusicShowcase(HudWidgets.TARGET));
			context.waitTicks(15);
			shot(context, "3vhy-target-closeup");
			context.setScreen(() -> new MusicShowcase(HudWidgets.POTIONS, HudWidgets.ARMOR));
			context.waitTicks(5);
			shot(context, "3vhy-potions-closeup");
			check(failed, "potion effects widget lists your effects", context.computeOnClient(client -> HudWidgets.POTIONS.visible(client)));
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
			context.runOnClient(client -> Modules.MUSIC_COLOR.reset());
			context.setScreen(() -> new MusicShowcase(HudWidgets.KEYSTROKES));
			context.runOnClient(client -> {
				client.getToastManager().clear();
				Modules.KEYSTROKES.sliderSetting("background").reset();
				client.options.keyAttack.setDown(true);
			});
			context.waitTicks(5);
			shot(context, "3vhy-keystrokes-closeup");
			context.runOnClient(client -> {
				client.options.keyAttack.setDown(false);
				client.options.keyUse.setDown(true);
				client.options.keyJump.setDown(true);
				Modules.KEYSTROKES_PRESSED.set(0xFFFF5FA2);
			});
			context.waitTicks(5);
			shot(context, "3vhy-keystrokes-closeup-pink");
			check(failed, "keystrokes pressed colour can be changed", Modules.KEYSTROKES_PRESSED.get() == 0xFFFF5FA2);
			context.runOnClient(client -> {
				client.options.keyUse.setDown(false);
				client.options.keyJump.setDown(false);
				Modules.KEYSTROKES_PRESSED.reset();
			});
			List<String> listedPlayers = context.computeOnClient(client -> List.copyOf(PlayerList.names()));
			context.runOnClient(client -> {
				PlayerList.names().clear();
				Modules.THEME_PANEL_COLOR.set(0xFFFF5FA2);
			});
			context.setScreen(() -> new MusicShowcase(HudWidgets.PLAYER_LIST));
			context.waitTicks(5);
			shot(context, "3vhy-player-list-closeup");
			context.runOnClient(client -> {
				PlayerList.names().addAll(listedPlayers);
				for (String name : List.of("OGSummer", "BobIsFound", "Frenk_btw", "Munkerlich", "Napooo_", "U_vv")) {
					PlayerList.names().add(name);
				}
			});
			context.waitTicks(3);
			shot(context, "3vhy-player-list-closeup-joining");
			context.waitTicks(12);
			shot(context, "3vhy-player-list-closeup-more");
			check(failed, "player list shows six and +N MORE", context.computeOnClient(client ->
					PlayerList.names().size() == listedPlayers.size() + 6 && HudWidgets.PLAYER_LIST.visible(client)));
			context.runOnClient(client -> {
				PlayerList.names().clear();
				PlayerList.names().addAll(listedPlayers);
			});
			boolean popsOn = context.computeOnClient(client -> Modules.POPS.enabled());
			context.runOnClient(client -> Modules.POPS.set(false));
			context.setScreen(() -> new MusicShowcase(HudWidgets.POPS));
			context.waitTicks(5);
			shot(context, "3vhy-pops-closeup");
			context.runOnClient(client -> Modules.POPS.set(popsOn));
			context.setScreen(() -> new MusicShowcase(HudWidgets.STAFF_LIST));
			context.waitTicks(10);
			shot(context, "3vhy-staff-list-closeup");
			context.runOnClient(client -> {
				StaffList.simulate("Frenk_btw", "owner", false);
				StaffList.simulate("Kai", "mod", false);
			});
			context.waitTicks(4);
			shot(context, "3vhy-staff-list-closeup-joining");
			context.waitTicks(30);
			context.runOnClient(client -> StaffList.simulate("Kai", "mod", true));
			context.waitTicks(12);
			shot(context, "3vhy-staff-list-closeup-spectating");
			check(failed, "staff list sorts by rank and marks spectators", context.computeOnClient(client -> {
				List<StaffList.Staff> staff = StaffList.current(client);
				return staff.size() == 2 && staff.get(0).rank().equals("OWNER") && staff.get(1).spectating();
			}));
			context.runOnClient(client -> {
				StaffList.leave("Frenk_btw");
				StaffList.leave("Kai");
			});
			context.waitTicks(3);
			shot(context, "3vhy-staff-list-closeup-leaving");
			context.waitTicks(20);
			shot(context, "3vhy-staff-list-closeup-clear");
			check(failed, "staff list drawn", Hooks.staffListDrawn);
			check(failed, "staff ranks read from the tab list", "mod".equals(StaffList.rankOf("[Sr.Mod] Kai"))
					&& "admin".equals(StaffList.rankOf("§c§lADMIN §fAlex")) && StaffList.rankOf("[VIP] Steve") == null);
			context.runOnClient(client -> StaffList.stopSimulating());
			context.setScreen(() -> new MusicShowcase(HudWidgets.MODULE_LIST));
			context.waitTicks(5);
			shot(context, "3vhy-module-list-closeup");
			context.runOnClient(client -> Modules.THEME_PANEL_COLOR.reset());
			check(failed, "module list shows the mods that are on", context.computeOnClient(client ->
					HudWidgets.MODULE_LIST.visible(client) && HudWidgets.MODULE_LIST.height(client) >= 8 * 5));
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

			skyLyrics(context, singleplayer, failed);
			context.runOnClient(client -> {
				MusicBridge.simulate(SAMPLE_SONG);
				Lyrics.simulate(MusicBridge.current(), "[00:50.00] Snow falls on the quiet town\n[00:55.00] Every light is burning down\n"
						+ "[01:05.00] Hold on, we're almost home\n[01:20.00] Under violet skies we roam");
			});
			context.setScreen(MusicShowcase::new);
			context.waitTicks(12);
			shot(context, "3vhy-music-closeup-lyrics");
			context.setScreen(() -> null);
			context.runOnClient(client -> {
				Modules.HOTBAR.set(true);
				client.getToastManager().clear();
			});
			context.waitTicks(5);
			shot(context, "3vhy-hotbar");
			check(failed, "the glass hotbar replaces Minecraft's", Hooks.hotbarDrawn);
			check(failed, "the hotbar shows where you're playing", Hooks.serverAddressDrawn);
			context.runOnClient(client -> Modules.HOTBAR_SELECTED.set("Accent"));
			context.getInput().pressKey(InputConstants.KEY_3);
			context.waitTicks(5);
			shot(context, "3vhy-hotbar-accent");
			context.getInput().pressKey(InputConstants.KEY_1);
			context.runOnClient(client -> Modules.HOTBAR_SELECTED.reset());
			check(failed, "lyrics read from LRC", Lyrics.parse("[00:01.50] a\n[00:03.00][00:09.00] b").size() == 3);
			context.runOnClient(client -> Modules.LYRICS.set(false));
			knives(context, singleplayer, failed, false);
			glassWorldScreens(context, singleplayer, failed);
			context.runOnClient(client -> client.player.setXRot(10f));

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
			check(failed, "swing speed slows pulling out an item (ItemInHandRenderer.tick)", Hooks.equipSpeedRan);
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

			context.runOnClient(client -> {
				TextHotkeys.add(GLFW.GLFW_KEY_F6, false, "/me 3vhy hotkey command");
				TextHotkeys.add(GLFW.GLFW_KEY_G, false, "3vhy hotkey chat");
				TextHotkeys.add(GLFW.GLFW_MOUSE_BUTTON_4, true, "gg");
				TextHotkeys.update(client, hotkey -> false);
				TextHotkeys.update(client, hotkey -> !hotkey.mouse && hotkey.key == GLFW.GLFW_KEY_F6);
				TextHotkeys.update(client, hotkey -> !hotkey.mouse && hotkey.key == GLFW.GLFW_KEY_F6);
				TextHotkeys.update(client, hotkey -> false);
				TextHotkeys.update(client, hotkey -> !hotkey.mouse && hotkey.key == GLFW.GLFW_KEY_G);
			});
			context.waitTicks(10);
			check(failed, "text hotkeys run a command once per press", context.computeOnClient(client ->
					chatLinesWith(client, "3vhy hotkey command") == 1));
			check(failed, "text hotkeys send a chat message", context.computeOnClient(client ->
					chatLinesWith(client, "3vhy hotkey chat") == 1));
			check(failed, "text hotkeys turn the mod on and name their keys", context.computeOnClient(client ->
					Modules.TEXT_HOTKEYS.enabled() && Config.get().textHotkeys.size() == 3
							&& TextHotkeys.keyName(GLFW.GLFW_KEY_F6, false).equals("F6")
							&& TextHotkeys.keyName(GLFW.GLFW_MOUSE_BUTTON_4, true).equals("Mouse 4")));

			context.getInput().pressKey(ThreevhyClient.openSettingsKey);
			context.waitTicks(30);
			shot(context, "3vhy-orbit");
			check(failed, "Right Shift opens the Orbit", context.computeOnClient(client -> client.screen instanceof OrbitScreen));
			context.getInput().pressKey(InputConstants.KEY_LEFT);
			context.getInput().pressKey(InputConstants.KEY_LEFT);
			context.waitTicks(30);
			check(failed, "the arrow keys turn the Orbit", context.computeOnClient(client ->
					client.screen instanceof OrbitScreen orbit && orbit.view().front() == 0));
			shot(context, "3vhy-orbit-profile");
			context.getInput().pressKey(InputConstants.KEY_RIGHT);
			context.getInput().pressKey(InputConstants.KEY_RIGHT);
			context.waitTicks(30);
			context.getInput().pressKey(InputConstants.KEY_RETURN);
			context.waitTicks(30);
			check(failed, "the Orbit opens the tab at the front", context.computeOnClient(client ->
					client.screen instanceof MenuScreen menu && menu.view().category() == Category.RENDER));
			shot(context, "3vhy-menu");
			boolean zoomBefore = context.computeOnClient(client -> Modules.ZOOM.enabled());
			float[] zoomCard = context.computeOnClient(client -> ((MenuScreen) client.screen).view().cardCenter(Modules.ZOOM));
			check(failed, "the menu lays out a card for each module", zoomCard != null);
			if (zoomCard != null) {
				context.getInput().setCursorPos(zoomCard[0] * menuScale, zoomCard[1] * menuScale);
				context.waitTicks(2);
				context.getInput().pressMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
				context.waitTicks(5);
				check(failed, "clicking a card switches its module", context.computeOnClient(client -> Modules.ZOOM.enabled() != zoomBefore));
				context.getInput().pressMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
				context.waitTicks(5);
				context.getInput().pressMouse(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
				context.waitTicks(15);
				check(failed, "right-clicking a card opens its settings", context.computeOnClient(client ->
						client.screen instanceof MenuScreen menu && menu.view().sheetModule() == Modules.ZOOM));
				shot(context, "3vhy-menu-zoom-settings");
				context.getInput().pressKey(InputConstants.KEY_ESCAPE);
				context.waitTicks(5);
			}
			check(failed, "zoom is back the way it was", context.computeOnClient(client -> Modules.ZOOM.enabled() == zoomBefore));
			context.getInput().typeChars("totem");
			context.waitTicks(10);
			check(failed, "typing in the menu searches", context.computeOnClient(client ->
					client.screen instanceof MenuScreen menu && menu.view().searchText().equals("totem")));
			shot(context, "3vhy-menu-search");

			context.setScreen(() -> new HudEditorScreen(null));
			context.waitTicks(10);
			shot(context, "3vhy-edit-hud");
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
			check(failed, "scrolling over the scoreboard in the HUD editor resizes it",
					context.computeOnClient(client -> Modules.SCOREBOARD_SIZE.get() < sizeBefore));
			context.runOnClient(client -> Modules.SCOREBOARD_SIZE.reset());

			for (Category category : List.of(Category.COMBAT, Category.HUD, Category.COSMETICS, Category.CLIENT)) {
				context.setScreen(() -> new MenuScreen(null, category));
				context.waitTicks(25);
				shot(context, "3vhy-menu-" + category.label.toLowerCase(java.util.Locale.ROOT));
			}
			context.runOnClient(client -> TextRules.add("Kills: #", "Kills: 9999"));
			for (Module module : List.of(Modules.CROSSHAIR, Modules.PLAYER_LIST, Modules.NAME_PROTECT, Modules.TEXT_HOTKEYS,
					Modules.SKIN_PROTECT, Modules.WAYPOINTS, Modules.MUSIC, Modules.LYRICS, Modules.THEME, Modules.MAIN_MENU)) {
				context.setScreen(() -> MenuScreen.settings(null, module));
				context.waitTicks(15);
				shot(context, "3vhy-menu-" + module.id.replace('_', '-'));
			}
			context.runOnClient(client -> List.copyOf(Config.get().textHotkeys).forEach(TextHotkeys::remove));
			context.runOnClient(client -> Config.get().createProfile("Crystal PvP"));
			context.setScreen(() -> MenuScreen.profiles(null));
			context.waitTicks(20);
			shot(context, "3vhy-menu-profiles");
			check(failed, "profiles can be created", context.computeOnClient(client ->
					Config.get().profile.equals("Crystal PvP") && Config.get().profileNames().contains(Config.DEFAULT_PROFILE)));
			context.runOnClient(client -> Config.get().loadProfile(Config.DEFAULT_PROFILE));
			context.runOnClient(client -> Modules.THEME_PANEL_COLOR.set(0xFF5BC8E6));
			context.setScreen(() -> new MenuScreen(null, Category.RENDER));
			context.waitTicks(20);
			shot(context, "3vhy-menu-cyan");
			context.runOnClient(client -> {
				Modules.THEME_PANEL_COLOR.reset();
				Modules.THEME_FONT.set("Minecraft");
			});
			context.setScreen(() -> new MenuScreen(null, Category.RENDER));
			context.waitTicks(20);
			shot(context, "3vhy-menu-minecraft-font");
			context.runOnClient(client -> {
				Modules.THEME_FONT.reset();
				Modules.THEME_RIGHT_SHIFT.set("Menu");
			});
			context.setScreen(() -> null);
			context.waitTicks(3);
			context.getInput().pressKey(ThreevhyClient.openSettingsKey);
			context.waitTicks(5);
			check(failed, "Theme > Right Shift opens > Menu skips the Orbit",
					context.computeOnClient(client -> client.screen instanceof MenuScreen));
			context.runOnClient(client -> Modules.THEME_RIGHT_SHIFT.reset());

			// Pause menu: the 3vhy Client button, then Confirm Disconnect
			context.setScreen(() -> null);
			context.waitTicks(5);
			context.getInput().pressKey(InputConstants.KEY_ESCAPE);
			context.waitTicks(10);
			shot(context, "3vhy-pause-menu");
			int scale = context.computeOnClient(client -> client.getWindow().getGuiScale());
			int[][] pauseButtons = context.computeOnClient(client -> {
				AbstractWidget mods = null;
				AbstractWidget ours = null;
				AbstractWidget stats = null;
				AbstractWidget options = null;
				AbstractWidget disconnect = null;
				AbstractWidget modMenu = null;
				boolean feedback = false;
				for (AbstractWidget widget : Compat.widgets(client.screen)) {
					if (!widget.visible) continue;
					String label = widget.getMessage().getString();
					if (label.equals(PauseMenuButton.LABEL)) mods = widget;
					if (label.equals("Mods")) modMenu = widget;
					if (label.equals(PauseMenuButton.OPTIONS_LABEL)) ours = widget;
					if (ConfirmDisconnect.isDisconnect(widget.getMessage())) disconnect = widget;
					if (widget.getMessage().getContents() instanceof TranslatableContents key) {
						if (key.getKey().equals("gui.stats")) stats = widget;
						if (key.getKey().equals("menu.options")) options = widget;
						if (key.getKey().equals("menu.sendFeedback") || key.getKey().equals("menu.reportBugs")) feedback = true;
					}
				}
				if (mods == null || ours == null || stats == null || options == null || disconnect == null) return null;
				ThreevhyClient.LOG.info("3vhy-check: pause menu: Statistics {}, Mods {} ({} wide), Options {}, 3vhy Options {}, Disconnect {}",
						stats.getY(), mods.getY(), mods.getWidth(), options.getY(), ours.getY(), disconnect.getY());
				AbstractWidget above = modMenu != null && modMenu.getY() > stats.getY() && modMenu.getY() < mods.getY() ? modMenu : stats;
				boolean modsRow = mods.getY() == above.getY() + above.getHeight() + 4 && mods.getX() == disconnect.getX()
						&& mods.getWidth() == disconnect.getWidth();
				boolean optionsRow = ours.getY() == options.getY() && ours.getX() > options.getX();
				return new int[][] {
						{mods.getX() + mods.getWidth() / 2, mods.getY() + mods.getHeight() / 2},
						{ours.getX() + ours.getWidth() / 2, ours.getY() + ours.getHeight() / 2},
						{modsRow ? 1 : 0, optionsRow ? 1 : 0, feedback ? 1 : 0}};
			});
			check(failed, "pause menu has 3vhy Client and 3vhy Options...", pauseButtons != null);
			check(failed, "3vhy Client is a full row right under Advancements and Statistics (or Mod Menu's Mods)", pauseButtons != null && pauseButtons[2][0] == 1);
			check(failed, "3vhy Options... sits beside Options...", pauseButtons != null && pauseButtons[2][1] == 1);
			check(failed, "pause menu leaves out the feedback links", pauseButtons != null && pauseButtons[2][2] == 0);
			if (pauseButtons != null) {
				context.getInput().setCursorPos(pauseButtons[0][0] * scale, pauseButtons[0][1] * scale);
				context.waitTicks(2);
				context.getInput().pressMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
				context.waitTicks(15);
			}
			shot(context, "3vhy-menu-from-pause");
			check(failed, "pause menu's 3vhy Client opens the menu",
					context.computeOnClient(client -> client.screen instanceof MenuScreen));
			context.getInput().pressKey(InputConstants.KEY_ESCAPE);
			context.waitTicks(5);
			check(failed, "closing the mod menu goes back to the pause menu",
					context.computeOnClient(client -> client.screen instanceof PauseScreen));
			if (pauseButtons != null) {
				context.getInput().setCursorPos(pauseButtons[1][0] * scale, pauseButtons[1][1] * scale);
				context.waitTicks(2);
				context.getInput().pressMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
				context.waitTicks(15);
			}
			check(failed, "3vhy Options... opens 3vhy's settings",
					context.computeOnClient(client -> client.screen instanceof MenuScreen menu && menu.view().sheetModule() == Modules.THEME));
			context.getInput().pressKey(InputConstants.KEY_ESCAPE);
			context.waitTicks(5);
			context.getInput().pressKey(InputConstants.KEY_ESCAPE);
			context.waitTicks(5);
			if (context.computeOnClient(client -> !(client.screen instanceof PauseScreen))) {
				context.getInput().pressKey(InputConstants.KEY_ESCAPE);
				context.waitTicks(10);
			}

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

	/** Runs one part of the test on its own (-PtestFocus=...), for quicker checks while working on it. */
	private static void runFocused(ClientGameTestContext context, String focus, List<String> failed) {
		ThreevhyClient.LOG.info("3vhy-check: running only the {} part of the test", focus);
		context.waitTicks(20);
		if (focus.contains("glass")) glassTitleScreens(context, failed);
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			context.waitTicks(100);
			if (focus.contains("glass")) glassWorldScreens(context, singleplayer, failed);
			if (focus.contains("lyrics")) skyLyrics(context, singleplayer, failed);
			if (focus.contains("knives")) knives(context, singleplayer, failed, true);
			if (focus.contains("calibrate")) calibrateKnife(context, singleplayer);
			if (focus.contains("bench")) benchmark(context, singleplayer, 900);
		}
		context.waitTicks(10);
	}

	/** Frame times and memory allocated on the render thread, one sample a frame, taken by a HUD layer this test adds. */
	private static final class FrameRecorder {
		private static final FrameRecorder INSTANCE = new FrameRecorder();
		private final long[] nanos = new long[4096];
		private final long[] bytes = new long[4096];
		private volatile int count = -1;
		private long lastNanos;
		private long lastBytes;
		private boolean registered = false;

		void frame() {
			if (count < 0 || count >= nanos.length) return;
			long now = System.nanoTime();
			long allocated = allocatedBytes();
			if (lastNanos != 0) {
				nanos[count] = now - lastNanos;
				bytes[count] = allocated - lastBytes;
				count++;
			}
			lastNanos = now;
			lastBytes = allocated;
		}

		private static long allocatedBytes() {
			java.lang.management.ThreadMXBean bean = java.lang.management.ManagementFactory.getThreadMXBean();
			return bean instanceof com.sun.management.ThreadMXBean sun ? sun.getCurrentThreadAllocatedBytes() : 0;
		}

		void start() {
			lastNanos = 0;
			count = 0;
		}

		/** {average ms, median ms, 99th percentile ms, average KB allocated a frame, frames}. */
		double[] stop() {
			int n = Math.max(0, count);
			count = -1;
			if (n == 0) return new double[] {0, 0, 0, 0, 0};
			long[] sorted = java.util.Arrays.copyOf(nanos, n);
			java.util.Arrays.sort(sorted);
			double sum = 0;
			double alloc = 0;
			for (int i = 0; i < n; i++) {
				sum += nanos[i];
				alloc += bytes[i];
			}
			return new double[] {sum / n / 1e6, sorted[n / 2] / 1e6, sorted[Math.min(n - 1, (int) Math.ceil(n * 0.99) - 1)] / 1e6, alloc / n / 1024.0, n};
		}
	}

	/**
	 * Measures frame time and render-thread allocation over {@code frames} frames in three set scenes: the
	 * world with every 3vhy mod off, the same with a typical HUD on, and the 3vhy menu open. Logged as
	 * "3vhy-bench" lines, so runs before and after a change can be compared on the same machine.
	 */
	private static void benchmark(ClientGameTestContext context, TestSingleplayerContext singleplayer, int frames) {
		var server = singleplayer.getServer();
		server.runCommand("time set noon");
		server.runCommand("weather clear");
		server.runCommand("scoreboard objectives add bench dummy \"Kills\"");
		server.runCommand("scoreboard objectives setdisplay sidebar bench");
		server.runCommand("scoreboard players set Steve bench 29");
		server.runCommand("scoreboard players set Alex bench 17");
		server.runCommand("effect give @p speed 600 1");
		server.runCommand("effect give @p strength 600 0");
		server.runCommand("item replace entity @p weapon.mainhand with netherite_sword");
		server.runCommand("item replace entity @p weapon.offhand with totem_of_undying");
		server.runCommand("item replace entity @p armor.head with diamond_helmet");
		server.runCommand("item replace entity @p armor.chest with diamond_chestplate");
		context.runOnClient(client -> {
			if (!FrameRecorder.INSTANCE.registered) {
				FrameRecorder.INSTANCE.registered = true;
				Compat.registerHud(net.minecraft.resources.Identifier.fromNamespaceAndPath("threevhy", "bench"), d -> FrameRecorder.INSTANCE.frame());
			}
			client.options.renderDistance().set(8);
			client.options.fov().set(70);
			client.options.cloudStatus().set(net.minecraft.client.CloudStatus.FANCY);
			client.getToastManager().clear();
		});
		face(context, 30, 5);
		context.waitTicks(200);
		List<Module> toggleable = new ArrayList<>();
		for (Module module : Modules.ALL) if (module.toggleable()) toggleable.add(module);
		java.util.Map<Module, Boolean> before = new java.util.HashMap<>();
		context.runOnClient(client -> {
			for (Module module : toggleable) {
				before.put(module, module.enabled());
				module.set(false);
			}
		});
		context.waitTicks(40);
		double[] off = measure(context, frames);
		context.runOnClient(client -> {
			for (Module module : List.of(Modules.FPS, Modules.CPS, Modules.PING, Modules.TPS, Modules.CLOCK, Modules.COORDS, Modules.KEYSTROKES,
					Modules.ARMOR, Modules.POTIONS, Modules.SCOREBOARD, Modules.MUSIC, Modules.MODULE_LIST, Modules.PLAYER_LIST, Modules.TARGET_HUD,
					Modules.CROSSHAIR, Modules.ZOOM, Modules.FREELOOK, Modules.NAME_PROTECT, Modules.CHAT, Modules.MENU_NAME, Modules.LYRICS)) {
				module.set(true);
			}
			Modules.MENU_NAME_HUD.set(true);
			MusicBridge.simulate(SAMPLE_SONG);
			Lyrics.simulate(MusicBridge.current(), "[00:50.00] Snow falls on the quiet town\n[00:55.00] Every light is burning down\n"
					+ "[01:05.00] Hold on, we're almost home\n[01:20.00] Under violet skies we roam\n[02:00.00] Under violet skies we roam");
			PlayerList.add("Steve");
			PlayerList.add(client.getUser().getName());
			TextRules.add("Kills: #", "Kills: 9999");
		});
		context.waitTicks(40);
		double[] hud = measure(context, frames);
		context.setScreen(() -> new MenuScreen(null, Category.HUD));
		context.waitTicks(40);
		double[] menu = measure(context, frames);
		context.setScreen(() -> null);
		context.runOnClient(client -> before.forEach(Module::set));
		report("world, 3vhy mods off", off);
		report("world, typical 3vhy HUD", hud);
		report("3vhy menu open", menu);
		ThreevhyClient.LOG.info("3vhy-bench: 3vhy HUD costs {} ms a frame on average ({} ms at the 99th percentile)",
				String.format(java.util.Locale.ROOT, "%.2f", hud[0] - off[0]), String.format(java.util.Locale.ROOT, "%.2f", hud[2] - off[2]));
	}

	private static double[] measure(ClientGameTestContext context, int frames) {
		context.runOnClient(client -> FrameRecorder.INSTANCE.start());
		int waited = 0;
		while (context.computeOnClient(client -> FrameRecorder.INSTANCE.count) < frames && waited < 20 * 120) {
			context.waitTicks(10);
			waited += 10;
		}
		return context.computeOnClient(client -> FrameRecorder.INSTANCE.stop());
	}

	private static void report(String scene, double[] result) {
		ThreevhyClient.LOG.info("3vhy-bench: {}: {} frames, {} ms average ({} FPS), {} ms median, {} ms 99th percentile ({} FPS 1% low), {} KB allocated a frame",
				scene, (int) result[4], String.format(java.util.Locale.ROOT, "%.2f", result[0]),
				String.format(java.util.Locale.ROOT, "%.1f", 1000 / Math.max(0.001, result[0])), String.format(java.util.Locale.ROOT, "%.2f", result[1]),
				String.format(java.util.Locale.ROOT, "%.2f", result[2]), String.format(java.util.Locale.ROOT, "%.1f", 1000 / Math.max(0.001, result[2])),
				String.format(java.util.Locale.ROOT, "%.1f", result[3]));
	}

	/**
	 * Sky Lyrics, on a platform high above spawn so the sky is open: they're drawn while the sky is, and
	 * then a wall, a tree, a cliff and a closed room each hide them like they hide the sky, while glass
	 * lets them through. The wall and the room are also checked pixel by pixel: the same view with the
	 * lyrics on and off may only differ in the sky.
	 */
	private static void skyLyrics(ClientGameTestContext context, TestSingleplayerContext singleplayer, List<String> failed) {
		var server = singleplayer.getServer();
		String song = "{\"key\":\"sky\",\"app\":\"Spotify.exe\",\"title\":\"Northern Lights\",\"artist\":\"3vhy Test Band\",\"album\":\"CI\","
				+ "\"playing\":false,\"position\":120,\"duration\":240}";
		String lrc = "[01:40.00] The city falls asleep\n[01:58.00] Written in the northern sky\n[02:10.00] Every word becomes a star\n";
		boolean musicWasOn = context.computeOnClient(client -> Modules.MUSIC.enabled());
		boolean warningWasOn = context.computeOnClient(client -> Modules.TOTEM_WARNING.enabled());
		double[] home = context.computeOnClient(client -> new double[] {client.player.getX(), client.player.getY(), client.player.getZ(),
				client.player.getYRot(), client.player.getXRot()});
		int x = (int) Math.floor(home[0]);
		int y = (int) Math.floor(home[1]) + 40;
		int z = (int) Math.floor(home[2]);
		server.runCommand("fill " + (x - 14) + " " + (y - 1) + " " + (z - 6) + " " + (x + 14) + " " + (y - 1) + " " + (z + 24) + " minecraft:smooth_stone");
		server.runCommand("tp @p " + x + ".5 " + y + " " + z + ".5 0 -14");
		server.runCommand("time set noon");
		server.runCommand("weather clear");
		server.runCommand("effect clear @p");
		server.runCommand("item replace entity @p weapon.mainhand with air");
		server.runCommand("item replace entity @p weapon.offhand with air");
		context.runOnClient(client -> {
			client.gui.getChat().clearMessages(false);
			Modules.MUSIC.set(false);
			Modules.TOTEM_WARNING.set(false);
			Modules.LYRICS.set(true);
			Modules.LYRICS_PLACE.set("Sky");
			client.options.cloudStatus().set(net.minecraft.client.CloudStatus.OFF);
			client.getToastManager().clear();
			MusicBridge.simulate(song);
			Lyrics.simulate(MusicBridge.current(), lrc);
		});
		face(context, 0, -14);
		context.waitTicks(40);
		Hooks.lyricsDrawn = false;
		context.waitTicks(5);
		check(failed, "sky lyrics are drawn while the sky is (SkyRenderer)", Hooks.lyricsDrawn);
		shot(context, "3vhy-sky-lyrics-open");

		server.runCommand("fill " + (x + 1) + " " + y + " " + (z + 6) + " " + (x + 14) + " " + (y + 16) + " " + (z + 6) + " minecraft:red_concrete");
		context.waitTicks(10);
		Path walled = shotPath(context, "3vhy-sky-lyrics-behind-wall");
		context.runOnClient(client -> Modules.LYRICS.set(false));
		context.waitTicks(5);
		Path walledOff = shotPath(context, "3vhy-sky-lyrics-behind-wall-off");
		context.runOnClient(client -> Modules.LYRICS.set(true));
		if (walled != null && walledOff != null) {
			int[] diff = compareShots(walled, walledOff);
			ThreevhyClient.LOG.info("3vhy-check: lyrics on vs off: {} of {} wall pixels changed, {} of {} sky pixels changed", diff[0], diff[1], diff[2],
					diff[3]);
			check(failed, "a wall hides the sky lyrics behind it", diff[1] > 2000 && diff[0] <= diff[1] / 400);
			check(failed, "the sky lyrics still show in the open sky beside the wall", diff[2] > 150);
		}
		server.runCommand("fill " + (x + 1) + " " + y + " " + (z + 6) + " " + (x + 14) + " " + (y + 16) + " " + (z + 6) + " minecraft:air");

		server.runCommand("fill " + (x - 4) + " " + y + " " + (z + 7) + " " + (x - 4) + " " + (y + 5) + " " + (z + 7) + " minecraft:oak_log");
		server.runCommand("fill " + (x - 7) + " " + (y + 3) + " " + (z + 5) + " " + (x - 1) + " " + (y + 7) + " " + (z + 9)
				+ " minecraft:oak_leaves[persistent=true] replace minecraft:air");
		context.waitTicks(10);
		shot(context, "3vhy-sky-lyrics-behind-tree");
		server.runCommand("fill " + (x - 7) + " " + y + " " + (z + 5) + " " + (x - 1) + " " + (y + 7) + " " + (z + 9) + " minecraft:air");

		server.runCommand("fill " + (x - 2) + " " + y + " " + (z + 10) + " " + (x + 14) + " " + (y + 5) + " " + (z + 16) + " minecraft:stone");
		server.runCommand("fill " + (x - 14) + " " + y + " " + (z + 10) + " " + (x - 3) + " " + (y + 2) + " " + (z + 16) + " minecraft:stone");
		context.waitTicks(10);
		shot(context, "3vhy-sky-lyrics-behind-cliff");
		server.runCommand("fill " + (x - 14) + " " + y + " " + (z + 10) + " " + (x + 14) + " " + (y + 5) + " " + (z + 16) + " minecraft:air");

		server.runCommand("fill " + (x - 14) + " " + y + " " + (z + 5) + " " + (x + 14) + " " + (y + 20) + " " + (z + 5)
				+ " minecraft:light_blue_stained_glass");
		context.waitTicks(10);
		shot(context, "3vhy-sky-lyrics-through-glass");
		server.runCommand("fill " + (x - 14) + " " + y + " " + (z + 5) + " " + (x + 14) + " " + (y + 20) + " " + (z + 5) + " minecraft:air");

		server.runCommand("fill " + (x - 3) + " " + (y - 1) + " " + (z - 3) + " " + (x + 3) + " " + (y + 4) + " " + (z + 3) + " minecraft:stone");
		server.runCommand("fill " + (x - 1) + " " + y + " " + (z - 1) + " " + (x + 1) + " " + (y + 2) + " " + (z + 1) + " minecraft:air");
		server.runCommand("tp @p " + x + ".5 " + y + " " + z + ".5 0 -14");
		face(context, 0, -14);
		context.waitTicks(10);
		Path inside = shotPath(context, "3vhy-sky-lyrics-indoors");
		context.runOnClient(client -> Modules.LYRICS.set(false));
		context.waitTicks(5);
		Path insideOff = shotPath(context, "3vhy-sky-lyrics-indoors-off");
		context.runOnClient(client -> Modules.LYRICS.set(true));
		if (inside != null && insideOff != null) {
			int[] diff = compareShots(inside, insideOff);
			ThreevhyClient.LOG.info("3vhy-check: indoors, lyrics on vs off: {} of {} pixels changed", diff[0] + diff[2], diff[1] + diff[3]);
			check(failed, "no sky lyrics leak into a closed room", diff[0] + diff[2] <= (diff[1] + diff[3]) / 400);
		}
		server.runCommand("fill " + (x - 3) + " " + (y - 1) + " " + (z - 3) + " " + (x + 3) + " " + (y + 4) + " " + (z + 3) + " minecraft:air");
		server.runCommand("fill " + (x - 14) + " " + (y - 1) + " " + (z - 6) + " " + (x + 14) + " " + (y - 1) + " " + (z + 24) + " minecraft:smooth_stone");
		server.runCommand("tp @p " + x + ".5 " + y + " " + z + ".5 0 -14");
		face(context, 0, -14);

		context.runOnClient(client -> Lyrics.simulate(MusicBridge.current(), "[01:40.00] The city falls asleep\n[01:50.00]\n[02:30.00] And we fly\n"));
		context.waitTicks(15);
		shot(context, "3vhy-sky-lyrics-instrumental");
		context.runOnClient(client -> Lyrics.simulatePlain(MusicBridge.current(),
				"The city falls asleep\nWritten in the northern sky\nEvery word becomes a star\n\nAnd we fly\nOver violet seas\nUntil the morning\n"));
		context.waitTicks(15);
		shot(context, "3vhy-sky-lyrics-unsynced");
		check(failed, "plain lyrics show, spread over the song", context.computeOnClient(client -> Lyrics.view(MusicBridge.current()).state()
				== Lyrics.State.UNSYNCED && !Lyrics.view(MusicBridge.current()).synced() && !Lyrics.lines(MusicBridge.current()).isEmpty()));

		server.runCommand("fill " + (x - 14) + " " + (y - 1) + " " + (z - 6) + " " + (x + 14) + " " + (y - 1) + " " + (z + 24) + " minecraft:air");
		server.runCommand(String.format(java.util.Locale.ROOT, "tp @p %.4f %.4f %.4f %.2f %.2f", home[0], home[1], home[2], home[3], home[4]));
		server.runCommand("item replace entity @p weapon.mainhand with netherite_sword");
		server.runCommand("item replace entity @p weapon.offhand with totem_of_undying");
		context.runOnClient(client -> {
			Modules.MUSIC.set(musicWasOn);
			Modules.TOTEM_WARNING.set(warningWasOn);
			Modules.LYRICS_PLACE.reset();
			client.options.cloudStatus().set(net.minecraft.client.CloudStatus.FANCY);
		});
		face(context, (float) home[3], (float) home[4]);
		context.waitTicks(20);
	}

	/**
	 * Knives: the old Blades settings carry over, hotbar slots are marked as knives, and each knife is
	 * drawn, slashes and inspects. With {@code every}, each knife gets a picture idle, mid-draw, mid-slash
	 * and mid-inspect on a platform in the sky; otherwise a few pictures in place.
	 */
	private static void knives(ClientGameTestContext context, TestSingleplayerContext singleplayer, List<String> failed, boolean every) {
		var server = singleplayer.getServer();
		check(failed, "Blades settings carry over to Knives", context.computeOnClient(client -> {
			java.util.Map<String, Boolean> modules = new java.util.HashMap<>(java.util.Map.of("blades", true));
			java.util.Map<String, String> settings = new java.util.HashMap<>(java.util.Map.of("blades.style", "Scythe", "blades.axes", "true"));
			Config.migrateBlades(modules, settings);
			return Boolean.TRUE.equals(modules.get("knives")) && !modules.containsKey("blades") && "Scythe".equals(settings.get("knives.model"))
					&& "Swords and axes".equals(settings.get("knives.auto_type")) && !settings.containsKey("blades.style");
		}));
		double[] home = context.computeOnClient(client -> new double[] {client.player.getX(), client.player.getY(), client.player.getZ(),
				client.player.getYRot(), client.player.getXRot()});
		int x = (int) Math.floor(home[0]);
		int y = (int) Math.floor(home[1]) + 40;
		int z = (int) Math.floor(home[2]);
		String[] items = {"netherite_sword", "diamond_sword", "stick", "iron_sword", "golden_sword", "blaze_rod", "netherite_sword", "iron_axe",
				"totem_of_undying"};
		for (int i = 0; i < 9; i++) server.runCommand("item replace entity @p hotbar." + i + " with " + items[i]);
		if (every) {
			server.runCommand("fill " + (x - 6) + " " + (y - 1) + " " + (z - 6) + " " + (x + 6) + " " + (y - 1) + " " + (z + 6) + " minecraft:smooth_stone");
			server.runCommand("tp @p " + x + ".5 " + y + " " + z + ".5 0 -8");
			server.runCommand("time set noon");
		}
		context.runOnClient(client -> {
			Modules.KNIVES.set(true);
			Modules.KNIVES_SLOTS.reset();
			for (KnifeModel model : KnifeModel.values()) Knives.setSlot(model.ordinal(), model);
			client.getToastManager().clear();
			client.gui.getChat().clearMessages(false);
		});
		face(context, 0, every ? -8 : 10);
		check(failed, "hotbar slots marked as knives keep their knife", context.computeOnClient(client ->
				Knives.slot(1) == KnifeModel.BUTTERFLY && Knives.slot(7) == KnifeModel.SCYTHE && Knives.slot(8) == null
						&& Modules.KNIVES_SLOTS.get().contains("2:bayonet")));
		context.runOnClient(client -> Knives.toggleSlot(8));
		check(failed, "a hotbar slot can be marked and unmarked", context.computeOnClient(client -> {
			boolean marked = Knives.slot(8) != null;
			Knives.toggleSlot(8);
			return marked && Knives.slot(8) == null;
		}));
		List<KnifeModel> shown = every ? List.of(KnifeModel.values()) : List.of(KnifeModel.KARAMBIT, KnifeModel.BUTTERFLY, KnifeModel.PHANTOM);
		for (KnifeModel model : shown) {
			String name = model.id();
			context.getInput().pressKey(GLFW.GLFW_KEY_1 + model.ordinal());
			if (every) {
				context.waitTicks(6);
				shot(context, "3vhy-knife-" + name + "-draw");
			}
			context.waitTicks(every ? 26 : 30);
			shot(context, "3vhy-knife-" + name);
			if (every || model == KnifeModel.KARAMBIT) {
				context.getInput().pressMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
				context.waitTicks(4);
				shot(context, "3vhy-knife-" + name + "-slash");
				context.waitTicks(16);
			}
			if (every || model == KnifeModel.BUTTERFLY) {
				context.getInput().pressKey(ThreevhyClient.inspectKey);
				context.waitTicks(every ? 22 : 26);
				shot(context, "3vhy-knife-" + name + "-inspect");
				if (model == KnifeModel.BUTTERFLY) check(failed, "the inspect key plays the knife's inspect", KnifeRenderer.inspecting()
						|| Hooks.knifeInspectRan);
				context.waitTicks(60);
			}
		}
		knifeSettings(context, failed);
		check(failed, "knives are drawn in your hand (ItemInHandRenderer)", Hooks.knifeRendered);
		check(failed, "switching to a knife plays its draw", Hooks.knifeDrawRan);
		check(failed, "a swing plays a slash (LocalPlayer.swing)", Hooks.knifeSlashRan);
		check(failed, "the inspect key starts an inspect", Hooks.knifeInspectRan);
		if (every) {
			for (String finish : List.of("Purple", "Crimson", "Ice", "Gold")) {
				context.runOnClient(client -> Modules.KNIVES_FINISH.set(finish));
				context.getInput().pressKey(GLFW.GLFW_KEY_3);
				context.waitTicks(30);
				shot(context, "3vhy-knife-bayonet-" + finish.toLowerCase(java.util.Locale.ROOT));
			}
			context.runOnClient(client -> {
				Modules.KNIVES_FINISH.reset();
				client.options.mainHand().set(net.minecraft.world.entity.HumanoidArm.LEFT);
				client.options.broadcastOptions();
			});
			context.getInput().pressKey(GLFW.GLFW_KEY_1);
			context.waitTicks(30);
			shot(context, "3vhy-knife-left-handed");
			context.runOnClient(client -> {
				client.options.mainHand().set(net.minecraft.world.entity.HumanoidArm.RIGHT);
				client.options.broadcastOptions();
			});
			server.runCommand("fill " + (x - 6) + " " + (y - 1) + " " + (z - 6) + " " + (x + 6) + " " + (y - 1) + " " + (z + 6) + " minecraft:air");
			server.runCommand(String.format(java.util.Locale.ROOT, "tp @p %.4f %.4f %.4f %.2f %.2f", home[0], home[1], home[2], home[3], home[4]));
		}
		context.runOnClient(client -> {
			Modules.KNIVES.set(false);
			Modules.KNIVES_SLOTS.reset();
		});
		server.runCommand("item replace entity @p hotbar.0 with netherite_sword");
		context.getInput().pressKey(GLFW.GLFW_KEY_1);
		context.waitTicks(5);
	}

	/**
	 * The Knives settings: the slot picker shows the hotbar, clicking slot 9 marks it, and a marked slot's
	 * knife picker opens its list of knives.
	 */
	private static void knifeSettings(ClientGameTestContext context, List<String> failed) {
		int scale = context.computeOnClient(client -> client.getWindow().getGuiScale());
		context.setScreen(() -> MenuScreen.settings(null, Modules.KNIVES));
		context.waitTicks(20);
		shot(context, "3vhy-menu-knives");
		float[] origin = context.computeOnClient(client -> ((MenuScreen) client.screen).view().sheetContent());
		float unit = origin[2];
		float width = 408;
		float slot = (width - 8 * 6) / 9f;
		context.getInput().setCursorPos((origin[0] + (8 * (slot + 6) + slot / 2f) * unit) * scale, (origin[1] + (26 + slot / 2f) * unit) * scale);
		context.waitTicks(2);
		context.getInput().pressMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
		context.waitTicks(5);
		check(failed, "clicking a slot in the Knives settings marks it", context.computeOnClient(client -> Knives.slot(8) != null));
		float firstRow = 26 + slot + 12 + 24;
		context.getInput().setCursorPos((origin[0] + (width - 48 - 75) * unit) * scale, (origin[1] + (firstRow + 22) * unit) * scale);
		context.waitTicks(2);
		context.getInput().pressMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
		context.waitTicks(10);
		shot(context, "3vhy-menu-knives-picker");
		context.runOnClient(client -> Knives.setSlot(8, null));
		context.setScreen(() -> null);
		context.waitTicks(5);
	}

	private static void escape(ClientGameTestContext context) {
		context.getInput().pressKey(InputConstants.KEY_ESCAPE);
		context.waitTicks(8);
	}

	/** Opens a screen through its button, takes its picture and goes back. */
	private static void visit(ClientGameTestContext context, int scale, String button, String name, int wait) {
		if (context.computeOnClient(client -> find(client, button) == null)) {
			ThreevhyClient.LOG.info("3vhy-check: no {} button on {}", button, context.computeOnClient(client ->
					client.screen == null ? "no screen" : client.screen.getClass().getSimpleName()));
			return;
		}
		click(context, scale, button);
		context.waitTicks(wait);
		shot(context, "3vhy-glass-" + name);
		escape(context);
	}

	/**
	 * Minecraft's screens out of a world, each pictured to check that every button, field, slider, checkbox,
	 * list, tab and tooltip is drawn as 3vhy glass: the title screen, servers, worlds, Realms, Options and its
	 * pages, and a confirm dialog.
	 */
	private static void glassTitleScreens(ClientGameTestContext context, List<String> failed) {
		int scale = context.computeOnClient(client -> client.getWindow().getGuiScale());
		context.setScreen(TitleScreen::new);
		context.waitTicks(20);
		shot(context, "3vhy-glass-title");
		int[] options = context.computeOnClient(client -> {
			AbstractWidget widget = find(client, "Options...");
			return widget == null ? null : new int[] {widget.getX() + widget.getWidth() / 2, widget.getY() + widget.getHeight() / 2};
		});
		if (options != null) {
			context.getInput().setCursorPos(options[0] * scale, options[1] * scale);
			context.waitTicks(10);
			shot(context, "3vhy-glass-title-hover");
		}
		check(failed, "every button on the title screen is glass", context.computeOnClient(client -> {
			for (AbstractWidget widget : Compat.widgets(client.screen)) {
				if (widget.visible && !dev.threevhy.client.gui.Glass.covers(widget)) {
					ThreevhyClient.LOG.info("3vhy-check: not glass: {} \"{}\"", widget.getClass().getName(), widget.getMessage().getString());
					return false;
				}
			}
			return true;
		}));

		check(failed, "every button on the title screen fits on the screen", context.computeOnClient(client -> {
			for (AbstractWidget widget : Compat.widgets(client.screen)) {
				if (widget.visible && (widget.getX() < 0 || widget.getY() < 0 || widget.getX() + widget.getWidth() > client.screen.width
						|| widget.getY() + widget.getHeight() > client.screen.height)) {
					ThreevhyClient.LOG.info("3vhy-check: off the screen: \"{}\" at {}, {} ({} x {})", widget.getMessage().getString(), widget.getX(),
							widget.getY(), widget.getWidth(), widget.getHeight());
					return false;
				}
			}
			return true;
		}));

		context.runOnClient(client -> client.options.skipMultiplayerWarning = false);
		click(context, scale, "Multiplayer");
		context.waitTicks(20);
		shot(context, "3vhy-glass-multiplayer-warning");
		context.runOnClient(client -> client.options.skipMultiplayerWarning = true);
		context.setScreen(TitleScreen::new);
		context.waitTicks(5);
		click(context, scale, "Multiplayer");
		context.waitTicks(20);
		shot(context, "3vhy-glass-servers");
		visit(context, scale, "Direct Connection", "direct-connect", 10);
		visit(context, scale, "Add Server", "add-server", 10);
		context.setScreen(TitleScreen::new);
		context.waitTicks(5);

		click(context, scale, "Singleplayer");
		context.waitTicks(20);
		shot(context, "3vhy-glass-worlds");
		click(context, scale, "Create New World");
		context.waitTicks(30);
		shot(context, "3vhy-glass-create-world");
		context.setScreen(TitleScreen::new);
		context.waitTicks(5);

		click(context, scale, "Minecraft Realms");
		context.waitTicks(40);
		shot(context, "3vhy-glass-realms");
		context.setScreen(TitleScreen::new);
		context.waitTicks(5);

		click(context, scale, "Options...");
		context.waitTicks(10);
		shot(context, "3vhy-glass-options");
		check(failed, "Options buttons and slider are glass (AbstractButton, AbstractSliderButton)", context.computeOnClient(client -> {
			int glass = 0;
			for (AbstractWidget widget : Compat.widgets(client.screen)) {
				if (!widget.visible) continue;
				if (!dev.threevhy.client.gui.Glass.covers(widget)) {
					ThreevhyClient.LOG.info("3vhy-check: not glass: {} \"{}\"", widget.getClass().getName(), widget.getMessage().getString());
					return false;
				}
				glass++;
			}
			return glass > 5;
		}));
		click(context, scale, "Video Settings...");
		context.waitTicks(10);
		shot(context, "3vhy-glass-video");
		int[] middle = context.computeOnClient(client -> new int[] {client.getWindow().getGuiScaledWidth() / 2 - 60,
				client.getWindow().getGuiScaledHeight() / 2});
		context.getInput().setCursorPos(middle[0] * scale, middle[1] * scale);
		context.waitTicks(25);
		shot(context, "3vhy-glass-video-tooltip");
		escape(context);
		click(context, scale, "Controls...");
		context.waitTicks(10);
		shot(context, "3vhy-glass-controls");
		visit(context, scale, "Key Binds...", "key-binds", 10);
		visit(context, scale, "Mouse Settings...", "mouse", 10);
		escape(context);
		visit(context, scale, "Language...", "language", 10);
		visit(context, scale, "Accessibility Settings...", "accessibility", 10);
		visit(context, scale, "Chat Settings...", "chat-settings", 10);
		visit(context, scale, "Skin Customization...", "skin", 10);
		visit(context, scale, "Online...", "online", 10);
		visit(context, scale, "Resource Packs...", "packs", 20);
		context.waitTicks(120);
		java.util.Set<String> glazed = dev.threevhy.client.gui.Glass.glazed();
		ThreevhyClient.LOG.info("3vhy-check: Minecraft widget art drawn as glass: {}", glazed);
		check(failed, "text fields, checkboxes, tabs, scrollbars and tooltips drawn as glass (GuiGraphics.blitSprite)",
				glazed.stream().anyMatch(path -> path.startsWith("widget/text_field")) && glazed.stream().anyMatch(path -> path.startsWith("widget/checkbox"))
						&& glazed.stream().anyMatch(path -> path.startsWith("widget/tab")) && glazed.stream().anyMatch(path -> path.startsWith("widget/scroller"))
						&& glazed.contains("tooltip/background"));
		java.util.Set<String> unglazed = dev.threevhy.client.gui.Glass.unglazed();
		ThreevhyClient.LOG.info("3vhy-check: Minecraft widget art left as it is: {}", unglazed);
		check(failed, "no Minecraft button, field, checkbox, slider, scrollbar, tab or tooltip art left", unglazed.stream().noneMatch(path ->
				path.matches("widget/(button|text_field|checkbox|slider|scroller|tab)(_.*)?|tooltip/(background|frame)")));
		context.setScreen(() -> new net.minecraft.client.gui.screens.ConfirmScreen(yes -> {
		}, Component.literal("Are you sure you want to delete this world?"), Component.literal("'New World' will be lost forever! (A long time!)")));
		context.waitTicks(10);
		shot(context, "3vhy-glass-confirm");
		context.setScreen(TitleScreen::new);
		context.waitTicks(5);
	}

	/** Minecraft's screens in a world, pictured like {@link #glassTitleScreens}: chat, a sign, a book and an anvil. */
	private static void glassWorldScreens(ClientGameTestContext context, TestSingleplayerContext singleplayer, List<String> failed) {
		var server = singleplayer.getServer();
		context.setScreen(() -> new net.minecraft.client.gui.screens.ChatScreen("Hello from 3vhy", false));
		context.waitTicks(10);
		shot(context, "3vhy-glass-chat");
		context.setScreen(() -> null);
		context.waitTicks(3);
		int[] at = context.computeOnClient(client -> new int[] {(int) Math.floor(client.player.getX()), (int) Math.floor(client.player.getY()),
				(int) Math.floor(client.player.getZ())});
		net.minecraft.core.BlockPos sign = new net.minecraft.core.BlockPos(at[0] + 2, at[1], at[2]);
		net.minecraft.core.BlockPos anvil = new net.minecraft.core.BlockPos(at[0] - 2, at[1], at[2]);
		server.runCommand("setblock " + sign.getX() + " " + sign.getY() + " " + sign.getZ() + " minecraft:oak_sign");
		server.runCommand("setblock " + anvil.getX() + " " + anvil.getY() + " " + anvil.getZ() + " minecraft:anvil");
		context.waitTicks(10);
		context.runOnClient(client -> {
			if (client.level.getBlockEntity(sign) instanceof net.minecraft.world.level.block.entity.SignBlockEntity entity) {
				client.player.openTextEdit(entity, true);
			}
		});
		context.waitTicks(10);
		shot(context, "3vhy-glass-sign");
		context.setScreen(() -> null);
		context.waitTicks(3);
		context.runOnClient(client -> client.gameMode.useItemOn(client.player, net.minecraft.world.InteractionHand.MAIN_HAND,
				new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(anvil), net.minecraft.core.Direction.UP, anvil,
						false)));
		context.waitTicks(15);
		shot(context, "3vhy-glass-anvil");
		context.setScreen(() -> null);
		context.waitTicks(3);
		server.runCommand("item replace entity @p hotbar.8 with minecraft:writable_book");
		context.waitTicks(3);
		context.getInput().pressKey(GLFW.GLFW_KEY_9);
		context.waitTicks(3);
		context.runOnClient(client -> client.gameMode.useItem(client.player, net.minecraft.world.InteractionHand.MAIN_HAND));
		context.waitTicks(15);
		shot(context, "3vhy-glass-book");
		context.setScreen(() -> null);
		context.waitTicks(3);
		server.runCommand("setblock " + sign.getX() + " " + sign.getY() + " " + sign.getZ() + " minecraft:air");
		server.runCommand("setblock " + anvil.getX() + " " + anvil.getY() + " " + anvil.getZ() + " minecraft:air");
		server.runCommand("item replace entity @p hotbar.8 with minecraft:air");
		context.getInput().pressKey(GLFW.GLFW_KEY_1);
		context.waitTicks(3);
	}

	/** Pictures of the knife at rest with one pose channel set at a time, to see which way each one moves it. */
	private static void calibrateKnife(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		var server = singleplayer.getServer();
		server.runCommand("item replace entity @p hotbar.0 with netherite_sword");
		server.runCommand("time set noon");
		context.runOnClient(client -> {
			Modules.KNIVES.set(true);
			Modules.KNIVES_SLOTS.reset();
			Knives.setSlot(0, KnifeModel.BAYONET);
		});
		face(context, 0, -8);
		context.getInput().pressKey(GLFW.GLFW_KEY_1);
		context.waitTicks(30);
		String[] names = {"x", "y", "z", "pitch", "yaw", "roll", "kpitch", "kyaw", "kroll"};
		float[] values = {0.15f, 0.15f, 0.15f, 30, 30, 30, 45, 45, 45};
		context.runOnClient(client -> KnifeRenderer.preview(KnifeModel.BAYONET, "channel0", 0));
		context.waitTicks(3);
		shot(context, "3vhy-calibrate-rest");
		for (int c = 0; c < names.length; c++) {
			int channel = c;
			context.runOnClient(client -> KnifeRenderer.preview(KnifeModel.BAYONET, "channel" + channel, values[channel]));
			context.waitTicks(3);
			shot(context, "3vhy-calibrate-" + names[c] + "-plus");
		}
		for (String which : List.of("slash0", "slash1", "slash2", "use")) {
			for (float t : new float[] {0.15f, 0.3f, 0.45f, 0.6f, 0.8f}) {
				context.runOnClient(client -> KnifeRenderer.preview(KnifeModel.BAYONET, which, t));
				context.waitTicks(2);
				shot(context, "3vhy-calibrate-" + which + "-" + Math.round(t * 100));
			}
		}
		context.runOnClient(client -> KnifeRenderer.preview(null, null, 0));
	}

	/** Turns the player to this yaw and pitch at once, with no turning in between. */
	private static void face(ClientGameTestContext context, float yaw, float pitch) {
		context.runOnClient(client -> {
			client.player.setYRot(yaw);
			client.player.setXRot(pitch);
			client.player.yRotO = yaw;
			client.player.xRotO = pitch;
		});
	}

	/** A screenshot's file, or null where in-world screenshots are skipped. */
	private static Path shotPath(ClientGameTestContext context, String name) {
		boolean sodiumOn26 = FabricLoader.getInstance().isModLoaded("sodium") && ThreevhyClient.minecraftVersion().startsWith("26.");
		if (sodiumOn26 && context.computeOnClient(client -> client.level != null)) {
			ThreevhyClient.LOG.info("3vhy-check: screenshot {} skipped (Sodium on 26.1)", name);
			return null;
		}
		return context.takeScreenshot(name);
	}

	/**
	 * Compares two screenshots of the same view over the middle of the screen (clear of the HUD's corners):
	 * {changed blocks, blocks, changed sky, sky}, counting pixels. Sky is where the second picture is blue;
	 * everything else is a block.
	 */
	private static int[] compareShots(Path a, Path b) {
		try {
			BufferedImage first = ImageIO.read(a.toFile());
			BufferedImage second = ImageIO.read(b.toFile());
			int w = Math.min(first.getWidth(), second.getWidth());
			int h = Math.min(first.getHeight(), second.getHeight());
			int[] out = new int[4];
			for (int py = h / 10; py < h * 7 / 10; py++) {
				for (int px = w / 5; px < w * 4 / 5; px++) {
					int one = first.getRGB(px, py);
					int two = second.getRGB(px, py);
					int r = (two >> 16) & 255;
					int g = (two >> 8) & 255;
					int bl = two & 255;
					boolean sky = bl > r + 15 && bl > g;
					boolean changed = Math.abs(((one >> 16) & 255) - r) > 12 || Math.abs(((one >> 8) & 255) - g) > 12 || Math.abs((one & 255) - bl) > 12;
					out[sky ? 3 : 1]++;
					if (changed) out[sky ? 2 : 0]++;
				}
			}
			return out;
		} catch (java.io.IOException | RuntimeException e) {
			ThreevhyClient.LOG.warn("3vhy-check: couldn't compare {} and {}", a, b, e);
			return new int[] {1, 0, 0, 0};
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

	/** The visible widget on the current screen with this label, or null. */
	private static AbstractWidget find(Minecraft client, String label) {
		for (AbstractWidget widget : Compat.widgets(client.screen)) {
			if (widget.visible && widget.getMessage().getString().equals(label)) return widget;
		}
		return client.screen == null ? null : findIn(client.screen.children(), label, 0);
	}

	/** Looks through lists and other containers on a screen for a widget with this label. */
	private static AbstractWidget findIn(List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children, String label, int depth) {
		if (depth > 4) return null;
		for (var child : children) {
			if (child instanceof AbstractWidget widget && widget.visible && widget.getMessage().getString().equals(label)) return widget;
			if (child instanceof net.minecraft.client.gui.components.events.ContainerEventHandler container) {
				AbstractWidget found = findIn(container.children(), label, depth + 1);
				if (found != null) return found;
			}
		}
		return null;
	}

	/** Clicks the widget with this label on the current screen, if there is one. */
	private static void click(ClientGameTestContext context, int scale, String label) {
		int[] at = context.computeOnClient(client -> {
			AbstractWidget widget = find(client, label);
			return widget == null ? null : new int[] {widget.getX() + widget.getWidth() / 2, widget.getY() + widget.getHeight() / 2};
		});
		ThreevhyClient.LOG.info("3vhy-check: clicking {} at {}", label, at == null ? "nowhere (not found)" : at[0] + ", " + at[1]);
		if (at == null) return;
		context.getInput().setCursorPos(at[0] * scale, at[1] * scale);
		context.waitTicks(2);
		context.getInput().pressMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
		context.waitTicks(3);
	}

	private static void check(List<String> failed, String name, boolean ok) {
		ThreevhyClient.LOG.info("3vhy-check: {} -> {}", name, ok ? "OK" : "MISSING");
		if (!ok) failed.add(name);
	}
}
