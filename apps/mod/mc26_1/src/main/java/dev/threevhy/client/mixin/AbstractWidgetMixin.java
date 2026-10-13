package dev.threevhy.client.mixin;

import dev.threevhy.client.compat.DrawImpl;
import dev.threevhy.client.gui.TitleScreenStyle;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 3vhy main menu: the title screen's buttons are drawn as 3vhy glass instead of Minecraft's stone. */
@Mixin(AbstractWidget.class)
public abstract class AbstractWidgetMixin {
	@Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
	private void threevhy$titleButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		AbstractWidget widget = (AbstractWidget) (Object) this;
		if (!TitleScreenStyle.styled(widget)) return;
		TitleScreenStyle.drawButton(new DrawImpl(graphics), widget, mouseX, mouseY);
		ci.cancel();
	}
}
