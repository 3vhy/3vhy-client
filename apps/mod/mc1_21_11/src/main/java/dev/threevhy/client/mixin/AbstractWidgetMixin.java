package dev.threevhy.client.mixin;

import dev.threevhy.client.compat.DrawImpl;
import dev.threevhy.client.gui.TitleScreenStyle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 3vhy main menu: the dark buttons down the right of the title screen, like Lunar's. */
@Mixin(AbstractWidget.class)
public abstract class AbstractWidgetMixin {
	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private void threevhy$titleButton(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		AbstractWidget widget = (AbstractWidget) (Object) this;
		if (!TitleScreenStyle.styled(widget)) return;
		TitleScreenStyle.drawButton(new DrawImpl(graphics), widget, mouseX, mouseY);
		ci.cancel();
	}
}
