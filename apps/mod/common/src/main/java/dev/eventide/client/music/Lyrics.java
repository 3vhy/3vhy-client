package dev.eventide.client.music;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.eventide.client.EventideClient;
import dev.eventide.client.module.Modules;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lyrics for the song playing, found by {@link LyricsSearch} on one background thread and kept per
 * song: a result always belongs to the song it was searched for, so switching songs mid-search never
 * loses or misplaces one. Found lyrics are saved in Eventide/lyrics, so a song played again shows them at
 * once, even offline. A song without lyrics is searched again later (sooner when a server was busy or
 * unreachable), never given up on for good. Plain lyrics without timings are spread over the song and
 * marked as not synced.
 */
public final class Lyrics {
	/** One line and when it starts, in seconds. A blank text is an instrumental break. */
	public record Line(double at, String text) {
	}

	/** Where the lyrics for a song stand. */
	public enum State {
		NONE, SEARCHING, SYNCED, UNSYNCED, INSTRUMENTAL, NOT_FOUND, UNREACHABLE
	}

	/** What the lyrics widgets need for a song: its lines, whether they're synced, and its state. */
	public record View(List<Line> lines, boolean synced, State state) {
		static final View EMPTY = new View(List.of(), true, State.NONE);
	}

	private static final class Entry {
		final String key;
		volatile State state = State.SEARCHING;
		volatile List<Line> lines = List.of();
		volatile boolean synced = true;
		volatile String provider = "";
		volatile String reason = "";
		volatile long retryAt = 0;
		volatile int failures = 0;

		Entry(String key) {
			this.key = key;
		}

		View view() {
			return new View(lines, synced, state);
		}
	}

	private static final Pattern STAMP = Pattern.compile("\\[(\\d+):(\\d+(?:[.:]\\d+)?)]");
	private static final Pattern OFFSET = Pattern.compile("\\[offset:\\s*([+-]?\\d+)]", Pattern.CASE_INSENSITIVE);
	private static final long[] NOT_FOUND_RETRY_MS = {120_000, 600_000, 1_800_000, 3_600_000};
	private static final long[] UNREACHABLE_RETRY_MS = {5_000, 20_000, 60_000, 180_000, 600_000};
	private static final Gson GSON = new Gson();
	private static final Map<String, Entry> ENTRIES = new ConcurrentHashMap<>();
	private static final AtomicBoolean BUSY = new AtomicBoolean(false);
	private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(runnable -> {
		Thread thread = new Thread(runnable, "Eventide lyrics");
		thread.setDaemon(true);
		return thread;
	});
	private static LyricsSearch search;

	private static volatile NowPlaying wanted = null;
	private static volatile String wantedKey = null;
	private static String keyTitle = null;
	private static String keyArtist = null;
	private static String keyMemo = null;

	private Lyrics() {
	}

	/** The key a song's lyrics are kept under, remembered for the last song so it isn't worked out every frame. */
	private static String keyOf(NowPlaying song) {
		if (song.title().equals(keyTitle) && song.artist().equals(keyArtist)) return keyMemo;
		keyTitle = song.title();
		keyArtist = song.artist();
		keyMemo = LyricsQuery.cacheKey(song.title(), song.artist());
		return keyMemo;
	}

	/**
	 * Makes sure lyrics for this song are found or on their way: the first time it's seen, when a
	 * failed search is due again, or after the player asked to reload. Cheap; call it every frame.
	 */
	public static void want(NowPlaying song) {
		if (song == null || song.title().isBlank()) return;
		String key = keyOf(song);
		wanted = song;
		wantedKey = key;
		Entry entry = ENTRIES.get(key);
		if (entry == null) {
			entry = new Entry(key);
			ENTRIES.put(key, entry);
		} else if ((entry.state == State.NOT_FOUND || entry.state == State.UNREACHABLE) && System.currentTimeMillis() >= entry.retryAt) {
			entry.state = State.SEARCHING;
		}
		if (entry.state == State.SEARCHING) schedule();
	}

	/** Searches again for the song playing now, forgetting what was found or saved for it. */
	public static void reload(NowPlaying song) {
		if (song == null || song.title().isBlank()) return;
		String key = keyOf(song);
		ENTRIES.remove(key);
		WORKER.execute(() -> {
			try {
				Files.deleteIfExists(cacheFile(key));
			} catch (IOException e) {
				EventideClient.LOG.warn("Couldn't remove the saved lyrics for {}", song.title(), e);
			}
		});
		want(song);
	}

	/** Starts the next search if the worker is free and the song playing still needs one. */
	private static void schedule() {
		if (!BUSY.compareAndSet(false, true)) return;
		NowPlaying song = wanted;
		String key = wantedKey;
		Entry entry = key == null ? null : ENTRIES.get(key);
		if (song == null || entry == null || entry.state != State.SEARCHING) {
			BUSY.set(false);
			return;
		}
		WORKER.execute(() -> {
			try {
				run(song, entry);
			} catch (RuntimeException e) {
				entry.reason = "unexpected problem: " + e;
				fail(entry, true);
				EventideClient.LOG.warn("Lyrics search for \"{}\" failed", song.title(), e);
			} finally {
				BUSY.set(false);
				schedule();
			}
		});
	}

	private static void run(NowPlaying song, Entry entry) {
		boolean debug = debug();
		LyricsSearch.Found saved = readCache(entry.key, song.duration());
		if (saved != null) {
			apply(entry, saved, song.duration());
			if (debug) log("Lyrics for \"" + song.title() + "\" from the saved copy (" + saved.provider() + ")");
			return;
		}
		if (search == null) search = new LyricsSearch("Eventide " + EventideClient.version() + " (https://github.com/3vhy/Eventide)");
		LyricsSearch.Outcome outcome = search.find(song.title(), song.artist(), song.duration(), debug ? Lyrics::log : line -> {
		});
		if (outcome.found() != null) {
			apply(entry, outcome.found(), song.duration());
			writeCache(entry.key, song, outcome.found());
			EventideClient.LOG.info("Lyrics for \"{}\" by {}: {} from {}", song.title(), song.artist(),
					outcome.found().kind().name().toLowerCase(Locale.ROOT), outcome.found().provider());
			return;
		}
		entry.reason = outcome.reason();
		fail(entry, outcome.retrySoon());
		EventideClient.LOG.info("No lyrics for \"{}\" by {}: {} (trying again in {}s)", song.title(), song.artist(), outcome.reason(),
				(entry.retryAt - System.currentTimeMillis()) / 1000);
	}

	private static void fail(Entry entry, boolean soon) {
		long[] waits = soon ? UNREACHABLE_RETRY_MS : NOT_FOUND_RETRY_MS;
		entry.retryAt = System.currentTimeMillis() + waits[Math.min(entry.failures, waits.length - 1)];
		entry.failures++;
		entry.state = soon ? State.UNREACHABLE : State.NOT_FOUND;
	}

	private static void apply(Entry entry, LyricsSearch.Found found, double duration) {
		entry.provider = found.provider();
		entry.reason = "";
		entry.failures = 0;
		switch (found.kind()) {
			case SYNCED -> {
				entry.lines = parse(found.lrc());
				entry.synced = true;
				entry.state = entry.lines.isEmpty() ? State.NOT_FOUND : State.SYNCED;
			}
			case PLAIN -> {
				entry.lines = spread(found.plain(), duration > 1 ? duration : found.duration());
				entry.synced = false;
				entry.state = entry.lines.isEmpty() ? State.NOT_FOUND : State.UNSYNCED;
			}
			case INSTRUMENTAL -> {
				entry.lines = List.of();
				entry.state = State.INSTRUMENTAL;
			}
			default -> {
				entry.lines = List.of();
				entry.state = State.NOT_FOUND;
			}
		}
	}

	/**
	 * Gives plain lyrics timings by spreading the lines over the song: after a short intro and before
	 * the last few seconds, each line getting time by its length, with a pause between verses.
	 */
	static List<Line> spread(String plain, double duration) {
		if (plain == null || plain.isBlank()) return List.of();
		double length = duration > 30 ? duration : 210;
		List<String> rows = new ArrayList<>();
		for (String row : plain.split("\\R")) rows.add(row.trim());
		while (!rows.isEmpty() && rows.get(0).isEmpty()) rows.remove(0);
		while (!rows.isEmpty() && rows.get(rows.size() - 1).isEmpty()) rows.remove(rows.size() - 1);
		double total = 0;
		for (String row : rows) total += row.isEmpty() ? 4 : 6 + row.length();
		if (total <= 0) return List.of();
		double start = Math.max(4, Math.min(18, length * 0.08));
		double end = length * 0.93;
		double perUnit = (end - start) / total;
		List<Line> lines = new ArrayList<>();
		double at = start;
		boolean lastBlank = false;
		for (String row : rows) {
			if (row.isEmpty()) {
				if (!lastBlank) lines.add(new Line(at, ""));
				lastBlank = true;
				at += 4 * perUnit;
				continue;
			}
			lastBlank = false;
			lines.add(new Line(at, row));
			at += (6 + row.length()) * perUnit;
		}
		return Collections.unmodifiableList(lines);
	}

	/** Reads LRC text: lines like {@code [01:23.45] words}, several stamps a line allowed, and an {@code [offset:ms]} tag. */
	public static List<Line> parse(String lrc) {
		if (lrc == null) return List.of();
		double offset = 0;
		Matcher offsetTag = OFFSET.matcher(lrc);
		if (offsetTag.find()) offset = Integer.parseInt(offsetTag.group(1)) / 1000.0;
		List<Line> parsed = new ArrayList<>();
		for (String raw : lrc.split("\\R")) {
			Matcher matcher = STAMP.matcher(raw);
			List<Double> times = new ArrayList<>();
			int end = 0;
			while (matcher.find() && matcher.start() == end) {
				String seconds = matcher.group(2).replace(':', '.');
				times.add(Integer.parseInt(matcher.group(1)) * 60 + Double.parseDouble(seconds));
				end = matcher.end();
			}
			String text = raw.substring(end).trim();
			for (double time : times) parsed.add(new Line(Math.max(0, time - offset), text));
		}
		parsed.sort((a, b) -> Double.compare(a.at(), b.at()));
		boolean anyWords = false;
		for (Line line : parsed) anyWords |= !line.text().isBlank();
		return anyWords ? Collections.unmodifiableList(parsed) : List.of();
	}

	/** For the in-game test: these synced lyrics for the song playing now. */
	public static void simulate(NowPlaying song, String lrc) {
		if (song == null) return;
		Entry entry = new Entry(keyOf(song));
		entry.lines = parse(lrc);
		entry.state = State.SYNCED;
		entry.provider = "test";
		ENTRIES.put(entry.key, entry);
	}

	/** For the in-game test: these plain (unsynced) lyrics for the song playing now. */
	public static void simulatePlain(NowPlaying song, String plain) {
		if (song == null) return;
		Entry entry = new Entry(keyOf(song));
		entry.lines = spread(plain, song.duration());
		entry.synced = false;
		entry.state = State.UNSYNCED;
		entry.provider = "test";
		ENTRIES.put(entry.key, entry);
	}

	/** The lyrics for {@code song} as they stand: none yet while they're being looked for. */
	public static View view(NowPlaying song) {
		if (song == null || song.title().isBlank()) return View.EMPTY;
		Entry entry = ENTRIES.get(keyOf(song));
		return entry == null ? View.EMPTY : entry.view();
	}

	/** The lines for {@code song}, or none if they haven't come yet. */
	public static List<Line> lines(NowPlaying song) {
		return view(song).lines();
	}

	/** The index of the line being sung at {@code seconds}, or -1 before the first. */
	public static int index(List<Line> lines, double seconds) {
		int lo = 0;
		int hi = lines.size() - 1;
		int found = -1;
		while (lo <= hi) {
			int mid = (lo + hi) >>> 1;
			if (lines.get(mid).at() <= seconds) {
				found = mid;
				lo = mid + 1;
			} else {
				hi = mid - 1;
			}
		}
		return found;
	}

	/** A short line for the settings page about the song playing: synced, not synced, searching, or why there are none. */
	public static String status() {
		NowPlaying song = MusicBridge.current();
		if (song == null || song.title().isBlank()) return MusicBridge.supported() ? "Waiting for a song" : "Needs Windows";
		Entry entry = ENTRIES.get(keyOf(song));
		if (entry == null) return "Searching…";
		return switch (entry.state) {
			case SEARCHING -> "Searching…";
			case SYNCED -> "Synced · " + entry.provider;
			case UNSYNCED -> "Not synced · " + entry.provider;
			case INSTRUMENTAL -> "Instrumental";
			case NOT_FOUND -> "Not found";
			case UNREACHABLE -> "Can't reach the lyrics servers";
			case NONE -> "";
		};
	}

	/** Why the song playing has no lyrics, with when it'll be looked for again; empty when it has them. */
	public static String detail() {
		NowPlaying song = MusicBridge.current();
		if (song == null || song.title().isBlank()) return "";
		Entry entry = ENTRIES.get(keyOf(song));
		if (entry == null || (entry.state != State.NOT_FOUND && entry.state != State.UNREACHABLE)) return "";
		long seconds = Math.max(0, (entry.retryAt - System.currentTimeMillis()) / 1000);
		String again = seconds >= 120 ? (seconds / 60) + " min" : seconds + "s";
		return (entry.reason.isEmpty() ? "No lyrics found" : capitalize(entry.reason)) + ". Trying again in " + again;
	}

	/** The state for the song playing, for widgets that only need that. */
	public static State state() {
		return view(MusicBridge.current()).state();
	}

	private static String capitalize(String text) {
		return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
	}

	private static boolean debug() {
		return Modules.LYRICS_DEBUG.get();
	}

	private static void log(String line) {
		EventideClient.LOG.info("[lyrics] {}", line);
	}

	private static Path cacheFile(String key) {
		String name;
		try {
			name = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(key.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			name = Integer.toHexString(key.hashCode());
		}
		return dev.eventide.client.Migration.dataDir().resolve("lyrics").resolve(name + ".json");
	}

	/** The saved lyrics for this song, if they were saved for a version of about this length. */
	private static LyricsSearch.Found readCache(String key, double duration) {
		Path file = cacheFile(key);
		if (!Files.isRegularFile(file)) return null;
		try {
			JsonObject json = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), JsonObject.class);
			if (json == null || !key.equals(string(json, "key"))) return null;
			double savedDuration = json.has("duration") ? json.get("duration").getAsDouble() : 0;
			if (duration > 1 && savedDuration > 1 && Math.abs(duration - savedDuration) > 12) return null;
			LyricsSearch.Kind kind = LyricsSearch.Kind.valueOf(string(json, "kind"));
			return new LyricsSearch.Found(kind, string(json, "lrc"), string(json, "plain"), string(json, "provider"),
					json.has("score") ? json.get("score").getAsDouble() : 1, string(json, "title"), string(json, "artist"), savedDuration);
		} catch (IOException | RuntimeException e) {
			EventideClient.LOG.warn("Couldn't read saved lyrics {}, searching again", file.getFileName(), e);
			return null;
		}
	}

	private static void writeCache(String key, NowPlaying song, LyricsSearch.Found found) {
		if (found.kind() == LyricsSearch.Kind.NONE) return;
		JsonObject json = new JsonObject();
		json.addProperty("key", key);
		json.addProperty("title", song.title());
		json.addProperty("artist", song.artist());
		json.addProperty("duration", song.duration() > 1 ? song.duration() : found.duration());
		json.addProperty("kind", found.kind().name());
		json.addProperty("provider", found.provider());
		json.addProperty("score", found.score());
		json.addProperty("lrc", found.lrc());
		json.addProperty("plain", found.plain());
		json.addProperty("saved", System.currentTimeMillis());
		Path file = cacheFile(key);
		try {
			Files.createDirectories(file.getParent());
			Files.writeString(file, GSON.toJson(json), StandardCharsets.UTF_8);
		} catch (IOException e) {
			EventideClient.LOG.warn("Couldn't save the lyrics for {}", song.title(), e);
		}
	}

	private static String string(JsonObject json, String key) {
		return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : "";
	}
}
