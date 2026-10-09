package dev.threevhy.client.gui;

import dev.threevhy.client.Draw;
import dev.threevhy.client.hud.Theme;
import dev.threevhy.client.module.Module;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** The item each module shows on its card in 3vhy Settings. */
public final class ModuleIcons {
	private static final Map<String, ItemStack> CACHE = new HashMap<>();
	private static final Map<Item, ItemStack> STACKS = new HashMap<>();
	/** Set when items couldn't be made yet: on 26.1 that's the case until a world has loaded. */
	private static boolean unavailable = false;

	private ModuleIcons() {
	}

	/** The module's icon item, or null when items can't be made yet (then a letter tile is shown). */
	public static ItemStack of(Module module) {
		ItemStack cached = CACHE.get(module.id);
		if (cached != null) return cached;
		if (unavailable && Minecraft.getInstance().level == null) return null;
		try {
			ItemStack stack = new ItemStack(item(module.id));
			CACHE.put(module.id, stack);
			unavailable = false;
			return stack;
		} catch (RuntimeException e) {
			unavailable = true;
			return null;
		}
	}

	/** An item to draw, or null when items can't be made yet. */
	public static ItemStack stack(Item item) {
		ItemStack cached = STACKS.get(item);
		if (cached != null) return cached;
		if (unavailable && Minecraft.getInstance().level == null) return null;
		try {
			ItemStack stack = new ItemStack(item);
			STACKS.put(item, stack);
			unavailable = false;
			return stack;
		} catch (RuntimeException e) {
			unavailable = true;
			return null;
		}
	}

	/** Draws the module's icon 24x24 at (x, y). */
	public static void draw(Draw d, Module module, int x, int y) {
		ItemStack stack = of(module);
		if (stack != null) {
			d.push();
			d.translate(x, y);
			d.scale(1.5f);
			d.item(stack, 0, 0);
			d.pop();
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		d.roundRect(x + 1, y + 1, x + 23, y + 23, 5, 0x405BC8E6);
		d.roundOutline(x + 1, y + 1, x + 23, y + 23, 5, 0x805BC8E6);
		String letter = module.name.substring(0, 1).toUpperCase(java.util.Locale.ROOT);
		d.push();
		d.translate(x + 12 - mc.font.width(letter), y + 5);
		d.scale(2f);
		d.text(letter, 0, 0, Theme.ICE, false);
		d.pop();
	}

	private static Item item(String id) {
		return switch (id) {
			case "fps" -> Items.CLOCK;
			case "cps" -> Items.IRON_SWORD;
			case "ping" -> Items.ENDER_PEARL;
			case "coords" -> Items.MAP;
			case "keystrokes" -> Items.TRIPWIRE_HOOK;
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
