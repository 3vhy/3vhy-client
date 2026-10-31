package dev.eventide.client.mixin;

import dev.eventide.client.WindowBranding;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
	/** Whenever Minecraft updates the window title, put "Eventide" back. */
	@Inject(method = "updateTitle", at = @At("RETURN"))
	private void eventide$keepTitle(CallbackInfo ci) {
		dev.eventide.client.Hooks.titleHookRan = true;
		WindowBranding.apply();
	}
}
