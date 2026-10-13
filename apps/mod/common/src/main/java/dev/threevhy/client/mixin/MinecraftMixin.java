package dev.threevhy.client.mixin;

import dev.threevhy.client.WindowBranding;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
	/** Whenever Minecraft updates the window title, put "3vhy Client" back. */
	@Inject(method = "updateTitle", at = @At("RETURN"))
	private void threevhy$keepTitle(CallbackInfo ci) {
		dev.threevhy.client.Hooks.titleHookRan = true;
		WindowBranding.apply();
	}
}
