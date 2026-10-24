package dev.threevhy.client.mixin;

import dev.threevhy.client.compat.DrawImpl;
import dev.threevhy.client.gui.TitleScreenStyle;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 3vhy main menu: the scene behind the title screen, the time of day's light, and how the panorama turns. */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
	protected TitleScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
	private void threevhy$background(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		if (!TitleScreenStyle.drawsBackground()) return;
		TitleScreenStyle.drawBackground(new DrawImpl(graphics), this.width, this.height);
		ci.cancel();
	}

	@Inject(method = "extractBackground", at = @At("TAIL"))
	private void threevhy$tint(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		TitleScreenStyle.drawTint(new DrawImpl(graphics), this.width, this.height);
	}

	@Override
	protected boolean panoramaShouldSpin() {
		return TitleScreenStyle.panoramaSpins();
	}
}
