package dev.threevhy.client.module;

/**
 * Estimates the server's ticks per second. Servers send the world time about once a second;
 * comparing how many game ticks passed with how much real time passed gives the TPS.
 */
public final class TpsTracker {
	private static long lastGameTime = -1;
	private static long lastNanos = 0;
	private static double tps = -1;

	private TpsTracker() {
	}

	public static void onTime(long gameTime) {
		long now = System.nanoTime();
		if (lastGameTime >= 0) {
			long ticks = gameTime - lastGameTime;
			double seconds = (now - lastNanos) / 1_000_000_000.0;
			if (ticks > 0 && seconds > 0.25) {
				double sample = Math.min(20.0, ticks / seconds);
				tps = tps < 0 ? sample : tps * 0.6 + sample * 0.4;
			}
		}
		lastGameTime = gameTime;
		lastNanos = now;
	}

	/** The estimated TPS, or -1 before the server has sent the time twice. */
	public static double tps() {
		return tps;
	}

	public static void reset() {
		lastGameTime = -1;
		tps = -1;
	}
}
