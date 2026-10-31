package dev.eventide.client.music;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Title clean-up and matching, on awkward titles as Spotify, Apple Music and browser tabs report them. */
class LyricsQueryTest {
	@Test
	void remastersAndEditsComeOff() {
		assertEquals("Under Pressure", LyricsQuery.cleanTitle("Under Pressure - Remastered 2011"));
		assertEquals("Song 2", LyricsQuery.cleanTitle("Song 2 - 2012 Remaster"));
		assertEquals("Hey Jude", LyricsQuery.cleanTitle("Hey Jude - Remastered 2015"));
		assertEquals("Blinding Lights", LyricsQuery.cleanTitle("Blinding Lights - Radio Edit"));
		assertEquals("Africa", LyricsQuery.cleanTitle("Africa - Live at Budokan"));
		assertEquals("Wonderwall", LyricsQuery.cleanTitle("Wonderwall (Live)"));
		assertEquals("1979", LyricsQuery.cleanTitle("1979 - Remastered 2012"));
	}

	@Test
	void featuredArtistsComeOff() {
		assertEquals("Somebody That I Used To Know", LyricsQuery.cleanTitle("Somebody That I Used To Know (feat. Kimbra)"));
		assertEquals("Señorita", LyricsQuery.cleanTitle("Señorita (with Camila Cabello)"));
		assertEquals("Rockabye", LyricsQuery.cleanTitle("Rockabye (feat. Sean Paul & Anne-Marie) [Official Video]"));
		assertEquals("Stay", LyricsQuery.cleanTitle("Stay ft. Justin Bieber"));
		assertEquals("Lose Yourself", LyricsQuery.cleanTitle("Lose Yourself - From \"8 Mile\" Soundtrack"));
	}

	@Test
	void theSongsOwnWordsStay() {
		assertEquals("(I Can't Get No) Satisfaction", LyricsQuery.cleanTitle("(I Can't Get No) Satisfaction - Mono Version"));
		assertEquals("HUMBLE.", LyricsQuery.cleanTitle("HUMBLE."));
		assertEquals("Despacito - Remix", LyricsQuery.cleanTitle("Despacito - Remix"));
		assertEquals("夜に駆ける", LyricsQuery.cleanTitle("夜に駆ける"));
		assertEquals("Gangnam Style (강남스타일)", LyricsQuery.cleanTitle("Gangnam Style (강남스타일)"));
		assertEquals("Mr. Brightside", LyricsQuery.cleanTitle("Mr. Brightside"));
		assertEquals("Don't Stop Me Now", LyricsQuery.cleanTitle("Don’t Stop Me Now - Remastered 2011"));
	}

	@Test
	void videoTitlesGiveTheArtistAndSong() {
		assertArrayEquals(new String[] {"The Weeknd", "Blinding Lights (Official Video)"},
				LyricsQuery.splitVideoTitle("The Weeknd - Blinding Lights (Official Video)", "TheWeekndVEVO"));
		assertArrayEquals(new String[] {"Rick Astley", "Never Gonna Give You Up (Official Music Video)"},
				LyricsQuery.splitVideoTitle("Rick Astley - Never Gonna Give You Up (Official Music Video)", "Rick Astley"));
		assertArrayEquals(new String[] {"Adele", "Hello"}, LyricsQuery.splitVideoTitle("Adele - Hello | Lyrics", "Adele - Topic"));
		assertNull(LyricsQuery.splitVideoTitle("Under Pressure - Remastered 2011", "Queen, David Bowie"));
		assertNull(LyricsQuery.splitVideoTitle("Mr. Brightside", "The Killers"));
		assertEquals("blinding lights", LyricsQuery.key(LyricsQuery.cleanTitle(
				LyricsQuery.splitVideoTitle("The Weeknd - Blinding Lights (Official Video)", "TheWeekndVEVO")[1])));
	}

	@Test
	void artistsSplitAndChannelsClean() {
		assertEquals("Queen", LyricsQuery.firstArtist("Queen, David Bowie"));
		assertEquals("Calvin Harris", LyricsQuery.firstArtist("Calvin Harris feat. Rihanna"));
		assertEquals("Marshmello", LyricsQuery.firstArtist("Marshmello x Bastille"));
		assertEquals("Lil Nas X", LyricsQuery.firstArtist("Lil Nas X"));
		assertEquals("Luis Fonsi", LyricsQuery.firstArtist("Luis Fonsi; Daddy Yankee"));
		assertEquals("TheWeeknd", LyricsQuery.channelArtist("TheWeekndVEVO"));
		assertEquals("Adele", LyricsQuery.channelArtist("Adele - Topic"));
	}

	@Test
	void keysIgnoreCaseAccentsAndPunctuation() {
		assertEquals("beyonce", LyricsQuery.key("Beyoncé"));
		assertEquals("rock and roll", LyricsQuery.key("Rock & Roll!"));
		assertEquals(LyricsQuery.key("Don't Stop Me Now"), LyricsQuery.key("Don’t  Stop Me Now"));
		assertEquals("夜に駆ける", LyricsQuery.key("夜に駆ける"));
		assertEquals("кино", LyricsQuery.key("КИНО"));
	}

	@Test
	void scoresPreferTheSameSongAndLength() {
		double same = LyricsQuery.score("Under Pressure - Remastered 2011", "Queen, David Bowie", 248, "Under Pressure", "Queen & David Bowie", 247);
		double otherLength = LyricsQuery.score("Under Pressure", "Queen", 248, "Under Pressure", "Queen", 280);
		double otherSong = LyricsQuery.score("Under Pressure", "Queen", 248, "Bohemian Rhapsody", "Queen", 248);
		assertTrue(same > 0.85, "same song scored " + same);
		assertEquals(0, otherLength);
		assertTrue(otherSong < 0.72, "another song scored " + otherSong);
		assertTrue(LyricsQuery.score("夜に駆ける", "YOASOBI", 261, "夜に駆ける", "YOASOBI", 261) > 0.95);
	}

	@Test
	void variantsGoFromAsReportedToCleaned() {
		List<LyricsQuery.Variant> variants = LyricsQuery.variants("Somebody That I Used To Know (feat. Kimbra)", "Gotye, Kimbra");
		assertEquals("Somebody That I Used To Know (feat. Kimbra)", variants.get(0).title());
		assertTrue(variants.stream().anyMatch(v -> v.title().equals("Somebody That I Used To Know") && v.artist().equals("Gotye")));
		List<LyricsQuery.Variant> video = LyricsQuery.variants("The Weeknd - Blinding Lights (Official Video)", "TheWeekndVEVO");
		assertTrue(video.stream().anyMatch(v -> v.title().equals("Blinding Lights") && v.artist().equals("The Weeknd")));
		assertEquals(LyricsQuery.cacheKey("Blinding Lights", "The Weeknd"),
				LyricsQuery.cacheKey("The Weeknd - Blinding Lights (Official Video)", "TheWeekndVEVO"));
	}

	@Test
	void lrcParsesOffsetsAndRepeatedStamps() {
		List<Lyrics.Line> lines = Lyrics.parse("[ti:Test]\n[offset:+500]\n[00:01.50] a\n[00:03.00][00:09.00] b\n[00:05:20]\n");
		assertEquals(4, lines.size());
		assertEquals(1.0, lines.get(0).at(), 1e-9);
		assertEquals("", lines.get(2).text());
		assertEquals(8.5, lines.get(3).at(), 1e-9);
		assertTrue(Lyrics.parse("[00:01.00]\n[00:02.00]\n").isEmpty(), "stamps without words are no lyrics");
	}

	@Test
	void plainLyricsSpreadOverTheSong() {
		List<Lyrics.Line> lines = Lyrics.spread("First line\nSecond line\n\nThird line after a break\n", 200);
		assertEquals(4, lines.size());
		assertTrue(lines.get(0).at() >= 4 && lines.get(0).at() <= 18);
		for (int i = 1; i < lines.size(); i++) assertTrue(lines.get(i).at() > lines.get(i - 1).at());
		assertTrue(lines.get(lines.size() - 1).at() < 200);
		assertEquals("", lines.get(2).text());
	}

	@Test
	void creditLinesLeaveNetEaseLyrics() {
		String lrc = LyricsSearch.stripCredits("[00:00.00] 作词 : Someone\n[00:00.50] 作曲 : Someone\n[00:12.00] The first line\n");
		assertEquals(1, Lyrics.parse(lrc).size());
	}
}
