package dev.threevhy.client.mixin;

import dev.threevhy.client.module.Nametags;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Nametags: players have their own nametag rule on top of the living-entity one. */
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
	@Inject(method = "shouldShowName(Lnet/minecraft/world/entity/Avatar;D)Z", at = @At("RETURN"), cancellable = true)
	private void threevhy$ownNametag(Avatar entity, double distance, CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValueZ() && Nametags.showOwn(entity)) {
			dev.threevhy.client.Hooks.ownNametagRan = true;
			cir.setReturnValue(true);
		}
	}
}
