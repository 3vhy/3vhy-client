package dev.threevhy.client.mixin;

import dev.threevhy.client.module.Zoom;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Zoom: the mouse wheel zooms further in or out while the zoom key is held. */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
	@Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
	private void threevhy$zoomScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
		if (Zoom.onScroll(vertical)) ci.cancel();
	}
}
