package dev.threevhy.client.mixin;

import java.util.Map;
import net.minecraft.client.gui.components.BossHealthOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** How many boss bars are showing, so widgets at the top centre sit below them. */
@Mixin(BossHealthOverlay.class)
public interface BossHealthOverlayAccessor {
	@Accessor("events")
	Map<?, ?> threevhy$events();
}
