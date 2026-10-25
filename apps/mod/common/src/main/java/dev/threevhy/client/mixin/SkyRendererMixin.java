package dev.threevhy.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.threevhy.client.hud.SkyLyrics;
import net.minecraft.client.renderer.SkyRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sky Lyrics, drawn as part of the sky: right after the sun, moon and stars (or the End's sky), before
 * any block is drawn, so the world covers them like it covers the sky.
 */
@Mixin(SkyRenderer.class)
public abstract class SkyRendererMixin {
	@Inject(method = "renderSunMoonAndStars", at = @At("TAIL"))
	private void threevhy$lyricsInSky(PoseStack poseStack, float sunAngle, float moonAngle, float starAngle,
			net.minecraft.world.level.MoonPhase moonPhase, float rainBrightness, float starBrightness, CallbackInfo ci) {
		SkyLyrics.draw(rainBrightness);
	}

	@Inject(method = "renderEndSky", at = @At("TAIL"))
	private void threevhy$lyricsInEndSky(CallbackInfo ci) {
		SkyLyrics.draw(1);
	}
}
