package dev.threevhy.client.mixin;

import net.minecraft.client.OptionInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Lets zoom change the field of view past the 30-110 slider range without going through
 * OptionInstance.set (which would reject the value and save it).
 */
@Mixin(OptionInstance.class)
public interface OptionInstanceAccessor {
	@Accessor("value")
	void threevhy$setValue(Object value);
}
