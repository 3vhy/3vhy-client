package dev.eventide.client.mixin;

import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Lets Eventide re-apply fullscreen when Borderless Fullscreen is switched on or off. */
@Mixin(Window.class)
public interface WindowAccessor {
	@Invoker("setMode")
	void eventide$setMode();
}
