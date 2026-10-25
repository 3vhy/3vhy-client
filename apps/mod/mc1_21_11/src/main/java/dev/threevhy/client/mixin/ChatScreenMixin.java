package dev.threevhy.client.mixin;

import dev.threevhy.client.compat.DrawImpl;
import dev.threevhy.client.gui.Glass;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** The bar you type chat in as a glass pill. */
@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {
	@Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V", ordinal = 0))
	private void threevhy$glassBar(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
		if (Glass.on()) Glass.chatBar(new DrawImpl(graphics), x1, y1, x2, y2);
		else graphics.fill(x1, y1, x2, y2, color);
	}
}
