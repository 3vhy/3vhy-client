package dev.threevhy.client.mixin;

import dev.threevhy.client.Hooks;
import dev.threevhy.client.gui.TitleScreenStyle;
import net.minecraft.client.gui.components.LogoRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 3vhy main menu: Minecraft's logo makes way for the 3vhy emblem and name. */
@Mixin(LogoRenderer.class)
public abstract class LogoRendererMixin {
	@Inject(method = {"renderLogo(Lnet/minecraft/client/gui/GuiGraphics;IF)V", "renderLogo(Lnet/minecraft/client/gui/GuiGraphics;IFI)V"}, at = @At("HEAD"), cancellable = true)
	private void threevhy$hideLogo(CallbackInfo ci) {
		if (!TitleScreenStyle.hidesMinecraftLogo()) return;
		Hooks.minecraftLogoHidden = true;
		ci.cancel();
	}
}
