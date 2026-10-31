package dev.eventide.client.mixin;

import dev.eventide.client.compat.DrawImpl;
import dev.eventide.client.gui.Glass;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.layouts.LayoutElement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The picked row in any list (worlds, servers, packs, languages) as a glass bar instead of a white box. */
@Mixin(AbstractSelectionList.class)
public abstract class AbstractSelectionListMixin {
	@Inject(method = "renderSelection", at = @At("HEAD"), cancellable = true)
	private void eventide$glassSelection(GuiGraphics graphics, @Coerce LayoutElement entry, int outline, CallbackInfo ci) {
		if (!Glass.on()) return;
		Glass.selection(new DrawImpl(graphics), entry.getX(), entry.getY(), entry.getX() + entry.getWidth(), entry.getY() + entry.getHeight(), outline == -1);
		ci.cancel();
	}
}
