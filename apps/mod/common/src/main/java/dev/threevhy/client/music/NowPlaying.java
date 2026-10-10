package dev.threevhy.client.music;

import java.util.Locale;

/**
 * What Windows says is playing, as of {@code receivedAt} (System.nanoTime). {@code artVersion} goes up
 * whenever the cover changes, so the texture is only uploaded once per song.
 */
public record NowPlaying(
		String key,
		String app,
		String title,
		String artist,
		String album,
		boolean playing,
		double position,
		double duration,
		long receivedAt,
		byte[] art,
		long artVersion) {

	/** Seconds into the song right now, counting time since the last update while it plays. */
	public double positionNow() {
		double seconds = position;
		if (playing) seconds += (System.nanoTime() - receivedAt) / 1_000_000_000.0;
		return duration > 0 ? Math.min(seconds, duration) : seconds;
	}

	public boolean hasTimeline() {
		return duration > 1;
	}

	/** The app's name as people know it, from its Windows app id (e.g. "Spotify.exe"). */
	public String appName() {
		String id = app.toLowerCase(Locale.ROOT);
		if (id.contains("spotify")) return "Spotify";
		if (id.contains("applemusic") || id.contains("itunes")) return "Apple Music";
		if (id.contains("zunemusic") || id.contains("mediaplayer")) return "Media Player";
		if (id.contains("tidal")) return "TIDAL";
		if (id.contains("deezer")) return "Deezer";
		if (id.contains("soundcloud")) return "SoundCloud";
		if (id.contains("amazon")) return "Amazon Music";
		if (id.contains("vlc")) return "VLC";
		if (id.contains("chrome") || id.contains("msedge") || id.contains("firefox") || id.contains("opera")
				|| id.contains("brave") || id.contains("vivaldi")) {
			return "Browser";
		}
		String name = app;
		int bang = name.indexOf('!');
		if (bang >= 0) name = name.substring(bang + 1);
		if (name.toLowerCase(Locale.ROOT).endsWith(".exe")) name = name.substring(0, name.length() - 4);
		int dot = name.lastIndexOf('.');
		if (dot >= 0 && dot < name.length() - 1) name = name.substring(dot + 1);
		return name.isBlank() ? "Music" : name;
	}

	/** A colour for the app's badge, so you can tell at a glance where the music comes from. */
	public int appColor() {
		return switch (appName()) {
			case "Spotify" -> 0xFF1ED760;
			case "Apple Music" -> 0xFFFA586A;
			case "Media Player" -> 0xFF4CC2FF;
			case "TIDAL" -> 0xFFE3F3FC;
			case "Deezer" -> 0xFFA238FF;
			case "SoundCloud" -> 0xFFFF7700;
			case "Amazon Music" -> 0xFF25D1DA;
			case "VLC" -> 0xFFFF8800;
			case "Browser" -> 0xFFFF4E45;
			default -> 0xFF5BC8E6;
		};
	}

	/** "1:05", or "1:02:05" past an hour. */
	public static String time(double seconds) {
		long total = Math.max(0, Math.round(Math.floor(seconds)));
		long hours = total / 3600;
		long minutes = (total % 3600) / 60;
		long secs = total % 60;
		if (hours > 0) return String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, secs);
		return String.format(Locale.ROOT, "%d:%02d", minutes, secs);
	}
}
