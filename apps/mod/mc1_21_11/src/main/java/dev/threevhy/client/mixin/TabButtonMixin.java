package dev.threevhy.client.mixin;

import dev.threevhy.client.gui.Glass;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.TabButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** An open tab's glass pill shows through, instead of Minecraft's dark backing over it. */
@Mixin(TabButton.class)
public abstract class TabButtonMixin {
	@Inject(method = "renderMenuBackground", at = @At("HEAD"), cancellable = true)
	private void threevhy$noBacking(GuiGraphics graphics, int x1, int y1, int x2, int y2, CallbackInfo ci) {
		if (Glass.on()) ci.cancel();
	}
}
