package dev.threevhy.client.module;

import dev.threevhy.client.Config;
import dev.threevhy.client.module.Module.Category;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/** Every 3vhy feature, plus the per-tick work for the ones that change game settings. */
public final class Modules {
	// HUD
	public static final Module FPS = new Module("fps", "FPS", "Frames per second", Category.HUD, true);
	public static final Module CPS = new Module("cps", "CPS", "Clicks per second, left and right", Category.HUD, true);
	public static final Module PING = new Module("ping", "Ping", "Your latency to the server", Category.HUD, true);
	public static final Module COORDS = new Module("coords", "Coordinates", "Position and facing", Category.HUD, false);
	public static final Module KEYSTROKES = new Module("keystrokes", "Keystrokes", "WASD, jump and mouse buttons", Category.HUD, true);
	public static final Module ARMOR = new Module("armor", "Armor status", "Armor with durability left", Category.HUD, true);
	public static final Module ITEMS = new Module("items", "PvP item counter", "Totems, crystals, obsidian, XP and gapples", Category.HUD, true);
	public static final Module POPS = new Module("pops", "Totem pops", "Who popped how many totems", Category.HUD, true);
	public static final Module TARGET_HUD = new Module("target_hud", "Target HUD",
			"Health, armor and totem of who you're fighting", Category.HUD, true);

	// PvP
	public static final Module CRYSTAL_OPTIMIZER = new Module("crystal_optimizer", "Crystal optimizer",
			"Crystals vanish the moment you hit them. Check the server's rules", Category.PVP, false);
	public static final Module POP_NOTIFIER = new Module("pop_notifier", "Pop notifications",
			"Chat message when someone pops a totem", Category.PVP, true);
	public static final Module TOTEM_WARNING = new Module("totem_warning", "Totem warning",
			"Warns you when your offhand has no totem but your inventory does", Category.PVP, true);
	public static final Module NO_HURT_CAM = new Module("no_hurt_cam", "No hurt cam",
			"Your view doesn't shake when you take damage", Category.PVP, false);
	public static final Module FULLBRIGHT = new Module("fullbright", "Fullbright",
			"See clearly in the dark", Category.PVP, false);

	// Utility
	public static final Module ZOOM = new Module("zoom", "Zoom", "Hold C to zoom in", Category.UTILITY, true);
	public static final Module TOGGLE_SPRINT = new Module("toggle_sprint", "Toggle sprint",
			"Press sprint once and keep sprinting", Category.UTILITY, false);

	// Discord
	public static final Module DISCORD = new Module("discord", "Discord presence",
			"Show 3vhy Client and your server on Discord", Category.DISCORD, true);

	public static final List<Module> ALL = List.of(
			FPS, CPS, PING, COORDS, KEYSTROKES, ARMOR, ITEMS, POPS, TARGET_HUD,
			CRYSTAL_OPTIMIZER, POP_NOTIFIER, TOTEM_WARNING, NO_HURT_CAM, FULLBRIGHT,
			ZOOM, TOGGLE_SPRINT,
			DISCORD);

	private static Boolean appliedToggleSprint = null;
	private static Boolean appliedNoHurtCam = null;
	private static boolean addedNightVision = false;
	private static final int NIGHT_VISION_TICKS = 20 * 30;

	private Modules() {
	}

	public static void tick(Minecraft mc) {
		syncToggleSprint(mc);
		syncNoHurtCam(mc);
		syncFullbright(mc);
		Zoom.tick(mc);
		Target.tick(mc);
	}

	/** Uses Minecraft's own "Sprint: Toggle" setting, so it behaves exactly like vanilla. */
	private static void syncToggleSprint(Minecraft mc) {
		boolean want = TOGGLE_SPRINT.enabled();
		if (appliedToggleSprint == null || appliedToggleSprint != want) {
			// Only touch the vanilla option when the module changes, so a player who never
			// uses the module keeps whatever they picked in Controls
			if (appliedToggleSprint != null || want) {
				mc.options.toggleSprint().set(want);
			}
			appliedToggleSprint = want;
		}
	}

	/** Uses the vanilla "Damage Tilt" accessibility setting. */
	private static void syncNoHurtCam(Minecraft mc) {
		boolean want = NO_HURT_CAM.enabled();
		if (appliedNoHurtCam != null && appliedNoHurtCam == want) {
			return;
		}
		if (want) {
			double current = mc.options.damageTiltStrength().get();
			if (current > 0) {
				Config.get().savedDamageTilt = current;
				Config.saveSoon();
			}
			mc.options.damageTiltStrength().set(0.0);
		} else if (appliedNoHurtCam != null) {
			mc.options.damageTiltStrength().set(Config.get().savedDamageTilt);
		}
		appliedNoHurtCam = want;
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
