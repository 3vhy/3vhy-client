package dev.threevhy.client.mixin;

import dev.threevhy.client.module.Freelook;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Freelook: the camera uses its own angles instead of the player's. */
@Mixin(Camera.class)
public abstract class CameraMixin {
	@ModifyArg(method = {"setup", "alignWithEntity"},
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V"), index = 0)
	private float threevhy$freelookYaw(float yaw) {
		if (!Freelook.active()) return yaw;
		dev.threevhy.client.Hooks.freelookCameraRan = true;
		return Freelook.yaw();
	}

	@ModifyArg(method = {"setup", "alignWithEntity"},
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V"), index = 1)
	private float threevhy$freelookPitch(float pitch) {
		return Freelook.active() ? Freelook.pitch() : pitch;
	}
}
