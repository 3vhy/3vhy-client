package dev.threevhy.client.mixin;

import dev.threevhy.client.module.Freelook;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Freelook: mouse movement turns the camera, not the player. */
@Mixin(Entity.class)
public abstract class EntityMixin {
	@Inject(method = "turn", at = @At("HEAD"), cancellable = true)
	private void threevhy$freelookTurn(double yRot, double xRot, CallbackInfo ci) {
		if ((Object) this == Minecraft.getInstance().player && Freelook.turn(yRot, xRot)) ci.cancel();
	}
}
