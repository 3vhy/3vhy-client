package dev.threevhy.client.mixin;

import net.minecraft.client.gui.components.AbstractSliderButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** A slider's position (0..1), for drawing Minecraft's sliders as 3vhy's glass. */
@Mixin(AbstractSliderButton.class)
public interface AbstractSliderButtonAccessor {
	@Accessor("value")
	double threevhy$value();
}
