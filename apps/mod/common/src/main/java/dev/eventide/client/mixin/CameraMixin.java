package dev.eventide.client.mixin;

import dev.eventide.client.module.Freelook;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Freelook: the camera uses its own angles instead of the player's. Knives: the small kick a slash gives the view. */
@Mixin(Camera.class)
public abstract class CameraMixin {
	@ModifyArg(method = {"setup", "alignWithEntity"},
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V"), index = 0)
	private float eventide$freelookYaw(float yaw) {
		float kick = dev.eventide.client.knife.KnifeRenderer.kickYaw();
		if (!Freelook.active()) return yaw + kick;
		dev.eventide.client.Hooks.freelookCameraRan = true;
		return Freelook.yaw() + kick;
	}

	@ModifyArg(method = {"setup", "alignWithEntity"},
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V"), index = 1)
	private float eventide$freelookPitch(float pitch) {
		return (Freelook.active() ? Freelook.pitch() : pitch) + dev.eventide.client.knife.KnifeRenderer.kickPitch();
	}
}
