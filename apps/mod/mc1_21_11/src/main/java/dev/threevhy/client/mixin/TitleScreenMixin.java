package dev.threevhy.client.mixin;

import dev.threevhy.client.compat.DrawImpl;
import dev.threevhy.client.gui.TitleScreenStyle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 3vhy main menu: October's ghosts behind the title screen in place of the panorama. */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin {
	@Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
	private void threevhy$background(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		if (!TitleScreenStyle.drawsBackground()) return;
		Screen screen = (Screen) (Object) this;
		TitleScreenStyle.drawBackground(new DrawImpl(graphics), screen.width, screen.height);
		ci.cancel();
	}
}
