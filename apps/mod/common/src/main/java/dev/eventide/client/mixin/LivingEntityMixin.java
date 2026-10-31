package dev.eventide.client.mixin;

import dev.eventide.client.module.SwingSpeed;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Swing Speed: the arm swing's length, which only drives the animation. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@Inject(method = "getCurrentSwingDuration", at = @At("RETURN"), cancellable = true)
	private void eventide$swingSpeed(CallbackInfoReturnable<Integer> cir) {
		int vanilla = cir.getReturnValueI();
		int changed = SwingSpeed.duration((LivingEntity) (Object) this, vanilla);
		if (changed != vanilla) cir.setReturnValue(changed);
	}
}
