package dev.threevhy.client.module;

import dev.threevhy.client.Hooks;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;

/**
 * Swing Speed: how long the arm swing takes when you hit, place or use something. Only the
 * animation on your screen changes; attacks and their timing are the same as without it.
 */
public final class SwingSpeed {
	private SwingSpeed() {
	}

	/** The swing's length in ticks for {@code entity}, given Minecraft's own. */
	public static int duration(LivingEntity entity, int vanilla) {
		if (!Modules.SWING_SPEED.enabled() || !entity.level().isClientSide()) return vanilla;
		if (entity != Minecraft.getInstance().player && !Modules.SWING_SPEED_OTHERS.get()) return vanilla;
		Hooks.swingSpeedRan = true;
		return Math.max(1, Math.min(100, (int) Math.round(vanilla / Modules.SWING_SPEED_SPEED.get())));
	}
}
