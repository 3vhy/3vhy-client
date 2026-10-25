package dev.threevhy.client.mixin;

import dev.threevhy.client.compat.DrawImpl;
import dev.threevhy.client.gui.ButtonStyle;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.SpriteIconButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The language and accessibility buttons' pixel icons as line icons on their glass. */
@Mixin(SpriteIconButton.class)
public abstract class SpriteIconButtonMixin {
	@Inject(method = "extractSprite", at = @At("HEAD"), cancellable = true)
	private void threevhy$lineIcon(GuiGraphicsExtractor graphics, int x, int y, CallbackInfo ci) {
		if (ButtonStyle.icon(new DrawImpl(graphics), (AbstractWidget) (Object) this)) ci.cancel();
	}
}
