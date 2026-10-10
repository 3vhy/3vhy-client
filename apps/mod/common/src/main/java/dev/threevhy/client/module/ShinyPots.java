package dev.threevhy.client.module;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

/** The enchantment glint on potions, like before 1.19.4. */
public final class ShinyPots {
	private ShinyPots() {
	}

	public static boolean shiny(ItemStack stack) {
		return Modules.SHINY_POTS.enabled() && stack.has(DataComponents.POTION_CONTENTS);
	}
}
