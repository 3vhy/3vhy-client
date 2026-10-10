package dev.threevhy.client.mixin;

import dev.threevhy.client.compat.DrawImpl;
import dev.threevhy.client.module.ItemHighlighter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Item Highlighter: tint behind the PvP items in every inventory. */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
	@Inject(method = "renderSlot", at = @At("HEAD"))
	private void threevhy$highlight(GuiGraphics graphics, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
		dev.threevhy.client.Hooks.slotHighlightRan = true;
		ItemHighlighter.drawSlot(new DrawImpl(graphics), slot);
	}
}
