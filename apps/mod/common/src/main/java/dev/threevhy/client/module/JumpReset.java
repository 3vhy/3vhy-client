package dev.threevhy.client.module;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Jump reset trainer: jumping the moment you're hit cancels some of the knockback. This times
 * your jump after each hit you take on the ground and grades it for a couple of seconds.
 */
public final class JumpReset {
	public record Result(String text, int color) {
	}

	private static final long SHOW_MS = 2500;
	private static long tick = 0;
	private static long hitTick = -1;
	private static int lastHurtTime = 0;
	private static boolean wasOnGround = true;
	private static Result result = null;
	private static long resultAt = 0;

	private JumpReset() {
	}

	/** The latest grade, while it's still being shown. */
	public static Result result() {
		if (result != null && System.currentTimeMillis() - resultAt > SHOW_MS) result = null;
		return result;
	}

	static void tick(Minecraft mc) {
		tick++;
		LocalPlayer player = mc.player;
		if (player == null || !Modules.JUMP_RESET.enabled()) {
			hitTick = -1;
			return;
		}
		boolean onGround = player.onGround();
		if (player.hurtTime > lastHurtTime && onGround) hitTick = tick;
		lastHurtTime = player.hurtTime;

		if (hitTick >= 0) {
			long late = tick - hitTick;
			boolean jumped = wasOnGround && !onGround && player.getDeltaMovement().y > 0.2;
			if (jumped) {
				show(late <= 1 ? new Result("Perfect", 0xFF4ADE80)
						: late <= 3 ? new Result("Good +" + late + "t", 0xFFFFD166)
						: new Result("Late +" + late + "t", 0xFFFF9F43));
				hitTick = -1;
			} else if (late > 10) {
				show(new Result("Missed", 0xFFFF6B81));
				hitTick = -1;
			}
		}
		wasOnGround = onGround;
	}

	private static void show(Result next) {
		result = next;
		resultAt = System.currentTimeMillis();
	}
}
