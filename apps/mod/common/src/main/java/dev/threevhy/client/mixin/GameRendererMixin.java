package dev.threevhy.client.mixin;

import dev.threevhy.client.module.Zoom;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Zoom: divide the field of view while the zoom key is held. */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@SuppressWarnings({"rawtypes", "unchecked"})
	@Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
	private void threevhy$zoom(CallbackInfoReturnable cir) {
		double divisor = Zoom.divisor();
		if (divisor <= 1.0001) return;
		// Works whether this Minecraft version returns the FOV as a float or a double
		Object fov = cir.getReturnValue();
		if (fov instanceof Float f) {
			cir.setReturnValue((float) (f / divisor));
		} else if (fov instanceof Double dbl) {
			cir.setReturnValue(dbl / divisor);
		}
	}
}
