package dev.threevhy.client;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;

/**
 * Writes what the game was doing to the log when it stalls, so lag spikes and freezes can be traced
 * to the mod (or the game code) that caused them. A watcher thread checks that the game still ticks.
 * When a tick is 0.3 seconds late it notes what the game thread is doing; once the game recovers
 * it logs how long the stall took, how much of it was Java cleaning up memory, and that stack.
 * Stalls over two seconds are also logged while they last. It only reads and logs.
 */
public final class FreezeWatch {
	private static final long STALL_MS = 300;
	private static final long FREEZE_MS = 2_000;
	private static final long REPORT_AGAIN_MS = 10_000;
	private static final int STACK_LINES = 40;
	/** Short stalls logged per session, so a stuttering game doesn't flood the log. */
	private static final int MAX_STALL_REPORTS = 40;

	private static volatile long lastTick = 0;
	private static volatile int freezes = 0;
	private static Thread gameThread;
	private static int stallReports = 0;

	private FreezeWatch() {
	}

	/** Called on the game thread once the game has started. */
	public static void start() {
		if (gameThread != null) return;
		gameThread = Thread.currentThread();
		Runtime runtime = Runtime.getRuntime();
		ThreevhyClient.LOG.info("3vhy: Java {}, memory for Minecraft {} MB, PC memory {} MB, {} CPU threads",
				System.getProperty("java.version"), runtime.maxMemory() / (1024 * 1024), pcMemoryMb(), runtime.availableProcessors());
		tick();
		Thread watcher = new Thread(FreezeWatch::watch, "3vhy freeze watch");
		watcher.setDaemon(true);
		watcher.start();
	}

	/** Called at the end of every client tick. */
	public static void tick() {
		lastTick = System.currentTimeMillis();
	}

	/** How many freezes (stalls over two seconds) were logged since the game started (for the in-game test). */
	public static int freezes() {
		return freezes;
	}

	private static void watch() {
		long stalledSince = 0;
		long gcAtStart = 0;
		String stackAtStart = null;
		boolean frozen = false;
		long nextReport = 0;
		while (true) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				return;
			}
			long now = System.currentTimeMillis();
			long last = lastTick;
			if (now - last < STALL_MS) {
				if (stalledSince != 0) {
					long length = last - stalledSince;
					long gc = gcMillis() - gcAtStart;
					if (frozen) {
						ThreevhyClient.LOG.warn("3vhy: the game was frozen for {} ms ({} ms of it cleaning up memory)", length, gc);
					} else if (length >= STALL_MS && stallReports < MAX_STALL_REPORTS) {
						stallReports++;
						ThreevhyClient.LOG.warn("3vhy: lag spike of {} ms ({} ms of it cleaning up memory). The game was doing this:\n{}",
								length, gc, stackAtStart);
					}
					stalledSince = 0;
					frozen = false;
				}
				continue;
			}
			if (stalledSince == 0) {
				stalledSince = last;
				gcAtStart = gcMillis();
				stackAtStart = stack();
			}
			if (now - last >= FREEZE_MS) {
				if (!frozen) {
					frozen = true;
					freezes++;
					nextReport = now;
				}
				if (now >= nextReport) {
					nextReport = now + REPORT_AGAIN_MS;
					ThreevhyClient.LOG.warn("3vhy: the game hasn't responded for {} ms. It was doing this:\n{}", now - last, stack());
				}
			}
		}
	}

	private static long gcMillis() {
		long total = 0;
		for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
			long time = bean.getCollectionTime();
			if (time > 0) total += time;
		}
		return total;
	}

	private static long pcMemoryMb() {
		try {
			if (ManagementFactory.getOperatingSystemMXBean() instanceof com.sun.management.OperatingSystemMXBean os) {
				return os.getTotalMemorySize() / (1024 * 1024);
			}
		} catch (Throwable ignored) {
			// not available on this Java
		}
		return -1;
	}

	private static String stack() {
		StringBuilder out = new StringBuilder();
		StackTraceElement[] frames = gameThread.getStackTrace();
		for (int i = 0; i < Math.min(frames.length, STACK_LINES); i++) out.append("\tat ").append(frames[i]).append('\n');
		if (frames.length > STACK_LINES) out.append("\t... ").append(frames.length - STACK_LINES).append(" more\n");
		return out.toString();
	}
}
