package dev.threevhy.client.mixin;

import dev.threevhy.client.module.SkinProtect;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Skin Protect: players' bodies and tab list heads come from here, so a default skin goes in its place. */
@Mixin(PlayerInfo.class)
public abstract class PlayerInfoMixin {
	@Shadow
	public abstract com.mojang.authlib.GameProfile getProfile();

	@Inject(method = "getSkin", at = @At("HEAD"), cancellable = true)
	private void threevhy$protectSkin(CallbackInfoReturnable<PlayerSkin> cir) {
		PlayerSkin replacement = SkinProtect.replacement(getProfile());
		if (replacement != null) cir.setReturnValue(replacement);
	}
}
