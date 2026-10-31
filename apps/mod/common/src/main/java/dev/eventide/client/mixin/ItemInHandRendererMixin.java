package dev.eventide.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.eventide.client.knife.KnifeRenderer;
import dev.eventide.client.module.SwingSpeed;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Swing Speed: how fast the item in your hand comes up when you switch to it (pulling out a sword).
 * Knives: your main hand drawn as a knife with its own animations, in place of the held item.
 */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
	@ModifyConstant(method = "tick", constant = {@Constant(floatValue = 0.4F), @Constant(floatValue = -0.4F)})
	private float eventide$equipSpeed(float step) {
		return SwingSpeed.equipStep(step);
	}

	@Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
	private void eventide$knife(AbstractClientPlayer player, float partialTick, float pitch, InteractionHand hand, float swing, ItemStack stack,
			float equip, PoseStack pose, SubmitNodeCollector collector, int light, CallbackInfo ci) {
		if (hand != InteractionHand.MAIN_HAND) return;
		if (KnifeRenderer.render(player, player.getMainArm(), stack, pose, collector, light)) ci.cancel();
	}
}
