package dev.threevhy.client.module;

import dev.threevhy.client.Config;
import dev.threevhy.client.module.Module.Category;
import dev.threevhy.client.module.Setting.Choice;
import dev.threevhy.client.module.Setting.Color;
import dev.threevhy.client.module.Setting.Slider;
import dev.threevhy.client.module.Setting.Text;
import dev.threevhy.client.module.Setting.Toggle;
import dev.threevhy.client.music.MusicBridge;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/** Every 3vhy feature and its settings, plus the per-tick work for the ones that change game options. */
public final class Modules {
	private Modules() {
	}

	/** A text HUD module: every one can hide its label, fade its background and pick a colour. */
	private static Module hud(String id, String name, String description, boolean on) {
		Module module = new Module(id, name, description, Category.HUD, on);
		module.add(new Toggle("label", "Show label", "The name in front of the value, in the theme colour", true));
		background(module, 85);
		module.add(new Color("color", "Text colour", "Colour of the value", 0xFFE3F3FC));
		return module;
	}

	/** How big one widget is drawn, on top of the HUD size in Theme. Scroll over it in the hub to change it. */
	private static Module sized(Module module) {
		module.add(new Slider("size", "Size", "Or scroll over it in the hub", 50, 200, 5, 100, "%"));
		return module;
	}

	/** How see-through a HUD widget's box is. 0% removes the box and gives the text a shadow. */
	private static Module background(Module module) {
		return background(module, 60);
	}

	private static Module background(Module module, int percent) {
		module.add(new Slider("background", "Background", "0% makes the box fully transparent", 0, 100, 5, percent, "%"));
		return module;
	}

	// HUD
	public static final Module FPS = hud("fps", "FPS", "Shows your current frames per second", true);
	public static final Module CPS = hud("cps", "CPS", "Shows your clicks per second, left and right", true);
	public static final Module PING = hud("ping", "Ping", "Shows your latency to the server, coloured by quality", true);
	public static final Toggle PING_COLOURS = PING.add(new Toggle("quality", "Colour by quality",
			"Green when it's good, red when it's bad", true));
	public static final Module TPS = hud("tps", "TPS", "Displays the server's current ticks per second", false).markNew();
	public static final Toggle TPS_COLOURS = TPS.add(new Toggle("quality", "Colour by quality",
			"Green at 20, red when the server lags", true));
	public static final Module CLOCK = hud("clock", "Clock", "Displays the real-world time on your HUD", false).markNew();
	public static final Toggle CLOCK_24H = CLOCK.add(new Toggle("24h", "24-hour time", "14:05 instead of 2:05 PM", true));
	public static final Toggle CLOCK_SECONDS = CLOCK.add(new Toggle("seconds", "Seconds", "Show seconds too", false));
	public static final Module COORDS = hud("coords", "Coordinates", "Shows your current coordinates and facing", false);
	public static final Toggle COORDS_FACING = COORDS.add(new Toggle("facing", "Facing", "Which way you're looking", true));
	public static final Module KEYSTROKES = background(new Module("keystrokes", "Keystrokes",
			"Shows WASD, jump and your mouse buttons as you press them", Category.HUD, true), 85);
	public static final Toggle KEYSTROKES_MOUSE = KEYSTROKES.add(new Toggle("mouse", "Mouse buttons", "LMB and RMB with CPS", true));
	public static final Toggle KEYSTROKES_SPACE = KEYSTROKES.add(new Toggle("space", "Space bar", "The jump key", true));
	public static final Color KEYSTROKES_PRESSED = KEYSTROKES.add(new Color("pressed", "Pressed colour",
			"The colour a key lights up while you hold it", 0xFFF6F6F7));
	public static final Module ARMOR = background(new Module("armor", "Armor Status",
			"Displays the durability and status of your equipped armor", Category.HUD, true));
	public static final Choice ARMOR_LAYOUT = ARMOR.add(new Choice("layout", "Layout",
			"A column at the side, or a row like an armor bar", 0, "Column", "Row"));
	public static final Choice ARMOR_DURABILITY = ARMOR.add(new Choice("durability", "Durability",
			"How durability is shown", 0, "Percent", "Points", "Hidden"));
	public static final Toggle ARMOR_HAND = ARMOR.add(new Toggle("hand", "Held item", "Also show the item in your hand", false));
	public static final Module ITEMS = background(new Module("items", "Item Counter",
			"Counts the PvP items in your inventory", Category.HUD, true));
	public static final Toggle ITEMS_TOTEMS = ITEMS.add(new Toggle("totem", "Totems", "Totems of Undying", true));
	public static final Toggle ITEMS_CRYSTALS = ITEMS.add(new Toggle("crystal", "End crystals", "End crystals", true));
	public static final Toggle ITEMS_OBSIDIAN = ITEMS.add(new Toggle("obsidian", "Obsidian", "Obsidian", true));
	public static final Toggle ITEMS_XP = ITEMS.add(new Toggle("xp", "XP bottles", "Bottles o' Enchanting", true));
	public static final Toggle ITEMS_GAPPLES = ITEMS.add(new Toggle("gapple", "Golden apples", "Golden and enchanted golden apples", true));
	public static final Toggle ITEMS_PEARLS = ITEMS.add(new Toggle("pearl", "Ender pearls", "Ender pearls", false));
	public static final Toggle ITEMS_ANCHORS = ITEMS.add(new Toggle("anchor", "Anchors", "Respawn anchors and glowstone", false));
	public static final Module POPS = background(new Module("pops", "Totem Pops", "Who popped how many totems", Category.HUD, true));
	public static final Module TARGET_HUD = background(new Module("target_hud", "Target HUD",
			"Health, armor and totem of who you're fighting", Category.HUD, true));
	public static final Toggle TARGET_ARMOR = TARGET_HUD.add(new Toggle("armor", "Armor", "Their armor and held items", true));
	public static final Module COMBO = hud("combo", "Combo Counter", "Flex on your opponent with the combo you land", false).markNew();
	public static final Module SCOREBOARD = new Module("scoreboard", "Scoreboard",
			"Move the server scoreboard, hide its red numbers, or hide it", Category.HUD, true).markNew();
	public static final Toggle SCOREBOARD_HIDE_NUMBERS = SCOREBOARD.add(new Toggle("numbers", "Hide numbers",
			"Hide the red score numbers on the right", false));
	public static final Slider SCOREBOARD_SIZE = SCOREBOARD.add(new Slider("size", "Size",
			"Or scroll over it in the hub", 50, 200, 5, 100, "%"));
	public static final Slider SCOREBOARD_BACKGROUND = SCOREBOARD.add(new Slider("background", "Background",
			"How dark the box behind it is. 0% removes it", 0, 100, 5, 30, "%"));
	public static final Color SCOREBOARD_COLOR = SCOREBOARD.add(new Color("color", "Background colour",
			"Colour of the box behind it", 0xFF000000));
	public static final Toggle SCOREBOARD_TITLE_BAR = SCOREBOARD.add(new Toggle("title_bar", "Title bar",
			"A darker strip behind the title, like Minecraft's", true));
	public static final Module MUSIC = sized(background(new Module("music", "Music HUD",
			"Shows the song playing on Spotify, YouTube or any other app", Category.HUD, false).markNew(), 90));
	public static final Toggle MUSIC_ART = MUSIC.add(new Toggle("art", "Album art", "The cover next to the song", true));
	public static final Toggle MUSIC_PROGRESS = MUSIC.add(new Toggle("progress", "Progress bar", "Time played and time left", true));
	public static final Toggle MUSIC_VOLUME = MUSIC.add(new Toggle("volume", "Volume", "Windows' volume under the song", true));
	public static final Color MUSIC_COLOR = MUSIC.add(new Color("color", "Colour", "The play button, progress bar and volume bar", 0xFFF2F2F2));
	public static final Toggle MUSIC_PAUSED = MUSIC.add(new Toggle("paused", "Show when paused",
			"Keep it on screen while the music is paused", true));
	public static final Module MODULE_LIST = sized(background(new Module("module_list", "Module List",
			"The mods you have on, listed down the side of the screen", Category.HUD, true).markNew(), 90));
	public static final Toggle MODULE_LIST_HUD = MODULE_LIST.add(new Toggle("hud", "HUD mods",
			"Also list the HUD mods you can already see, like FPS and Keystrokes", false));
	public static final Toggle MODULE_LIST_GLOW = MODULE_LIST.add(new Toggle("glow", "Glow",
			"A soft glow around the names, like 67's", true));
	public static final Module PLAYER_LIST = sized(background(new Module("player_list", "Player List",
			"Add players and see when they're online on your server", Category.HUD, true).markNew(), 90));
	public static final Text PLAYER_LIST_TITLE = PLAYER_LIST.add(new Text("title", "Title", "The name at the top of the list",
			"Players", 16));
	public static final Choice PLAYER_LIST_ICON = PLAYER_LIST.add(new Choice("icon", "Icon",
			"A green plus like 67's, or the player's head, before each name", 0, "Plus", "Head"));
	public static final Toggle PLAYER_LIST_OFFLINE = PLAYER_LIST.add(new Toggle("offline", "Show offline players",
			"Also list players who aren't on, after the ones who are", false));
	public static final Toggle PLAYER_LIST_ALERTS = PLAYER_LIST.add(new Toggle("alerts", "Join alerts",
			"A chat message when someone on your list joins or leaves", true));
	public static final Toggle PLAYER_LIST_ASK = PLAYER_LIST.add(new Toggle("ask", "Ask the server",
			"For servers like DonutSMP whose tab list doesn't show everyone: checks /msg name suggestions", true));
	public static final Text PLAYER_LIST_COMMAND = PLAYER_LIST.add(new Text("command", "Check with",
			"The command whose name suggestions show who's online, like msg or tpa", "msg", 16));
	public static final Module JUMP_RESET = background(new Module("jump_reset", "Jump Reset",
			"Shows how well you timed your jump after being hit, to take less knockback", Category.HUD, false).markNew());

	// PvP
	public static final Module CRYSTAL_OPTIMIZER = new Module("crystal_optimizer", "Crystal Optimizer",
			"Crystals vanish the moment you hit them. Check the server's rules", Category.PVP, false);
	public static final Module POP_NOTIFIER = new Module("pop_notifier", "Pop Alerts",
			"A chat message when someone pops a totem", Category.PVP, true);
	public static final Module TOTEM_WARNING = new Module("totem_warning", "Totem Warning",
			"Warns you when your offhand has no totem but your inventory does", Category.PVP, true);
	public static final Module ITEM_HIGHLIGHTER = new Module("item_highlighter", "Item Highlighter",
			"Highlights PvP items in your inventory so you find them quickly", Category.PVP, true).markNew();
	public static final Toggle HIGHLIGHT_TOTEMS = ITEM_HIGHLIGHTER.add(new Toggle("totem", "Totems", "Totems of Undying", true));
	public static final Toggle HIGHLIGHT_CRYSTALS = ITEM_HIGHLIGHTER.add(new Toggle("crystal", "End crystals", "End crystals", true));
	public static final Toggle HIGHLIGHT_OBSIDIAN = ITEM_HIGHLIGHTER.add(new Toggle("obsidian", "Obsidian", "Obsidian", false));
	public static final Toggle HIGHLIGHT_GAPPLES = ITEM_HIGHLIGHTER.add(new Toggle("gapple", "Golden apples", "Golden apples", true));
	public static final Toggle HIGHLIGHT_PEARLS = ITEM_HIGHLIGHTER.add(new Toggle("pearl", "Ender pearls", "Ender pearls", true));
	public static final Toggle HIGHLIGHT_XP = ITEM_HIGHLIGHTER.add(new Toggle("xp", "XP bottles", "Bottles o' Enchanting", false));
	public static final Color HIGHLIGHT_COLOR = ITEM_HIGHLIGHTER.add(new Color("color", "Colour", "Highlight colour", 0xFF5BC8E6));
	public static final Module HURT_CAM = new Module("no_hurt_cam", "Hurt Cam",
			"Choose how much your view shakes when you take damage", Category.PVP, false);
	public static final Slider HURT_CAM_STRENGTH = HURT_CAM.add(new Slider("strength", "Shake", "0% means no shake at all",
			0, 100, 5, 0, "%"));

	// Visual
	public static final Module CROSSHAIR = new Module("crosshair", "Custom Crosshair",
			"Personalise your crosshair for better aiming", Category.VISUAL, false).markNew();
	public static final Choice CROSSHAIR_STYLE = CROSSHAIR.add(new Choice("style", "Style", "Shape of the crosshair", 0,
			"Cross", "Cross + dot", "Dot", "Circle", "T"));
	public static final Slider CROSSHAIR_SIZE = CROSSHAIR.add(new Slider("size", "Size", "Length of each arm", 1, 10, 1, 4, ""));
	public static final Slider CROSSHAIR_GAP = CROSSHAIR.add(new Slider("gap", "Gap", "Space in the middle", 0, 6, 1, 2, ""));
	public static final Slider CROSSHAIR_THICKNESS = CROSSHAIR.add(new Slider("thickness", "Thickness", "Width of the lines",
			1, 3, 1, 1, ""));
	public static final Color CROSSHAIR_COLOR = CROSSHAIR.add(new Color("color", "Colour", "Crosshair colour", 0xFFE3F3FC));
	public static final Toggle CROSSHAIR_OUTLINE = CROSSHAIR.add(new Toggle("outline", "Outline", "A dark edge so it shows on bright blocks", true));
	public static final Toggle CROSSHAIR_TARGET = CROSSHAIR.add(new Toggle("target", "Red on players",
			"Turns red while you're aiming at a player", true));
	public static final Module FULLBRIGHT = new Module("fullbright", "Fullbright",
			"Removes darkness and improves visibility in caves", Category.VISUAL, false);
	public static final Module NO_FOG = new Module("no_fog", "NoFog", "Removes fog for a clearer, more detailed view",
			Category.VISUAL, false).markNew();
	public static final Module FOV_CHANGER = new Module("fov_changer", "FOV Changer",
			"Customise how much your FOV changes when sprinting or flying", Category.VISUAL, false).markNew();
	public static final Slider FOV_EFFECTS = FOV_CHANGER.add(new Slider("effects", "FOV effects",
			"How much sprinting, flying and speed change your FOV", 0, 100, 5, 0, "%"));
	public static final Module NAMETAGS = new Module("nametags", "Nametags",
			"Customise player nametags and see your own in third person", Category.VISUAL, false).markNew();
	public static final Toggle NAMETAGS_OWN = NAMETAGS.add(new Toggle("own", "Show your own", "See your nametag in third person", true));
	public static final Module SHINY_POTS = new Module("shiny_pots", "Shiny Pots",
			"Adds an enchantment glint to potions", Category.VISUAL, false).markNew();
	public static final Module SKIN_PROTECT = new Module("skin_protect", "Skin Protect",
			"Wear a different skin: a default one, any player's, or your own file", Category.VISUAL, false).markNew();
	public static final Toggle SKIN_PROTECT_OWN = SKIN_PROTECT.add(new Toggle("own", "Your skin",
			"Change how you look", true));
	public static final Toggle SKIN_PROTECT_OTHERS = SKIN_PROTECT.add(new Toggle("others", "Other players",
			"Everyone else gets the same skin too", false));
	public static final Choice SKIN_PROTECT_SOURCE = SKIN_PROTECT.add(new Choice("source", "Show as",
			"Minecraft's default skins, any player's skin, or a skin file", 0,
			CustomSkins.SOURCE_DEFAULT, CustomSkins.SOURCE_PLAYER, CustomSkins.SOURCE_FILE));
	public static final Text SKIN_PROTECT_PLAYER = SKIN_PROTECT.add(new Text("player", "Player",
			"Whose skin to wear, for A player's skin", "", 16));
	public static final Choice SKIN_PROTECT_ARMS = SKIN_PROTECT.add(new Choice("arms", "Arms",
			"Classic or slim arms, for skin files", 0, "Classic", "Slim"));
	public static final Module MENU_NAME = new Module("menu_name", "Menu Logo",
			"The 3vhy Client logo in the bottom right of menus, chests and your inventory, or on your HUD all the time",
			Category.VISUAL, true).markNew();
	public static final Choice MENU_NAME_SHOW = MENU_NAME.add(new Choice("show", "Show", "The logo, your name, or both", 0,
			"Logo", "Logo and name", "Name"));
	public static final Toggle MENU_NAME_HEAD = MENU_NAME.add(new Toggle("head", "Show your head", "Your skin's face next to your name", true));
	public static final Toggle MENU_NAME_HUD = MENU_NAME.add(new Toggle("hud", "Show in game",
			"Keep it on your HUD while you play too, above the Music HUD; move it in Edit HUD", false));
	public static final Module SWING_SPEED = new Module("swing_speed", "Swing Speed",
			"Slows down (or speeds up) your arm swing when you hit, place and use things", Category.VISUAL, false).markNew();
	public static final Slider SWING_SPEED_SPEED = SWING_SPEED.add(new Slider("speed", "Speed",
			"How fast your arm swings: under 1x is slower than normal", 0.2, 2.0, 0.05, 0.5, "x"));
	public static final Toggle SWING_SPEED_OTHERS = SWING_SPEED.add(new Toggle("others", "Other players too",
			"Everyone's swing on your screen, not just yours", false));
	public static final Module MAIN_MENU = new Module("main_menu", "Main Menu",
			"The 3vhy look on the title screen; buttons from other mods still show", Category.VISUAL, true);
	public static final Choice MAIN_MENU_SEASON = MAIN_MENU.add(new Choice("season", "October look",
			"White with ghosts and pumpkins: on by itself in October, or always or never", 0, "Auto", "Always", "Never"));
	public static final Module THEME = new Module("theme", "Theme",
			"Switch between light and dark colours and the snow for 3vhy's menus", Category.VISUAL, true).alwaysOn();
	public static final Choice THEME_MODE = THEME.add(new Choice("mode", "Colours", "Dark glass or light frost", 0, "Dark", "Light"));
	public static final Choice THEME_MENU_STYLE = THEME.add(new Choice("menu_style", "Mod menu",
			"Cards like Lunar's, or a floating panel for each category", 0, "Classic", "Panels"));
	public static final Choice THEME_FONT = THEME.add(new Choice("font", "Menu font",
			"The font in the Panels menu and the Music HUD: a clean sans-serif, or Minecraft's pixel font", 0, "Clean",
			"Minecraft"));
	public static final Color THEME_PANEL_COLOR = THEME.add(new Color("panel_color", "Panels colour",
			"The colour of the Panels menu, the module list and the menu logo", 0xFF5A9CFF));
	public static final Toggle THEME_RAINBOW = THEME.add(new Toggle("rainbow", "Rainbow",
			"The Panels colour slowly cycles through the rainbow", false));
	public static final Toggle THEME_SNOW = THEME.add(new Toggle("snow", "Snow", "Pixel snow behind 3vhy's menus", true));
	public static final Slider THEME_SNOW_AMOUNT = THEME.add(new Slider("snow_amount", "Snow amount", "How much snow falls",
			10, 100, 10, 50, "%"));
	public static final Slider THEME_HUD_SCALE = THEME.add(new Slider("hud_scale", "HUD size", "Makes every HUD widget bigger or smaller",
			50, 200, 5, 100, "%"));
	public static final Toggle THEME_ROUNDED_HUD = THEME.add(new Toggle("rounded_hud", "Rounded HUD",
			"Soft round corners on HUD widgets", true));
	public static final Choice THEME_WINDOW_TITLE = THEME.add(new Choice("window_title", "Window title",
			"Keep \"Minecraft\" in it so Medal, OBS and Discord recognise the game", 0,
			"Minecraft - 3vhy Client", "3vhy Client"));

	// Utility
	public static final Module ZOOM = new Module("zoom", "Zoom", "Hold C to zoom in", Category.UTILITY, true);
	public static final Slider ZOOM_STRENGTH = ZOOM.add(new Slider("strength", "Zoom strength", "How far zoom goes in",
			2, 10, 0.5, 4, "x"));
	public static final Toggle ZOOM_SMOOTH = ZOOM.add(new Toggle("smooth", "Smooth camera", "Cinematic camera while zooming", true));
	public static final Toggle ZOOM_SCROLL = ZOOM.add(new Toggle("scroll", "Scroll to adjust", "Mouse wheel zooms further in or out", true));
	public static final Module FREELOOK = new Module("freelook", "Freelook",
			"Hold Left Alt to look around freely without changing your movement", Category.UTILITY, true).markNew();
	public static final Choice FREELOOK_MODE = FREELOOK.add(new Choice("mode", "Key mode", "Hold the key or press it once",
			0, "Hold", "Toggle"));
	public static final Module TOGGLE_SPRINT = new Module("toggle_sprint", "ToggleSprint",
			"Press sprint once and keep sprinting", Category.UTILITY, false);
	public static final Toggle TOGGLE_SNEAK = TOGGLE_SPRINT.add(new Toggle("sneak", "Toggle sneak too", "Press sneak once to keep sneaking", false));
	public static final Module CHAT = new Module("chat", "Chat",
			"Timestamps, stacked spam and longer chat history", Category.UTILITY, true).markNew();
	public static final Toggle CHAT_TIMESTAMPS = CHAT.add(new Toggle("timestamps", "Timestamps", "[14:05] in front of every message", true));
	public static final Toggle CHAT_STACK = CHAT.add(new Toggle("stack", "Stack spam", "Repeated messages become one with (x3)", true));
	public static final Toggle CHAT_HISTORY = CHAT.add(new Toggle("history", "Longer history", "Keep 1000 lines instead of 100", true));
	public static final Module TEXT_HOTKEYS = new Module("text_hotkeys", "Text Hotkeys",
			"Press a key to send a chat message or run a command, like gg or /home", Category.UTILITY, false).markNew();
	public static final Module TAB_EDITOR = new Module("tab_editor", "Tab Editor",
			"Customise the tab list: ping as numbers, hide parts", Category.UTILITY, true).markNew();
	public static final Toggle TAB_PING_NUMBERS = TAB_EDITOR.add(new Toggle("ping", "Ping as numbers", "137ms instead of bars", true));
	public static final Toggle TAB_HIDE_HEADER = TAB_EDITOR.add(new Toggle("header", "Hide header and footer", "Just the player list", false));
	public static final Module CONFIRM_DISCONNECT = new Module("confirm_disconnect", "Confirm Disconnect",
			"Asks for a second click before you disconnect", Category.UTILITY, true).markNew();
	public static final Module BORDERLESS = new Module("borderless", "Borderless Fullscreen",
			"Fullscreen as a borderless window, so alt-tab is instant", Category.UTILITY, false).markNew();
	public static final Module NAME_PROTECT = new Module("name_protect", "Name Protect",
			"Hides your name and changes any text on screen, like your stats", Category.UTILITY, false).markNew();
	public static final Text NAME_PROTECT_NAME = NAME_PROTECT.add(new Text("name", "Show me as",
			"The name shown instead of yours", "You", 16));
	public static final Toggle NAME_PROTECT_OTHERS = NAME_PROTECT.add(new Toggle("others", "Hide other players",
			"Other players' names become Player 1, Player 2 and so on", false));
	public static final Module WAYPOINTS = new Module("waypoints", "Waypoints",
			"Mark places and see them on screen with their distance", Category.UTILITY, true).markNew();
	public static final Toggle WAYPOINTS_DISTANCE = WAYPOINTS.add(new Toggle("distance", "Distance", "Show how far away each one is", true));
	public static final Module PROFILES = new Module("profiles", "Profiles",
			"Save and switch between different mod and setting setups", Category.UTILITY, true).alwaysOn();
	public static final Module DISCORD = new Module("discord", "Discord Presence",
			"Show 3vhy Client and your server on your Discord profile", Category.UTILITY, true);
	public static final Toggle DISCORD_SERVER = DISCORD.add(new Toggle("server", "Show server address",
			"Your server's address on your Discord profile", true));

	public static final List<Module> ALL = List.of(
			FPS, CPS, PING, TPS, CLOCK, COORDS, KEYSTROKES, ARMOR, ITEMS, POPS, TARGET_HUD, COMBO, SCOREBOARD, JUMP_RESET,
			MUSIC, PLAYER_LIST, MODULE_LIST,
			CRYSTAL_OPTIMIZER, POP_NOTIFIER, TOTEM_WARNING, ITEM_HIGHLIGHTER, HURT_CAM,
			CROSSHAIR, FULLBRIGHT, NO_FOG, FOV_CHANGER, NAMETAGS, SHINY_POTS, SWING_SPEED, SKIN_PROTECT, MENU_NAME, MAIN_MENU,
			THEME,
			ZOOM, FREELOOK, TOGGLE_SPRINT, CHAT, TEXT_HOTKEYS, TAB_EDITOR, CONFIRM_DISCONNECT, BORDERLESS, NAME_PROTECT, WAYPOINTS,
			PROFILES, DISCORD);

	private static Boolean appliedToggleSprint = null;
	private static Boolean appliedToggleSneak = null;
	private static Double appliedHurtCam = null;
	private static Double appliedFovEffects = null;
	private static Boolean appliedNoFog = null;
	private static boolean addedNightVision = false;
	private static final int NIGHT_VISION_TICKS = 20 * 30;

	public static void tick(Minecraft mc) {
		syncToggleKeys(mc);
		syncHurtCam(mc);
		syncFovEffects(mc);
		syncFullbright(mc);
		syncFog();
		Zoom.tick(mc);
		Freelook.tick(mc);
		Target.tick(mc);
		Combo.tick(mc);
		JumpReset.tick(mc);
		ConfirmDisconnect.tick();
		NameProtect.tick(mc);
		TextRules.tick();
		TextHotkeys.tick(mc);
		CustomSkins.tick();
		PlayerList.tick(mc);
		MusicBridge.tick();
	}

	/** Uses Minecraft's own "Sprint: Toggle" and "Sneak: Toggle", so they behave exactly like vanilla. */
	private static void syncToggleKeys(Minecraft mc) {
		boolean sprint = TOGGLE_SPRINT.enabled();
		// Only touch the vanilla options when the module changes, so players who never use it keep
		// whatever they picked in Controls
		if (appliedToggleSprint == null || appliedToggleSprint != sprint) {
			if (appliedToggleSprint != null || sprint) mc.options.toggleSprint().set(sprint);
			appliedToggleSprint = sprint;
		}
		boolean sneak = sprint && TOGGLE_SNEAK.get();
		if (appliedToggleSneak == null || appliedToggleSneak != sneak) {
			if (appliedToggleSneak != null || sneak) mc.options.toggleCrouch().set(sneak);
			appliedToggleSneak = sneak;
		}
	}

	/** Uses the vanilla "Damage Tilt" accessibility option. */
	private static void syncHurtCam(Minecraft mc) {
		Double want = HURT_CAM.enabled() ? HURT_CAM_STRENGTH.get() / 100.0 : null;
		if (java.util.Objects.equals(want, appliedHurtCam)) return;
		if (want != null) {
			if (appliedHurtCam == null) {
				Config.get().savedDamageTilt = mc.options.damageTiltStrength().get();
				Config.saveSoon();
			}
			mc.options.damageTiltStrength().set(want);
		} else if (appliedHurtCam != null) {
			mc.options.damageTiltStrength().set(Config.get().savedDamageTilt);
		}
		appliedHurtCam = want;
	}

	/** Uses the vanilla "FOV Effects" accessibility option. */
	private static void syncFovEffects(Minecraft mc) {
		Double want = FOV_CHANGER.enabled() ? FOV_EFFECTS.get() / 100.0 : null;
		if (java.util.Objects.equals(want, appliedFovEffects)) return;
		if (want != null) {
			if (appliedFovEffects == null) {
				Config.get().savedFovEffects = mc.options.fovEffectScale().get();
				Config.saveSoon();
			}
			mc.options.fovEffectScale().set(want);
		} else if (appliedFovEffects != null) {
			mc.options.fovEffectScale().set(Config.get().savedFovEffects);
		}
		appliedFovEffects = want;
	}

	/** Uses the fog switch Minecraft keeps for debugging. */
	private static void syncFog() {
		boolean noFog = NO_FOG.enabled();
		if (appliedNoFog == null && !noFog) {
			appliedNoFog = false;
			return;
		}
		if (appliedNoFog != null && appliedNoFog == noFog) return;
		// toggleFog flips the switch and returns the new state; flip again if it landed wrong
		if (FogRenderer.toggleFog() == noFog) FogRenderer.toggleFog();
		appliedNoFog = noFog;
	}

	/** Fullbright through a hidden, client-only night vision effect. */
	private static void syncFullbright(Minecraft mc) {
		LocalPlayer player = mc.player;
		if (player == null) {
			addedNightVision = false;
			return;
		}
		if (FULLBRIGHT.enabled()) {
			MobEffectInstance current = player.getEffect(MobEffects.NIGHT_VISION);
			if (current == null || (addedNightVision && current.getDuration() < NIGHT_VISION_TICKS - 100)) {
				player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, NIGHT_VISION_TICKS, 0, false, false, false));
				addedNightVision = true;
			}
		} else if (addedNightVision) {
			player.removeEffect(MobEffects.NIGHT_VISION);
			addedNightVision = false;
		}
	}
}
