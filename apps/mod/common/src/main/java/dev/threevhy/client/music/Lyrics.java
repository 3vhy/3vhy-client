package dev.threevhy.client.music;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.threevhy.client.ThreevhyClient;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Time-synced lyrics for the song playing, from LRCLIB (lrclib.net, a free, open lyrics database). They
 * are fetched once per song on a background thread; songs without synced lyrics simply show none.
 */
public final class Lyrics {
	/** One line and when it starts, in seconds. */
	public record Line(double at, String text) {
	}

	private static final Pattern STAMP = Pattern.compile("\\[(\\d+):(\\d+(?:\\.\\d+)?)]");
	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8))
			.followRedirects(HttpClient.Redirect.NORMAL).build();

	private static volatile String songKey = null;
	private static volatile List<Line> lines = List.of();
	private static volatile String status = "";
	private static CompletableFuture<?> request = null;

	private Lyrics() {
	}

	private static String keyOf(NowPlaying song) {
		return song.artist() + "\u0000" + song.title();
	}

	/** Starts fetching the lyrics for this song if they aren't loaded or on their way already. */
	public static void want(NowPlaying song) {
		if (song == null || song.title().isBlank()) return;
		String key = keyOf(song);
		if (key.equals(songKey)) return;
		songKey = key;
		lines = List.of();
		status = "Looking for lyrics";
		if (request != null) request.cancel(true);
		String url = "https://lrclib.net/api/get?artist_name=" + encode(song.artist()) + "&track_name=" + encode(song.title())
				+ (song.album().isBlank() ? "" : "&album_name=" + encode(song.album()))
				+ (song.duration() > 1 ? "&duration=" + Math.round(song.duration()) : "");
		HttpRequest get = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10))
				.header("User-Agent", "3vhy Client " + ThreevhyClient.version() + " (https://github.com/3vhy/3vhy-client)").build();
		request = HTTP.sendAsync(get, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).thenAccept(response -> {
			if (!key.equals(songKey)) return;
			if (response.statusCode() == 404) {
				status = "No lyrics for this song";
				return;
			}
			if (response.statusCode() != 200) {
				status = "Lyrics server said " + response.statusCode();
				return;
			}
			JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
			JsonElement synced = json.get("syncedLyrics");
			if (synced == null || synced.isJsonNull() || synced.getAsString().isBlank()) {
				status = "No synced lyrics for this song";
				return;
			}
			lines = parse(synced.getAsString());
			status = lines.isEmpty() ? "No synced lyrics for this song" : "Showing lyrics";
		}).exceptionally(problem -> {
			if (key.equals(songKey)) status = "Couldn't reach the lyrics server";
			return null;
		});
	}

	private static String encode(String text) {
		return URLEncoder.encode(text, StandardCharsets.UTF_8);
	}

	/** Reads LRC text: lines like {@code [01:23.45] words}, several stamps a line allowed. */
	public static List<Line> parse(String lrc) {
		List<Line> parsed = new ArrayList<>();
		for (String raw : lrc.split("\\R")) {
			Matcher matcher = STAMP.matcher(raw);
			List<Double> times = new ArrayList<>();
			int end = 0;
			while (matcher.find() && matcher.start() == end) {
				times.add(Integer.parseInt(matcher.group(1)) * 60 + Double.parseDouble(matcher.group(2)));
				end = matcher.end();
			}
			String text = raw.substring(end).trim();
			for (double time : times) parsed.add(new Line(time, text));
		}
		parsed.sort((a, b) -> Double.compare(a.at(), b.at()));
		return Collections.unmodifiableList(parsed);
	}

	/** For the in-game test: these lyrics for the song playing now. */
	public static void simulate(NowPlaying song, String lrc) {
		songKey = song == null ? "" : keyOf(song);
		lines = parse(lrc);
		status = "Showing lyrics";
	}

	/** The lines for {@code song}, or none if they belong to another song or haven't come yet. */
	public static List<Line> lines(NowPlaying song) {
		return song != null && keyOf(song).equals(songKey) ? lines : List.of();
	}

	/** The index of the line being sung at {@code seconds}, or -1 before the first. */
	public static int index(List<Line> lines, double seconds) {
		int found = -1;
		for (int i = 0; i < lines.size(); i++) {
			if (lines.get(i).at() > seconds) break;
			found = i;
		}
		return found;
	}

	public static String status() {
		return status;
	}
}
