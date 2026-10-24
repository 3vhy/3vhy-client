package dev.threevhy.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.threevhy.client.Hooks;
import dev.threevhy.client.module.Blades;
import dev.threevhy.client.module.SwingSpeed;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Swing Speed: how fast the item in your hand comes up when you switch to it (pulling out a sword).
 * Blades: the inspect animation, turning the blade over in your hand.
 */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
	@ModifyConstant(method = "tick", constant = {@Constant(floatValue = 0.4F), @Constant(floatValue = -0.4F)})
	private float threevhy$equipSpeed(float step) {
		return SwingSpeed.equipStep(step);
	}

	@Inject(method = "renderArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem("
			+ "Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;"
			+ "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"))
	private void threevhy$inspect(AbstractClientPlayer player, float partialTick, float pitch, InteractionHand hand, float swing, ItemStack stack,
			float equip, PoseStack pose, SubmitNodeCollector collector, int light, CallbackInfo ci) {
		float t = Blades.inspectProgress(hand);
		if (t < 0) return;
		Hooks.inspectRan = true;
		float side = player.getMainArm() == HumanoidArm.RIGHT ? 1 : -1;
		float in = smooth(t / 0.16f);
		float out = 1 - smooth((t - 0.8f) / 0.2f);
		float e = Math.min(in, out);
		float flip = smooth((t - 0.38f) / 0.34f);
		float yaw = e * (-68 + 108 * flip);
		float roll = e * (20 + 7 * (float) Math.sin(t * Math.PI * 2));
		float tilt = e * 12 * (float) Math.sin(t * Math.PI);
		pose.translate(-0.13f * side * e, 0.09f * e, 0.05f * e);
		pose.mulPose(Axis.YP.rotationDegrees(yaw * side));
		pose.mulPose(Axis.ZP.rotationDegrees(roll * side));
		pose.mulPose(Axis.XP.rotationDegrees(tilt));
	}

	private static float smooth(float x) {
		float t = Math.max(0, Math.min(1, x));
		return t * t * (3 - 2 * t);
	}
}
