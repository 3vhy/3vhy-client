package dev.eventide.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.eventide.client.module.Nametags;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Nametags: a player's tag in Eventide's style (face, name and health), with the scoreboard line above it kept as Minecraft draws it. */
@Mixin(AvatarRenderer.class)
public abstract class AvatarNameTagMixin {
	@Inject(method = "submitNameTag(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;"
			+ "Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V", at = @At("HEAD"), cancellable = true)
	private void eventide$nameTag(AvatarRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
		if (!Nametags.styled(state)) return;
		int offset = state.showExtraEars ? -10 : 0;
		pose.pushPose();
		if (state.scoreText != null) {
			collector.submitNameTag(pose, state.nameTagAttachment, offset, state.scoreText, !state.isDiscrete, state.lightCoords, state.distanceToCameraSq,
					camera);
			pose.translate(0.0F, 9.0F * 1.15F * 0.025F, 0.0F);
		}
		Nametags.submit(state, pose, collector, camera.orientation, offset);
		pose.popPose();
		dev.eventide.client.Hooks.styledNametagRan = true;
		ci.cancel();
	}
}
