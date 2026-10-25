package dev.threevhy.client.mixin;

import dev.threevhy.client.compat.DrawImpl;
import dev.threevhy.client.gui.ButtonStyle;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Every button that draws Minecraft's button background and label (Minecraft's and any mod's) gets the
 * glass pill and Inter label instead; whatever the button draws besides them stays.
 */
@Mixin(AbstractButton.class)
public abstract class AbstractButtonMixin {
	@Unique
	private static GuiGraphics threevhy$graphics;

	@Inject(method = "renderWidget", at = @At("HEAD"))
	private void threevhy$keepGraphics(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		threevhy$graphics = graphics;
	}

	@Inject(method = "renderDefaultSprite", at = @At("HEAD"), cancellable = true)
	private void threevhy$glassBackground(GuiGraphics graphics, CallbackInfo ci) {
		if (ButtonStyle.background(new DrawImpl(graphics), (AbstractWidget) (Object) this)) ci.cancel();
	}

	@Inject(method = "renderDefaultLabel", at = @At("HEAD"), cancellable = true)
	private void threevhy$glassLabel(ActiveTextCollector output, CallbackInfo ci) {
		if (threevhy$graphics != null && ButtonStyle.label(new DrawImpl(threevhy$graphics), (AbstractWidget) (Object) this)) ci.cancel();
	}
}
