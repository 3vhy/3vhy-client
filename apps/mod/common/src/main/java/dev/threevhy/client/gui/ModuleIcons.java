package dev.threevhy.client.gui;

import dev.threevhy.client.module.Module;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** The item each module shows on its card in 3vhy Settings. */
public final class ModuleIcons {
	private static final Map<String, ItemStack> CACHE = new HashMap<>();

	private ModuleIcons() {
	}

	public static ItemStack of(Module module) {
		return CACHE.computeIfAbsent(module.id, id -> new ItemStack(item(id)));
	}

	private static Item item(String id) {
		return switch (id) {
			case "fps" -> Items.CLOCK;
			case "cps" -> Items.STONE_BUTTON;
			case "ping" -> Items.ENDER_PEARL;
			case "coords" -> Items.MAP;
			case "keystrokes" -> Items.LEVER;
			case "armor" -> Items.DIAMOND_CHESTPLATE;
			case "items" -> Items.ENCHANTED_GOLDEN_APPLE;
			case "pops" -> Items.TOTEM_OF_UNDYING;
			case "target_hud" -> Items.TARGET;
			case "crystal_optimizer" -> Items.END_CRYSTAL;
			case "pop_notifier" -> Items.BELL;
			case "totem_warning" -> Items.REDSTONE_TORCH;
			case "no_hurt_cam" -> Items.SHIELD;
			case "fullbright" -> Items.LANTERN;
			case "zoom" -> Items.SPYGLASS;
			case "toggle_sprint" -> Items.SUGAR;
			case "main_menu" -> Items.PAINTING;
			case "discord" -> Items.NAME_TAG;
			default -> Items.BOOK;
		};
	}
}
