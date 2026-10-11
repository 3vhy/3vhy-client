package dev.threevhy.client.mixin;

import dev.threevhy.client.hud.CpsTracker;
import dev.threevhy.client.module.Zoom;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Zoom: the mouse wheel zooms further in or out while the zoom key is held. CPS: every button
 * press Minecraft handles is counted, without touching the window's own mouse callbacks (other
 * mods use those too).
 */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
	@Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
	private void threevhy$zoomScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
		if (Zoom.onScroll(vertical)) ci.cancel();
	}

	@Inject(method = "onButton", at = @At("HEAD"), require = 1)
	private void threevhy$countClick(long window, MouseButtonInfo button, int action, CallbackInfo ci) {
		if (action == GLFW.GLFW_PRESS) CpsTracker.onPress(button.button());
	}
}
