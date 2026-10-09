package dev.threevhy.client.module;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Who you're fighting: the last living thing you hit, or the player under your crosshair. */
public final class Target {
	private static final long FORGET_AFTER_MS = 6_000;

	private static LivingEntity entity = null;
	private static long lastSeen = 0;

	private Target() {
	}

	public static void onAttack(Entity attacked) {
		if (attacked instanceof LivingEntity living) {
			entity = living;
			lastSeen = System.currentTimeMillis();
		}
	}

	static void tick(Minecraft mc) {
		long now = System.currentTimeMillis();
		if (mc.level == null || mc.player == null) {
			entity = null;
			return;
		}
		if (mc.crosshairPickEntity instanceof Player player && player != mc.player) {
			entity = player;
			lastSeen = now;
		}
		if (entity != null && (entity.isRemoved() || entity.level() != mc.level || now - lastSeen > FORGET_AFTER_MS)) {
			entity = null;
		}
	}

	public static LivingEntity get() {
		return entity;
	}
}
