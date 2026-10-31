package dev.eventide.client.music;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns what a music app reports ("Under Pressure - Remastered 2011" by "Queen, David Bowie", or a
 * YouTube tab's "The Weeknd - Blinding Lights (Official Video)" by "TheWeekndVEVO") into the searches a
 * lyrics database understands, and scores how well a found song matches. Plain Java, so it can be unit
 * tested without the game.
 */
public final class LyricsQuery {
	/** One search to try: a title and an artist, and why it's worth trying (for the debug log). */
	public record Variant(String title, String artist, String why) {
	}

	/** Words in brackets or after a dash that describe a release, not the song. */
	private static final String RELEASE_WORDS = "remaster(?:ed)?|re-?master|live|radio edit|single edit|edit|version|ver\\.|mono|stereo"
			+ "|official|video|audio|lyrics?|lyric video|visuali[sz]er|hd|hq|4k|explicit|clean|bonus|deluxe|anniversary|demo|mix|"
			+ "sped up|speed up|slowed|reverb|nightcore|extended|acoustic|unplugged|session|from |taken from|soundtrack|ost|"
			+ "prod\\.?|produced by|music video|mv|m/v|performance|topic|bass boosted|8d|instrumental version|karaoke|"
			+ "\\d{4}";
	private static final Pattern FEAT_BRACKET = Pattern.compile("\\s*[(\\[（【]\\s*(?:feat\\.?|ft\\.?|featuring|with)\\s[^)\\]）】]*[)\\]）】]",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern FEAT_TAIL = Pattern.compile("\\s+(?:feat\\.?|ft\\.?|featuring)\\s.*$", Pattern.CASE_INSENSITIVE);
	private static final Pattern RELEASE_BRACKET = Pattern.compile("\\s*[(\\[（【]([^)\\]）】]*)[)\\]）】]");
	private static final Pattern RELEASE_WORD = Pattern.compile("(?:^|\\b|\\s)(?:" + RELEASE_WORDS + ")(?:\\b|\\s|$)", Pattern.CASE_INSENSITIVE);
	private static final Pattern DASH_TAIL = Pattern.compile("\\s+[-–—]\\s+(.+)$");
	private static final Pattern PIPE_TAIL = Pattern.compile("\\s*[|｜].*$");
	private static final Pattern ARTIST_SPLIT = Pattern.compile("\\s*(?:,|;|/|、|\\s&\\s|\\sx\\s|\\sX\\s|\\s×\\s|\\sand\\s|\\sfeat\\.?\\s|\\sft\\.?\\s|"
			+ "\\sfeaturing\\s|\\swith\\s|\\svs\\.?\\s)\\s*", Pattern.CASE_INSENSITIVE);
	private static final Pattern CHANNEL_SUFFIX = Pattern.compile("(?:\\s*-\\s*topic|vevo|\\s+official(?:\\s+channel)?|\\s+music)$",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern NON_KEY = Pattern.compile("[^\\p{L}\\p{N}]+");
	private static final Pattern MARKS = Pattern.compile("\\p{M}+");

	private LyricsQuery() {
	}

	/** Curly quotes, odd dashes and spacing made plain, so titles compare and search alike. */
	public static String tidy(String text) {
		if (text == null) return "";
		String out = Normalizer.normalize(text, Normalizer.Form.NFKC)
				.replace('\u2019', '\'').replace('\u2018', '\'').replace('\u201C', '"').replace('\u201D', '"')
				.replace('\u2010', '-').replace('\u2011', '-').replace('\u00A0', ' ');
		return out.replaceAll("\\s+", " ").trim();
	}

	/**
	 * The song's own title, without what describes the release: featured artists, "Remastered 2011",
	 * "Radio Edit", "(Live)", "(Official Video)", "| Lyrics" and the like. Never returns an empty title.
	 */
	public static String cleanTitle(String title) {
		String original = tidy(title);
		String out = PIPE_TAIL.matcher(original).replaceAll("");
		out = FEAT_BRACKET.matcher(out).replaceAll("");
		Matcher bracket = RELEASE_BRACKET.matcher(out);
		StringBuilder kept = new StringBuilder();
		int last = 0;
		while (bracket.find()) {
			kept.append(out, last, bracket.start());
			if (!RELEASE_WORD.matcher(bracket.group(1)).find()) kept.append(out, bracket.start(), bracket.end());
			last = bracket.end();
		}
		kept.append(out.substring(last));
		out = kept.toString().trim();
		Matcher dash = DASH_TAIL.matcher(out);
		if (dash.find() && RELEASE_WORD.matcher(dash.group(1)).find()) out = out.substring(0, dash.start()).trim();
		out = FEAT_TAIL.matcher(out).replaceAll("").trim();
		out = out.replaceAll("^[\"'«]+|[\"'»]+$", "").trim();
		return out.isEmpty() ? original : out;
	}

	/** The first of several artists ("Queen, David Bowie" gives "Queen"). */
	public static String firstArtist(String artist) {
		String clean = tidy(artist);
		String[] parts = ARTIST_SPLIT.split(clean, 2);
		return parts.length == 0 || parts[0].isBlank() ? clean : parts[0].trim();
	}

	/** A YouTube channel's name as the artist's ("TheWeekndVEVO" gives "TheWeeknd", "Adele - Topic" gives "Adele"). */
	public static String channelArtist(String artist) {
		String out = tidy(artist);
		String before;
		do {
			before = out;
			out = CHANNEL_SUFFIX.matcher(out).replaceAll("").trim();
		} while (!out.equals(before) && !out.isEmpty());
		return out.isEmpty() ? tidy(artist) : out;
	}

	/**
	 * For a video's title like "Artist - Song (Official Video)": the artist and song in it, or null when
	 * the title doesn't look like that. Browsers report the channel as the artist, so the title's own
	 * artist is the better guess.
	 */
	public static String[] splitVideoTitle(String title, String artist) {
		String clean = PIPE_TAIL.matcher(tidy(title)).replaceAll("");
		Matcher dash = Pattern.compile("^(.+?)\\s+[-–—]\\s+(.+)$").matcher(clean);
		if (!dash.find()) return null;
		String left = dash.group(1).trim();
		String right = dash.group(2).trim();
		if (RELEASE_WORD.matcher(right).matches()) return null;
		String channel = key(channelArtist(artist)).replace(" ", "");
		String leftKey = key(left).replace(" ", "");
		boolean channelLike = artist == null || artist.isBlank() || CHANNEL_SUFFIX.matcher(tidy(artist)).find()
				|| !channel.isEmpty() && (leftKey.contains(channel) || channel.contains(leftKey));
		if (!channelLike) return null;
		return new String[] {left, right};
	}

	/**
	 * A key for comparing names: lower case, accents and punctuation gone, "&" read as "and". Letters of
	 * every script are kept, so Japanese or Cyrillic titles compare too.
	 */
	public static String key(String text) {
		String out = Normalizer.normalize(tidy(text), Normalizer.Form.NFKD);
		out = MARKS.matcher(out).replaceAll("");
		out = out.toLowerCase(Locale.ROOT).replace("&", " and ");
		out = NON_KEY.matcher(out).replaceAll(" ").trim();
		return out;
	}

	/** How alike two names are, 0..1: the better of letter-by-letter and word-by-word likeness of their keys. */
	public static double similarity(String a, String b) {
		String ka = key(a);
		String kb = key(b);
		if (ka.isEmpty() || kb.isEmpty()) return 0;
		if (ka.equals(kb)) return 1;
		String sa = ka.replace(" ", "");
		String sb = kb.replace(" ", "");
		if (sa.equals(sb)) return 0.98;
		int distance = levenshtein(sa, sb);
		double letters = 1 - distance / (double) Math.max(sa.length(), sb.length());
		Set<String> wa = new LinkedHashSet<>(List.of(ka.split(" ")));
		Set<String> wb = new LinkedHashSet<>(List.of(kb.split(" ")));
		int shared = 0;
		for (String word : wa) if (wb.contains(word)) shared++;
		double words = shared / (double) Math.max(wa.size(), wb.size());
		boolean contains = sa.contains(sb) || sb.contains(sa);
		double inside = contains ? Math.min(sa.length(), sb.length()) / (double) Math.max(sa.length(), sb.length()) * 0.4 + 0.55 : 0;
		return Math.max(letters, Math.max(words, inside));
	}

	/** How well an artist matches: full name, any one of several artists, or a channel name. */
	public static double artistSimilarity(String wanted, String found) {
		double best = similarity(wanted, found);
		for (String part : ARTIST_SPLIT.split(tidy(wanted))) best = Math.max(best, similarity(part, found));
		for (String part : ARTIST_SPLIT.split(tidy(found))) best = Math.max(best, similarity(wanted, part) * 0.95);
		best = Math.max(best, similarity(channelArtist(wanted), found) * 0.95);
		return best;
	}

	/**
	 * How well a found song matches the one playing, 0..1: mostly the title, then the artist, then how
	 * close the lengths are. A length more than 20 seconds off is a different version and scores 0.
	 */
	public static double score(String title, String artist, double duration, String foundTitle, String foundArtist, double foundDuration) {
		double titleScore = Math.max(similarity(cleanTitle(title), cleanTitle(foundTitle)), similarity(title, foundTitle));
		double artistScore = artist == null || artist.isBlank() ? 0.6 : artistSimilarity(artist, foundArtist);
		double durationScore = 0.6;
		if (duration > 1 && foundDuration > 1) {
			double off = Math.abs(duration - foundDuration);
			if (off > 20) return 0;
			durationScore = off <= 2 ? 1 : Math.max(0, 1 - (off - 2) / 13);
		}
		return 0.55 * titleScore + 0.3 * artistScore + 0.15 * durationScore;
	}

	/**
	 * The searches to try, best first: as reported, then cleaned up, then with only the first artist, and
	 * for videos the artist and song read from the title. Duplicates are left out.
	 */
	public static List<Variant> variants(String title, String artist) {
		List<Variant> out = new ArrayList<>();
		Set<String> seen = new LinkedHashSet<>();
		String rawTitle = tidy(title);
		String rawArtist = tidy(artist);
		String clean = cleanTitle(rawTitle);
		String first = firstArtist(rawArtist);
		add(out, seen, rawTitle, rawArtist, "as reported");
		add(out, seen, clean, rawArtist, "cleaned title");
		add(out, seen, clean, first, "cleaned title, first artist");
		String[] video = splitVideoTitle(rawTitle, rawArtist);
		if (video != null) {
			add(out, seen, cleanTitle(video[1]), firstArtist(video[0]), "artist and song from a video title");
			add(out, seen, cleanTitle(video[1]), video[0], "song from a video title, full artist");
		}
		String channel = channelArtist(rawArtist);
		if (!channel.equals(rawArtist)) add(out, seen, clean, channel, "channel name as the artist");
		return out;
	}

	private static void add(List<Variant> out, Set<String> seen, String title, String artist, String why) {
		if (title.isBlank()) return;
		String id = key(title) + "\u0000" + key(artist);
		if (seen.add(id)) out.add(new Variant(title, artist, why));
	}

	/** The key a song's lyrics are cached under: first artist and cleaned title, as compared. */
	public static String cacheKey(String title, String artist) {
		String[] video = splitVideoTitle(title, artist);
		String t = video != null ? cleanTitle(video[1]) : cleanTitle(title);
		String a = video != null ? firstArtist(video[0]) : firstArtist(artist);
		return key(a) + "|" + key(t);
	}

	private static int levenshtein(String a, String b) {
		int[] previous = new int[b.length() + 1];
		int[] current = new int[b.length() + 1];
		for (int j = 0; j <= b.length(); j++) previous[j] = j;
		for (int i = 1; i <= a.length(); i++) {
			current[0] = i;
			for (int j = 1; j <= b.length(); j++) {
				int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
				current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), previous[j - 1] + cost);
			}
			int[] swap = previous;
			previous = current;
			current = swap;
		}
		return previous[b.length()];
	}
}
