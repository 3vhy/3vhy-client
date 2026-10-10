package dev.threevhy.client.mixin;

import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Lets 3vhy re-apply fullscreen when Borderless Fullscreen is switched on or off. */
@Mixin(Window.class)
public interface WindowAccessor {
	@Invoker("setMode")
	void threevhy$setMode();
}
