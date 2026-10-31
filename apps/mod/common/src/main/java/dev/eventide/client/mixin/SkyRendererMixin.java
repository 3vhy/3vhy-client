package dev.eventide.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.eventide.client.hud.SkyLyrics;
import dev.eventide.client.sky.LivingSky;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.world.level.MoonPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The sky pass. With Skies on, its sky shader takes the place of Minecraft's sky colour, sunset glow, dark
 * lower half and stars (and the sun and moon, if you turn them off), and of the End's sky if you like.
 * Sky Lyrics are drawn right after the sun, moon and stars (or the End's sky), before any block is drawn,
 * so the world covers them like it covers the sky.
 */
@Mixin(SkyRenderer.class)
public abstract class SkyRendererMixin {
	@Inject(method = "renderSkyDisc", at = @At("HEAD"), cancellable = true)
	private void eventide$livingSky(int skyColor, CallbackInfo ci) {
		if (!LivingSky.active()) return;
		LivingSky.drawWorld();
		ci.cancel();
	}

	@Inject(method = "renderSunriseAndSunset", at = @At("HEAD"), cancellable = true)
	private void eventide$noSunset(PoseStack poseStack, float sunAngle, int color, CallbackInfo ci) {
		if (LivingSky.active()) ci.cancel();
	}

	@Inject(method = "renderDarkDisc", at = @At("HEAD"), cancellable = true)
	private void eventide$noDarkDisc(CallbackInfo ci) {
		if (LivingSky.active()) ci.cancel();
	}

	@Inject(method = "renderStars", at = @At("HEAD"), cancellable = true)
	private void eventide$noStars(float brightness, PoseStack poseStack, CallbackInfo ci) {
		if (LivingSky.active()) ci.cancel();
	}

	@Inject(method = "renderSun", at = @At("HEAD"), cancellable = true)
	private void eventide$sun(float rainBrightness, PoseStack poseStack, CallbackInfo ci) {
		if (LivingSky.active() && !LivingSky.sunAndMoon()) ci.cancel();
	}

	@Inject(method = "renderMoon", at = @At("HEAD"), cancellable = true)
	private void eventide$moon(MoonPhase phase, float rainBrightness, PoseStack poseStack, CallbackInfo ci) {
		if (LivingSky.active() && !LivingSky.sunAndMoon()) ci.cancel();
	}

	@Inject(method = "renderSunMoonAndStars", at = @At("TAIL"))
	private void eventide$lyricsInSky(PoseStack poseStack, float sunAngle, float moonAngle, float starAngle, MoonPhase moonPhase,
			float rainBrightness, float starBrightness, CallbackInfo ci) {
		SkyLyrics.draw(rainBrightness);
	}

	@Inject(method = "renderEndSky", at = @At("HEAD"), cancellable = true)
	private void eventide$livingEndSky(CallbackInfo ci) {
		if (!LivingSky.activeInEnd()) return;
		LivingSky.drawWorld();
		SkyLyrics.draw(1);
		ci.cancel();
	}

	@Inject(method = "renderEndSky", at = @At("TAIL"))
	private void eventide$lyricsInEndSky(CallbackInfo ci) {
		SkyLyrics.draw(1);
	}
}
