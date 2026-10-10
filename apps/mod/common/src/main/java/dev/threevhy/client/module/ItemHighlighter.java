package dev.threevhy.client.module;

import dev.threevhy.client.Draw;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Tints the slots holding the PvP items you picked, in every inventory screen. */
public final class ItemHighlighter {
	private ItemHighlighter() {
	}

	private static boolean wanted(ItemStack stack) {
		if (stack.isEmpty()) return false;
		if (Modules.HIGHLIGHT_TOTEMS.get() && stack.is(Items.TOTEM_OF_UNDYING)) return true;
		if (Modules.HIGHLIGHT_CRYSTALS.get() && stack.is(Items.END_CRYSTAL)) return true;
		if (Modules.HIGHLIGHT_OBSIDIAN.get() && stack.is(Items.OBSIDIAN)) return true;
		if (Modules.HIGHLIGHT_GAPPLES.get() && (stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE))) return true;
		if (Modules.HIGHLIGHT_PEARLS.get() && stack.is(Items.ENDER_PEARL)) return true;
		return Modules.HIGHLIGHT_XP.get() && stack.is(Items.EXPERIENCE_BOTTLE);
	}

	/** Called before a slot's item is drawn, in the screen's slot coordinates. */
	public static void drawSlot(Draw d, Slot slot) {
		if (!Modules.ITEM_HIGHLIGHTER.enabled() || !wanted(slot.getItem())) return;
		int color = Modules.HIGHLIGHT_COLOR.get() & 0xFFFFFF;
		int x = slot.x;
		int y = slot.y;
		d.fill(x, y, x + 16, y + 16, 0x58000000 | color);
		d.outline(x, y, x + 16, y + 16, 0xC0000000 | color);
	}
}
