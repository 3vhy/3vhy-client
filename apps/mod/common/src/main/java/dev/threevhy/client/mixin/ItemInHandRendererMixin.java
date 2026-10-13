package dev.threevhy.client.mixin;

import dev.threevhy.client.module.SwingSpeed;
import net.minecraft.client.renderer.ItemInHandRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Swing Speed: how fast the item in your hand comes up when you switch to it (pulling out a sword). */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
	@ModifyConstant(method = "tick", constant = {@Constant(floatValue = 0.4F), @Constant(floatValue = -0.4F)})
	private float threevhy$equipSpeed(float step) {
		return SwingSpeed.equipStep(step);
	}
}
