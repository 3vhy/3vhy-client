package dev.threevhy.client.mixin;

import dev.threevhy.client.module.ShinyPots;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Shiny Pots: potions get the enchantment glint. */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
	@Inject(method = "hasFoil", at = @At("RETURN"), cancellable = true)
	private void threevhy$shinyPots(CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValueZ() && ShinyPots.shiny((ItemStack) (Object) this)) cir.setReturnValue(true);
	}
}
