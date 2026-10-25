package dev.threevhy.client.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import dev.threevhy.client.ui.VertexArena;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Once the frame's GUI is drawn, 3vhy's shapes are done with their vertices, so the block they used is reused. */
@Mixin(GuiRenderer.class)
public abstract class GuiRendererMixin {
	@Inject(method = "render", at = @At("TAIL"))
	private void threevhy$reuseVertices(GpuBufferSlice fog, CallbackInfo ci) {
		VertexArena.reset();
	}
}
