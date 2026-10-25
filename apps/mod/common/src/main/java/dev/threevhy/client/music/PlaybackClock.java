package dev.threevhy.client.music;

/**
 * Where the song playing is, smooth between Windows' once-a-second reports. Small differences between
 * a report and the running clock are eased in rather than jumped to, so lyrics don't flicker back and
 * forth; a big difference is a seek, skip or replay and is followed at once. Pausing stops the clock.
 * Some apps (browsers, mostly) report no position at all while they play; then the clock counts from
 * when the song started on its own.
 */
final class PlaybackClock {
	private static final double SEEK = 1.2;
	private static final int STUCK_REPORTS = 3;

	private String key = null;
	private double base = 0;
	private long baseAt = 0;
	private boolean playing = false;
	private double lastRaw = Double.NaN;
	private int sameRaw = 0;
	private boolean ownClock = false;

	/** Takes a new report from Windows. Called on the music thread. */
	synchronized void report(NowPlaying song) {
		long now = System.nanoTime();
		boolean newSong = !song.key().equals(key);
		if (newSong) {
			key = song.key();
			lastRaw = song.position();
			sameRaw = 0;
			ownClock = !song.hasTimeline();
			base = ownClock ? 0 : song.position();
			baseAt = now;
			playing = song.playing();
			return;
		}
		if (song.playing() && song.position() == lastRaw) {
			sameRaw++;
		} else {
			sameRaw = 0;
		}
		lastRaw = song.position();
		boolean stuck = song.playing() && sameRaw >= STUCK_REPORTS;
		if (!song.hasTimeline() || stuck) {
			if (!ownClock) {
				ownClock = true;
				base = seconds(now);
				baseAt = now;
			}
		} else if (ownClock) {
			ownClock = false;
			base = song.position();
			baseAt = now;
		}
		if (song.playing() != playing) {
			base = ownClock ? seconds(now) : song.position();
			baseAt = now;
			playing = song.playing();
			return;
		}
		if (ownClock) return;
		double predicted = seconds(now);
		double reported = song.position();
		double off = reported - predicted;
		if (Math.abs(off) > SEEK) {
			base = reported;
		} else {
			base = predicted + off * 0.3;
		}
		baseAt = now;
	}

	synchronized void clear() {
		key = null;
	}

	private double seconds(long now) {
		return playing ? base + (now - baseAt) / 1_000_000_000.0 : base;
	}

	/** Seconds into {@code song} now, or its own estimate when this clock follows another song. */
	synchronized double seconds(NowPlaying song) {
		if (song == null) return 0;
		if (!song.key().equals(key)) return song.positionNow();
		double seconds = seconds(System.nanoTime());
		return song.duration() > 1 ? Math.min(seconds, song.duration()) : Math.max(0, seconds);
	}
}
