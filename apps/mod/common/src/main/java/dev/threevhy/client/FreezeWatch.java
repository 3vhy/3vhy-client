package dev.threevhy.client;

/**
 * Writes what the game was doing to the log when it stops responding, so a freeze can be traced
 * to the mod (or the game code) that caused it. A watcher thread checks that the game still ticks;
 * after two seconds without a tick it logs the game thread's stack, again every ten seconds while
 * the freeze lasts, and how long the freeze took once it's over. It only reads and logs.
 */
public final class FreezeWatch {
	private static final long REPORT_AFTER_MS = 2_000;
	private static final long REPORT_AGAIN_MS = 10_000;
	private static final int STACK_LINES = 60;

	private static volatile long lastTick = 0;
	private static volatile int freezes = 0;
	private static Thread gameThread;

	private FreezeWatch() {
	}

	/** Called on the game thread once the game has started. */
	public static void start() {
		if (gameThread != null) return;
		gameThread = Thread.currentThread();
		tick();
		Thread watcher = new Thread(FreezeWatch::watch, "3vhy freeze watch");
		watcher.setDaemon(true);
		watcher.setPriority(Thread.MIN_PRIORITY);
		watcher.start();
	}

	/** Called at the end of every client tick. */
	public static void tick() {
		lastTick = System.currentTimeMillis();
	}

	/** How many freezes were logged since the game started (for the in-game test). */
	public static int freezes() {
		return freezes;
	}

	private static void watch() {
		long frozenSince = 0;
		long nextReport = 0;
		while (true) {
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
				return;
			}
			long now = System.currentTimeMillis();
			long last = lastTick;
			if (now - last < REPORT_AFTER_MS) {
				if (frozenSince != 0) {
					ThreevhyClient.LOG.warn("3vhy: the game was frozen for {} ms", last - frozenSince);
					frozenSince = 0;
				}
				continue;
			}
			if (frozenSince == 0) {
				frozenSince = last;
				nextReport = now;
				freezes++;
			}
			if (now >= nextReport) {
				nextReport = now + REPORT_AGAIN_MS;
				ThreevhyClient.LOG.warn("3vhy: the game hasn't responded for {} ms. It was doing this:\n{}", now - last, stack());
			}
		}
	}

	private static String stack() {
		StringBuilder out = new StringBuilder();
		StackTraceElement[] frames = gameThread.getStackTrace();
		for (int i = 0; i < Math.min(frames.length, STACK_LINES); i++) out.append("\tat ").append(frames[i]).append('\n');
		if (frames.length > STACK_LINES) out.append("\t... ").append(frames.length - STACK_LINES).append(" more\n");
		return out.toString();
	}
}
