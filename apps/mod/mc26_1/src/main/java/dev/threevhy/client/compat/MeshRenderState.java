package dev.threevhy.client.compat;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;

/**
 * One of 3vhy's shapes for Minecraft's GUI renderer: a batch of quads, each corner with its own colour
 * (and texture coordinates when textured). Smooth boxes, gradients, glows and icons are all made of
 * these. Minecraft 26.1 version.
 */
record MeshRenderState(RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2f pose, float[] xy, float[] uv, int[] colors,
		ScreenRectangle scissorArea, ScreenRectangle bounds) implements GuiElementRenderState {
	@Override
	public void buildVertices(VertexConsumer consumer) {
		boolean textured = uv != null;
		for (int i = 0; i < colors.length; i++) {
			VertexConsumer vertex = consumer.addVertexWith2DPose(pose, xy[2 * i], xy[2 * i + 1]);
			if (textured) vertex.setUv(uv[2 * i], uv[2 * i + 1]);
			vertex.setColor(colors[i]);
		}
	}
}
