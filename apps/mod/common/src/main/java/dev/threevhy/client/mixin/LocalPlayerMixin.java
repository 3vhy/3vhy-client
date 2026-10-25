package dev.threevhy.client.mixin;

import dev.threevhy.client.module.Knives;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Knives: your arm swinging (a hit, or a click on a block) starts the knife's slash. */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
	@Inject(method = "swing", at = @At("HEAD"))
	private void threevhy$slash(InteractionHand hand, CallbackInfo ci) {
		Knives.onSwing(hand);
	}
}
