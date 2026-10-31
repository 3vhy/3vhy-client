package dev.eventide.client.mixin;

import dev.eventide.client.compat.DrawImpl;
import dev.eventide.client.gui.ButtonStyle;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Sliders as glass, with the accent filling up to the handle. */
@Mixin(AbstractSliderButton.class)
public abstract class AbstractSliderButtonMixin {
	@Inject(method = "extractWidgetRenderState", at = @At("HEAD"), cancellable = true)
	private void eventide$glassSlider(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		if (ButtonStyle.slider(new DrawImpl(graphics), (AbstractSliderButton) (Object) this)) ci.cancel();
	}
}
