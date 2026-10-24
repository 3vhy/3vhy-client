package dev.threevhy.client.hud;

import dev.threevhy.client.music.Lyrics;
import dev.threevhy.client.music.NowPlaying;
import dev.threevhy.client.ui.Ease;
import java.util.List;

/**
 * Follows the song's lyrics as it plays: the line being sung, the one before it (still fading out), the
 * next one, and how far through the current line the singer is, for the karaoke fill.
 */
final class LyricTracker {
	private static final long CHANGE_MS = 520;

	private int index = -2;
	private String current = "";
	private String previous = "";
	private String next = "";
	private long changedAt = 0;
	private double lineAt = 0;
	private double lineEnd = 0;
	private boolean any = false;

	/** Catches up with {@code song}; returns whether a new line started. */
	boolean update(NowPlaying song) {
		if (song == null) {
			index = -2;
			current = "";
			previous = "";
			next = "";
			any = false;
			return false;
		}
		Lyrics.want(song);
		List<Lyrics.Line> lines = Lyrics.lines(song);
		any = !lines.isEmpty();
		int found = Lyrics.index(lines, song.positionNow() + 0.12);
		if (found == index) return false;
		index = found;
		previous = current;
		current = found < 0 ? "" : lines.get(found).text();
		lineAt = found < 0 ? 0 : lines.get(found).at();
		lineEnd = found + 1 < lines.size() ? lines.get(found + 1).at() : lineAt + 4;
		next = "";
		for (int i = found + 1; i < lines.size(); i++) {
			if (!lines.get(i).text().isBlank()) {
				next = lines.get(i).text();
				break;
			}
		}
		changedAt = System.currentTimeMillis();
		return true;
	}

	/** Whether the song has synced lyrics at all. */
	boolean any() {
		return any;
	}

	String current() {
		return current;
	}

	String previous() {
		return previous;
	}

	String next() {
		return next;
	}

	/** How far the change to the current line has gone, 0..1. */
	float change() {
		return Ease.OUT.apply((System.currentTimeMillis() - changedAt) / (double) CHANGE_MS);
	}

	/** How much of the current line has been sung, 0..1. */
	float sung(NowPlaying song) {
		if (song == null || lineEnd <= lineAt) return 1;
		return (float) Math.max(0, Math.min(1, (song.positionNow() - lineAt) / Math.min(lineEnd - lineAt, 8)));
	}
}
