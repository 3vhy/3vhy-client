package dev.threevhy.client.gui;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Item stacks to draw on the HUD and in menus. On 26.1 items can't be made until a world has
 * loaded, so this returns null there instead of crashing, and callers draw without the icon.
 */
public final class SafeItems {
	private static final Map<Item, ItemStack> STACKS = new HashMap<>();
	private static boolean unavailable = false;

	private SafeItems() {
	}

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
}
