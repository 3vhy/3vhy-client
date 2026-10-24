package dev.threevhy.client.mixin;

import dev.threevhy.client.compat.DrawImpl;
import dev.threevhy.client.gui.ButtonStyle;
import dev.threevhy.client.gui.TitleScreenStyle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Minecraft's buttons and sliders as 3vhy's violet glass, and 3vhy's own title screen buttons. Only the
 * drawing of the widget is swapped, so hover state and tooltips work as usual.
 */
@Mixin(AbstractWidget.class)
public abstract class AbstractWidgetMixin {
	@Shadow
	protected abstract void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta);

	@Redirect(method = "render", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/components/AbstractWidget;renderWidget(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"))
	private void threevhy$drawWidget(AbstractWidget widget, GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		if (TitleScreenStyle.styled(widget)) {
			TitleScreenStyle.drawWidget(new DrawImpl(graphics), widget);
		} else if (ButtonStyle.styles(widget)) {
			ButtonStyle.draw(new DrawImpl(graphics), widget);
		} else {
			renderWidget(graphics, mouseX, mouseY, delta);
		}
	}
}
