package dev.threevhy.client.mixin;

import dev.threevhy.client.compat.DrawImpl;
import dev.threevhy.client.module.TabTweaks;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tab Editor: ping as numbers, and the server's header and footer can be hidden. */
@Mixin(PlayerTabOverlay.class)
public abstract class PlayerTabOverlayMixin {
	@Shadow private Component header;
	@Shadow private Component footer;

	@Inject(method = "renderPingIcon", at = @At("HEAD"), cancellable = true)
	private void threevhy$pingNumber(GuiGraphics graphics, int width, int x, int y, PlayerInfo info, CallbackInfo ci) {
		if (TabTweaks.drawPing(new DrawImpl(graphics), width, x, y, info)) {
			dev.threevhy.client.Hooks.tabPingRan = true;
			ci.cancel();
		}
	}

	@Redirect(method = "render", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
			target = "Lnet/minecraft/client/gui/components/PlayerTabOverlay;header:Lnet/minecraft/network/chat/Component;"))
	private Component threevhy$header(PlayerTabOverlay overlay) {
		return TabTweaks.hideHeaderAndFooter() ? null : header;
	}

	@Redirect(method = "render", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
			target = "Lnet/minecraft/client/gui/components/PlayerTabOverlay;footer:Lnet/minecraft/network/chat/Component;"))
	private Component threevhy$footer(PlayerTabOverlay overlay) {
		return TabTweaks.hideHeaderAndFooter() ? null : footer;
	}
}
