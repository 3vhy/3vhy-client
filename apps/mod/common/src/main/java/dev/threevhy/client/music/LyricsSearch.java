package dev.threevhy.client.music;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Looks up a song's lyrics in a chain of free lyrics sources: LRCLIB first (exact lookups for each
 * query variant, then its search), then NetEase Cloud Music for synced lyrics LRCLIB lacks (it covers a
 * lot of Asian and new music), then lyrics.ovh for plain lyrics. Synced lyrics from any source win over
 * plain ones. Busy servers and timeouts are retried a couple of times, and every step is reported to a
 * {@link Log}, so a song without lyrics always comes with the reason. Plain Java: no game classes.
 */
public final class LyricsSearch {
	/** What was found for a song. */
	public enum Kind {
		SYNCED, PLAIN, INSTRUMENTAL, NONE
	}

	/** Lyrics for a song: LRC text when synced, plain text otherwise, and where they came from. */
	public record Found(Kind kind, String lrc, String plain, String provider, double score, String title, String artist, double duration) {
	}

	/** The search's result: what was found (or null), whether it failed for a reason worth retrying soon, and why. */
	public record Outcome(Found found, boolean retrySoon, String reason) {
	}

	/** Where each step of a search is written. */
	public interface Log {
		void line(String text);
	}

	/** One song a lyrics source offered, with its lyrics if they came with it. */
	private record Candidate(String id, String title, String artist, double duration, String lrc, String plain, boolean instrumental) {
	}

	/** An answer from a server, or why there wasn't one. */
	private record Reply(int status, String body, String problem, boolean transientProblem) {
		boolean ok() {
			return status == 200 && body != null;
		}
	}

	private static final double ACCEPT = 0.72;
	private static final long DEADLINE_MS = 45_000;
	private static final long[] RETRY_WAIT_MS = {1500, 4000};
	private static final Pattern STAMP = Pattern.compile("\\[\\d+:\\d+(?:[.:]\\d+)?]");
	private static final Pattern CREDIT = Pattern.compile("^\\s*(?:作词|作曲|编曲|制作人|混音|母带|和声|吉他|贝斯|鼓|弦乐|录音|监制|出品|发行|词|曲|"
			+ "lyrics by|composed by|produced by|written by|arranged by|mixed by|mastered by|vocals? by)\\s*[:：]", Pattern.CASE_INSENSITIVE);

	private final HttpClient http;
	private final String userAgent;
	private long deadline;

	public LyricsSearch(String userAgent) {
		this.userAgent = userAgent;
		this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).followRedirects(HttpClient.Redirect.NORMAL).build();
	}

	/** Searches every source for the song. Blocks; call it off the render thread. */
	public Outcome find(String title, String artist, double duration, Log log) {
		deadline = System.currentTimeMillis() + DEADLINE_MS;
		List<LyricsQuery.Variant> variants = LyricsQuery.variants(title, artist);
		log.line("Looking up \"" + title + "\" by \"" + artist + "\"" + (duration > 1 ? String.format(Locale.ROOT, " (%.0fs)", duration) : "")
				+ "; " + variants.size() + " query variants");
		for (LyricsQuery.Variant variant : variants) log.line("  variant: \"" + variant.title() + "\" by \"" + variant.artist() + "\" (" + variant.why() + ")");
		List<String> problems = new ArrayList<>();
		boolean transientSeen = false;
		Found plain = null;
		Found instrumental = null;

		for (LyricsQuery.Variant variant : variants) {
			if (late()) break;
			for (boolean withDuration : duration > 1 ? new boolean[] {true, false} : new boolean[] {false}) {
				String url = "https://lrclib.net/api/get?artist_name=" + enc(variant.artist()) + "&track_name=" + enc(variant.title())
						+ (withDuration ? "&duration=" + Math.round(duration) : "");
				Reply reply = get(url, log, "LRCLIB get (" + variant.why() + (withDuration ? ", with length" : "") + ")");
				if (reply.transientProblem()) transientSeen = true;
				if (reply.status() == 404) continue;
				if (!reply.ok()) {
					problems.add("LRCLIB: " + reply.problem());
					break;
				}
				Candidate candidate = lrclib(parseObject(reply.body(), log));
				if (candidate == null) {
					problems.add("LRCLIB: an answer that couldn't be read");
					break;
				}
				double score = LyricsQuery.score(title, artist, duration, candidate.title(), candidate.artist(), candidate.duration());
				log.line(String.format(Locale.ROOT, "  LRCLIB matched \"%s\" by \"%s\" (%.0fs), score %.2f, %s", candidate.title(),
						candidate.artist(), candidate.duration(), score, describe(candidate)));
				Found found = toFound(candidate, "LRCLIB", Math.max(score, ACCEPT));
				if (found.kind() == Kind.SYNCED) return done(found, log);
				if (found.kind() == Kind.PLAIN && plain == null) plain = found;
				if (found.kind() == Kind.INSTRUMENTAL && instrumental == null) instrumental = found;
				break;
			}
			if (plain != null || instrumental != null) break;
		}

		String cleanTitle = LyricsQuery.cleanTitle(title);
		String firstArtist = LyricsQuery.firstArtist(artist);
		String[] video = LyricsQuery.splitVideoTitle(title, artist);
		if (video != null) {
			cleanTitle = LyricsQuery.cleanTitle(video[1]);
			firstArtist = LyricsQuery.firstArtist(video[0]);
		}
		String[] searches = {
				"https://lrclib.net/api/search?track_name=" + enc(cleanTitle) + "&artist_name=" + enc(firstArtist),
				"https://lrclib.net/api/search?q=" + enc(firstArtist + " " + cleanTitle)};
		for (String url : searches) {
			if (late()) break;
			Reply reply = get(url, log, "LRCLIB search");
			if (reply.transientProblem()) transientSeen = true;
			if (!reply.ok()) {
				if (reply.status() != 404) problems.add("LRCLIB search: " + reply.problem());
				continue;
			}
			List<Candidate> candidates = new ArrayList<>();
			JsonArray results = parseArray(reply.body(), log);
			if (results != null) {
				for (JsonElement each : results) {
					Candidate candidate = each.isJsonObject() ? lrclib(each.getAsJsonObject()) : null;
					if (candidate != null) candidates.add(candidate);
				}
			}
			Found best = best(candidates, title, artist, duration, "LRCLIB search", log);
			if (best != null && best.kind() == Kind.SYNCED) return done(best, log);
			if (best != null && best.kind() == Kind.PLAIN && plain == null) plain = best;
			if (best != null && best.kind() == Kind.INSTRUMENTAL && instrumental == null) instrumental = best;
			if (best == null) problems.add("LRCLIB search: nothing close enough");
		}

		if (!late() && instrumental == null) {
			Found netease = netease(cleanTitle, firstArtist, title, artist, duration, log, problems);
			if (netease != null && netease.kind() == Kind.SYNCED) return done(netease, log);
			if (netease != null && plain == null && netease.kind() == Kind.PLAIN) plain = netease;
			if (lastTransient) transientSeen = true;
		}

		if (plain == null && instrumental == null && !late()) {
			Reply reply = get("https://api.lyrics.ovh/v1/" + encPath(firstArtist) + "/" + encPath(cleanTitle), log, "lyrics.ovh");
			if (reply.transientProblem()) transientSeen = true;
			if (reply.ok()) {
				JsonObject json = parseObject(reply.body(), log);
				String text = json == null ? "" : string(json, "lyrics");
				if (!text.isBlank()) {
					log.line("  lyrics.ovh has plain lyrics (" + text.lines().count() + " lines)");
					plain = new Found(Kind.PLAIN, "", stripSections(text), "lyrics.ovh", ACCEPT, cleanTitle, firstArtist, 0);
				}
			} else if (reply.status() != 404) {
				problems.add("lyrics.ovh: " + reply.problem());
			}
		}

		if (plain != null) return done(plain, log);
		if (instrumental != null) return done(instrumental, log);
		boolean timedOut = late();
		String listed = String.join("; ", new java.util.LinkedHashSet<>(problems));
		String reason = timedOut ? "the search took too long" + (problems.isEmpty() ? "" : " (" + listed + ")")
				: problems.isEmpty() ? "not on LRCLIB, NetEase or lyrics.ovh" : listed;
		log.line("  no lyrics: " + reason);
		return new Outcome(null, transientSeen || timedOut, reason);
	}

	private boolean lastTransient = false;

	/** NetEase Cloud Music: search, pick the closest song, then fetch its LRC. */
	private Found netease(String cleanTitle, String firstArtist, String title, String artist, double duration, Log log, List<String> problems) {
		lastTransient = false;
		Reply reply = get("https://music.163.com/api/search/get?type=1&limit=12&s=" + enc(cleanTitle + " " + firstArtist), log, "NetEase search");
		lastTransient = reply.transientProblem();
		if (!reply.ok()) {
			problems.add("NetEase: " + reply.problem());
			return null;
		}
		JsonObject json = parseObject(reply.body(), log);
		JsonObject result = json != null && json.has("result") && json.get("result").isJsonObject() ? json.getAsJsonObject("result") : null;
		JsonArray songs = result != null && result.has("songs") && result.get("songs").isJsonArray() ? result.getAsJsonArray("songs") : null;
		if (songs == null || songs.isEmpty()) {
			problems.add("NetEase: no songs found");
			return null;
		}
		List<Candidate> candidates = new ArrayList<>();
		for (JsonElement each : songs) {
			if (!each.isJsonObject()) continue;
			JsonObject song = each.getAsJsonObject();
			StringBuilder artists = new StringBuilder();
			if (song.has("artists") && song.get("artists").isJsonArray()) {
				for (JsonElement a : song.getAsJsonArray("artists")) {
					if (!a.isJsonObject()) continue;
					if (!artists.isEmpty()) artists.append(", ");
					artists.append(string(a.getAsJsonObject(), "name"));
				}
			}
			double length = song.has("duration") && song.get("duration").isJsonPrimitive() ? song.get("duration").getAsDouble() / 1000.0 : 0;
			candidates.add(new Candidate(string(song, "id"), string(song, "name"), artists.toString(), length, null, null, false));
		}
		Candidate chosen = null;
		double chosenScore = 0;
		for (Candidate candidate : candidates) {
			double score = LyricsQuery.score(title, artist, duration, candidate.title(), candidate.artist(), candidate.duration());
			if (score > chosenScore) {
				chosenScore = score;
				chosen = candidate;
			}
		}
		if (chosen == null || chosenScore < ACCEPT) {
			problems.add(chosen == null ? "NetEase: no songs found" : String.format(Locale.ROOT, "NetEase: closest was \"%s\" by \"%s\" (score %.2f)",
					chosen.title(), chosen.artist(), chosenScore));
			log.line(String.format(Locale.ROOT, "  NetEase: best of %d is \"%s\", score %.2f: too low", candidates.size(),
					chosen == null ? "-" : chosen.title(), chosenScore));
			return null;
		}
		log.line(String.format(Locale.ROOT, "  NetEase picked \"%s\" by \"%s\" (%.0fs), score %.2f", chosen.title(), chosen.artist(), chosen.duration(),
				chosenScore));
		Reply lyric = get("https://music.163.com/api/song/lyric?lv=1&kv=1&tv=-1&id=" + enc(chosen.id()), log, "NetEase lyrics");
		lastTransient |= lyric.transientProblem();
		if (!lyric.ok()) {
			problems.add("NetEase lyrics: " + lyric.problem());
			return null;
		}
		JsonObject body = parseObject(lyric.body(), log);
		if (body == null) return null;
		if (body.has("nolyric") && body.get("nolyric").getAsBoolean()) {
			log.line("  NetEase: marked as having no lyrics (instrumental)");
			return new Found(Kind.INSTRUMENTAL, "", "", "NetEase", chosenScore, chosen.title(), chosen.artist(), chosen.duration());
		}
		JsonObject lrc = body.has("lrc") && body.get("lrc").isJsonObject() ? body.getAsJsonObject("lrc") : null;
		String text = lrc == null ? "" : stripCredits(string(lrc, "lyric"));
		if (STAMP.matcher(text).find() && !Lyrics.parse(text).isEmpty()) {
			return new Found(Kind.SYNCED, text, "", "NetEase", chosenScore, chosen.title(), chosen.artist(), chosen.duration());
		}
		if (!text.isBlank()) return new Found(Kind.PLAIN, "", text, "NetEase", chosenScore, chosen.title(), chosen.artist(), chosen.duration());
		problems.add("NetEase: the song has no lyrics there");
		return null;
	}

	/** The best-scoring candidate that's close enough, preferring synced lyrics when two are about as close. */
	private Found best(List<Candidate> candidates, String title, String artist, double duration, String provider, Log log) {
		Candidate best = null;
		double bestScore = 0;
		Candidate bestAny = null;
		double bestAnyScore = 0;
		for (Candidate candidate : candidates) {
			double score = LyricsQuery.score(title, artist, duration, candidate.title(), candidate.artist(), candidate.duration());
			if (score > bestAnyScore) {
				bestAnyScore = score;
				bestAny = candidate;
			}
			double ranked = score + (candidate.lrc() != null && !candidate.lrc().isBlank() ? 0.06 : 0);
			if (score >= ACCEPT && ranked > bestScore) {
				bestScore = ranked;
				best = candidate;
			}
		}
		if (best == null) {
			log.line(String.format(Locale.ROOT, "  %s: %d results, closest \"%s\" by \"%s\" scored %.2f: too low", provider, candidates.size(),
					bestAny == null ? "-" : bestAny.title(), bestAny == null ? "-" : bestAny.artist(), bestAnyScore));
			return null;
		}
		double score = LyricsQuery.score(title, artist, duration, best.title(), best.artist(), best.duration());
		log.line(String.format(Locale.ROOT, "  %s: %d results, picked \"%s\" by \"%s\" (%.0fs), score %.2f, %s", provider, candidates.size(),
				best.title(), best.artist(), best.duration(), score, describe(best)));
		return toFound(best, provider.startsWith("LRCLIB") ? "LRCLIB" : provider, score);
	}

	private static Found toFound(Candidate candidate, String provider, double score) {
		if (candidate.lrc() != null && !candidate.lrc().isBlank() && !Lyrics.parse(candidate.lrc()).isEmpty()) {
			return new Found(Kind.SYNCED, candidate.lrc(), candidate.plain() == null ? "" : candidate.plain(), provider, score, candidate.title(),
					candidate.artist(), candidate.duration());
		}
		if (candidate.plain() != null && !candidate.plain().isBlank()) {
			return new Found(Kind.PLAIN, "", candidate.plain(), provider, score, candidate.title(), candidate.artist(), candidate.duration());
		}
		if (candidate.instrumental()) {
			return new Found(Kind.INSTRUMENTAL, "", "", provider, score, candidate.title(), candidate.artist(), candidate.duration());
		}
		return new Found(Kind.NONE, "", "", provider, score, candidate.title(), candidate.artist(), candidate.duration());
	}

	private static String describe(Candidate candidate) {
		if (candidate.lrc() != null && !candidate.lrc().isBlank()) return "synced";
		if (candidate.plain() != null && !candidate.plain().isBlank()) return "plain only";
		return candidate.instrumental() ? "instrumental" : "no lyrics";
	}

	private static Outcome done(Found found, Log log) {
		log.line(String.format(Locale.ROOT, "  using %s lyrics from %s (score %.2f)", found.kind().name().toLowerCase(Locale.ROOT), found.provider(),
				found.score()));
		return new Outcome(found, false, "");
	}

	private static Candidate lrclib(JsonObject json) {
		if (json == null) return null;
		String title = string(json, "trackName");
		if (title.isEmpty()) title = string(json, "name");
		double duration = json.has("duration") && json.get("duration").isJsonPrimitive() ? json.get("duration").getAsDouble() : 0;
		boolean instrumental = json.has("instrumental") && json.get("instrumental").isJsonPrimitive() && json.get("instrumental").getAsBoolean();
		return new Candidate(string(json, "id"), title, string(json, "artistName"), duration, string(json, "syncedLyrics"), string(json, "plainLyrics"),
				instrumental);
	}

	private boolean late() {
		return System.currentTimeMillis() > deadline;
	}

	/** One GET, retried when the server is busy (429 or 5xx) or doesn't answer in time. */
	private Reply get(String url, Log log, String what) {
		Reply reply = null;
		for (int attempt = 0; attempt <= RETRY_WAIT_MS.length; attempt++) {
			if (attempt > 0) {
				long wait = RETRY_WAIT_MS[attempt - 1];
				if (System.currentTimeMillis() + wait > deadline) break;
				log.line("  " + what + ": " + reply.problem() + ", trying again in " + wait / 1000.0 + "s");
				try {
					Thread.sleep(wait);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return new Reply(0, null, "stopped", true);
				}
			}
			reply = once(url);
			if (!reply.transientProblem()) break;
		}
		if (reply.ok()) {
			log.line("  " + what + ": answered");
		} else {
			log.line("  " + what + ": " + reply.problem());
		}
		return reply;
	}

	private Reply once(String url) {
		HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(9)).header("User-Agent", userAgent)
				.header("Accept", "application/json").GET().build();
		try {
			HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			int status = response.statusCode();
			if (status == 200) return new Reply(200, response.body(), "", false);
			if (status == 404) return new Reply(404, null, "not found", false);
			boolean busy = status == 429 || status >= 500;
			String message = shortMessage(response.body());
			return new Reply(status, null, "HTTP " + status + (message.isEmpty() ? "" : " (" + message + ")"), busy);
		} catch (HttpTimeoutException e) {
			return new Reply(0, null, "timed out", true);
		} catch (IOException e) {
			return new Reply(0, null, "couldn't connect (" + e.getClass().getSimpleName() + (e.getMessage() == null ? "" : ": " + e.getMessage()) + ")",
					true);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return new Reply(0, null, "stopped", true);
		} catch (RuntimeException e) {
			return new Reply(0, null, "request failed (" + e + ")", false);
		}
	}

	private static String shortMessage(String body) {
		if (body == null || body.isBlank()) return "";
		try {
			JsonElement json = JsonParser.parseString(body);
			if (json.isJsonObject()) {
				String message = string(json.getAsJsonObject(), "message");
				if (!message.isEmpty()) return message.length() > 80 ? message.substring(0, 80) : message;
			}
		} catch (RuntimeException ignored) {
			// Not JSON: an HTML error page, say
		}
		return "";
	}

	private static JsonObject parseObject(String body, Log log) {
		try {
			JsonElement json = JsonParser.parseString(body);
			if (json.isJsonObject()) return json.getAsJsonObject();
			log.line("  unexpected answer: not a JSON object");
		} catch (RuntimeException e) {
			log.line("  couldn't read the answer as JSON: " + e.getMessage());
		}
		return null;
	}

	private static JsonArray parseArray(String body, Log log) {
		try {
			JsonElement json = JsonParser.parseString(body);
			if (json.isJsonArray()) return json.getAsJsonArray();
			log.line("  unexpected answer: not a JSON list");
		} catch (RuntimeException e) {
			log.line("  couldn't read the answer as JSON: " + e.getMessage());
		}
		return null;
	}

	private static String string(JsonObject json, String key) {
		JsonElement value = json.get(key);
		return value == null || value.isJsonNull() || !value.isJsonPrimitive() ? "" : value.getAsString();
	}

	/** Leaves out NetEase's credit lines ("作词 : ..."), which are stamped like lyrics. */
	static String stripCredits(String lrc) {
		if (lrc == null) return "";
		StringBuilder out = new StringBuilder();
		for (String line : lrc.split("\\R")) {
			String text = STAMP.matcher(line).replaceAll("");
			if (CREDIT.matcher(text).find()) continue;
			out.append(line).append('\n');
		}
		return out.toString();
	}

	/** Leaves out "[Chorus]" style section names some plain lyrics have. */
	static String stripSections(String text) {
		StringBuilder out = new StringBuilder();
		for (String line : text.split("\\R")) {
			String trimmed = line.trim();
			if (trimmed.startsWith("[") && trimmed.endsWith("]")) continue;
			out.append(line).append('\n');
		}
		return out.toString().trim();
	}

	private static String enc(String text) {
		return URLEncoder.encode(text, StandardCharsets.UTF_8);
	}

	private static String encPath(String text) {
		return URLEncoder.encode(text, StandardCharsets.UTF_8).replace("+", "%20");
	}
}
