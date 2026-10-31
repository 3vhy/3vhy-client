package dev.eventide.client.mixin;

import net.minecraft.client.gui.components.AbstractSliderButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** A slider's position (0..1), for drawing Minecraft's sliders as Eventide's glass. */
@Mixin(AbstractSliderButton.class)
public interface AbstractSliderButtonAccessor {
	@Accessor("value")
	double eventide$value();
}
