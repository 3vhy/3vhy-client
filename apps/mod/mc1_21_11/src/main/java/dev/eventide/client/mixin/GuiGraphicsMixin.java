package dev.eventide.client.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.eventide.client.compat.DrawImpl;
import dev.eventide.client.gui.Glass;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Minecraft's widget art (text fields, checkboxes, tabs, scrollbars, tooltips, header lines, list and screen
 * backgrounds) drawn as glass, whoever draws it: Minecraft's screens and any mod's.
 */
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {
	@Inject(method = "blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V", at = @At("HEAD"),
			cancellable = true)
	private void eventide$glassSprite(RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height, int color, CallbackInfo ci) {
		if (Glass.handles(sprite) && Glass.sprite(new DrawImpl((GuiGraphics) (Object) this), sprite, x, y, width, height, color)) ci.cancel();
	}

	@Inject(method = "blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIIIIII)V", at = @At("HEAD"),
			cancellable = true)
	private void eventide$glassTexture(RenderPipeline pipeline, Identifier texture, int x, int y, float u, float v, int width, int height,
			int regionWidth, int regionHeight, int textureWidth, int textureHeight, int color, CallbackInfo ci) {
		if (Glass.handlesTexture(texture) && Glass.texture(new DrawImpl((GuiGraphics) (Object) this), texture, x, y, width, height)) ci.cancel();
	}
}
