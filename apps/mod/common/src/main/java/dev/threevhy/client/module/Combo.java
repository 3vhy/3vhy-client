package dev.threevhy.client.module;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Counts the hits you land in a row. Getting hit, or three seconds without a hit, ends the combo. */
public final class Combo {
	private static final long TIMEOUT_MS = 3000;
	private static int count = 0;
	private static long lastHitAt = 0;
	private static LivingEntity pending = null;
	private static int pendingTicks = 0;
	private static int lastHurtTime = 0;

	private Combo() {
	}

	public static int count() {
		return count;
	}

	public static void onAttack(Entity attacked) {
		if (attacked instanceof LivingEntity living) {
			pending = living;
			pendingTicks = 4;
		}
	}

	static void tick(Minecraft mc) {
		LocalPlayer player = mc.player;
		if (player == null) {
			count = 0;
			pending = null;
			return;
		}
		if (player.hurtTime > lastHurtTime) count = 0;
		lastHurtTime = player.hurtTime;

		if (pending != null) {
			// The hit counts once the server shows the target flinching
			if (pending.hurtTime > 0 && pending.hurtTime >= pending.hurtDuration - 3) {
				count++;
				lastHitAt = System.currentTimeMillis();
				pending = null;
			} else if (--pendingTicks <= 0) {
				pending = null;
			}
		}
		if (count > 0 && System.currentTimeMillis() - lastHitAt > TIMEOUT_MS) count = 0;
	}
}
