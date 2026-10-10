package dev.threevhy.client.mixin;

import com.mojang.blaze3d.platform.Window;
import dev.threevhy.client.module.Borderless;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Borderless fullscreen takes over Minecraft's switch between windowed and fullscreen. */
@Mixin(Window.class)
public abstract class WindowMixin {
	@Shadow @Final private long handle;
	@Shadow private boolean fullscreen;
	@Shadow private int windowedX;
	@Shadow private int windowedY;
	@Shadow private int windowedWidth;
	@Shadow private int windowedHeight;
	@Shadow private int x;
	@Shadow private int y;
	@Shadow private int width;
	@Shadow private int height;

	@Inject(method = "setMode", at = @At("HEAD"), cancellable = true)
	private void threevhy$borderless(CallbackInfo ci) {
		boolean handled = Borderless.apply(handle, fullscreen, () -> {
			windowedX = x;
			windowedY = y;
			windowedWidth = width;
			windowedHeight = height;
		});
		if (handled) ci.cancel();
	}
}
