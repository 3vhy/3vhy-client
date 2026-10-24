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
import dev.threevhy.client.module.Module;
import dev.threevhy.client.module.Module.Category;
import dev.threevhy.client.module.Modules;
import dev.threevhy.client.module.NameProtect;
import dev.threevhy.client.module.PlayerList;
import dev.threevhy.client.module.PopTracker;
import dev.threevhy.client.module.Target;
import dev.threevhy.client.music.MusicBridge;
import dev.threevhy.client.ui.Sky;
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
			context.waitTicks(5);
			shot(context, "3vhy-player-list-closeup-more");
			check(failed, "player list shows six and +N MORE", context.computeOnClient(client ->
					PlayerList.names().size() == listedPlayers.size() + 6 && HudWidgets.PLAYER_LIST.visible(client)));
			context.runOnClient(client -> {
				PlayerList.names().clear();
				PlayerList.names().addAll(listedPlayers);
			});
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
					Modules.SKIN_PROTECT, Modules.WAYPOINTS, Modules.MUSIC, Modules.THEME, Modules.MAIN_MENU)) {
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
				boolean feedback = false;
				for (AbstractWidget widget : Compat.widgets(client.screen)) {
					if (!widget.visible) continue;
					String label = widget.getMessage().getString();
					if (label.equals(PauseMenuButton.LABEL)) mods = widget;
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
				boolean modsRow = mods.getY() == stats.getY() + stats.getHeight() + 4 && mods.getX() == disconnect.getX()
						&& mods.getWidth() == disconnect.getWidth();
				boolean optionsRow = ours.getY() == options.getY() && ours.getX() > options.getX();
				return new int[][] {
						{mods.getX() + mods.getWidth() / 2, mods.getY() + mods.getHeight() / 2},
						{ours.getX() + ours.getWidth() / 2, ours.getY() + ours.getHeight() / 2},
						{modsRow ? 1 : 0, optionsRow ? 1 : 0, feedback ? 1 : 0}};
			});
			check(failed, "pause menu has 3vhy Client and 3vhy Options...", pauseButtons != null);
			check(failed, "3vhy Client is a full row right under Advancements and Statistics", pauseButtons != null && pauseButtons[2][0] == 1);
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
