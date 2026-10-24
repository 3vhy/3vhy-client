package dev.threevhy.client.mixin;

import dev.threevhy.client.compat.DrawImpl;
import dev.threevhy.client.gui.TitleScreenStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 3vhy main menu: the 3vhy logo where Minecraft's goes on the title screen. */
@Mixin(LogoRenderer.class)
public abstract class LogoRendererMixin {
	@Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IFI)V", at = @At("HEAD"), cancellable = true)
	private void threevhy$logo(GuiGraphicsExtractor graphics, int screenWidth, float alpha, int y, CallbackInfo ci) {
		if (!TitleScreenStyle.enabled() || !(Minecraft.getInstance().screen instanceof TitleScreen)) return;
		TitleScreenStyle.drawLogo(new DrawImpl(graphics), screenWidth, alpha, y);
		ci.cancel();
	}

	@Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IF)V", at = @At("HEAD"), cancellable = true)
	private void threevhy$logoAtTop(GuiGraphicsExtractor graphics, int screenWidth, float alpha, CallbackInfo ci) {
		if (!TitleScreenStyle.enabled() || !(Minecraft.getInstance().screen instanceof TitleScreen)) return;
		TitleScreenStyle.drawLogo(new DrawImpl(graphics), screenWidth, alpha, 30);
		ci.cancel();
	}
}
