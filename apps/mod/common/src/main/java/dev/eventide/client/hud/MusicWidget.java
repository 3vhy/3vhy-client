package dev.eventide.client.hud;

import dev.eventide.client.Draw;
import dev.eventide.client.gui.Accent;
import dev.eventide.client.module.Modules;
import dev.eventide.client.music.MusicArt;
import dev.eventide.client.music.MusicBridge;
import dev.eventide.client.music.NowPlaying;
import dev.eventide.client.ui.Anim;
import dev.eventide.client.ui.Ease;
import dev.eventide.client.ui.Grad;
import dev.eventide.client.ui.Logo;
import dev.eventide.client.ui.Paint;
import dev.eventide.client.ui.Tokens;
import dev.eventide.client.ui.Type;
import dev.eventide.client.ui.Type.Weight;
import net.minecraft.client.Minecraft;

/**
 * The Music Player, laid out like the website's Spotify player: the line being sung above the card,
 * big and filling in like karaoke with the next line faint under it, then a glass card with the cover
 * (the app's icon on its corner), the title and artist, a thin violet progress bar with the times under
 * it, and back / a light play button / next. Every size is the website's, in CSS pixels scaled to the
 * HUD by {@link #S}.
 *
 * <p>It slides in when music starts and out when it stops, a new song's cover fades in as its title
 * slides up over the old one, the bar glides to where the song is, play and pause turn into each other,
 * and each lyric line rises in as the last one leaves.
 */
final class MusicWidget extends HudWidget {
	/** GUI pixels per CSS pixel of the website's player. */
	private static final float S = 0.45f;
	private static final int W = Math.round(440 * S);
	private static final float CARD_H = 99 * S;
	private static final float PAD_L = 12 * S;
	private static final float PAD_R = 16 * S;
	private static final float ART = 56 * S;
	private static final float GAP = 14 * S;
	private static final float CONTROLS = (17 + 10 + 34 + 10 + 17) * S;
	private static final float VOLUME_ROW = 14;
	private static final float LYRICS_H = (23 * 1.3f + 6 + 14 * 1.6f + 22) * S;
	private static final float LINE_CAP = 20 * 0.727f * S;
	private static final float NEXT_CAP = 14 * 0.727f * S;
	private static final long CHANGE_MS = 520;
	private static final Grad BAR = new Grad(0xFF8B3DFF, 0xFFEBA2FF, 90);
	private static final Grad ART_FILL = new Grad(0xFF2A1052, 0xFFD12CF0, 140);

	private final Anim appear = new Anim(0, 480, Ease.OUT);
	private final Anim playing = new Anim(0, 260, Ease.OUT);
	private final Anim lyricsShown = new Anim(0, 420, Ease.OUT);
	private final LyricTracker lyrics = new LyricTracker();
	private NowPlaying last = null;
	private String songKey = null;
	private String oldTitle = "";
	private String oldArtist = "";
	private long changedAt = 0;
	private long toggledAt = 0;
	private boolean wasPlaying = false;
	private float shownProgress = -1;
	private long lastFrame = 0;

	MusicWidget() {
		super("music", Modules.MUSIC, Anchor.TOP_CENTER);
	}

	private static boolean progress(NowPlaying song) {
		return Modules.MUSIC_PROGRESS.get() && (song == null || song.hasTimeline());
	}

	private static boolean volume(NowPlaying song) {
		return Modules.MUSIC_VOLUME.get() && song != null && song.hasVolume();
	}

	/** The bar's colours: the website's violet to lilac, or a gradient around the colour picked. */
	private static Grad fill() {
		int c = Modules.MUSIC_COLOR.get() | 0xFF000000;
		if (c == (Tokens.PRIMARY_MID | 0xFF000000)) return BAR;
		return new Grad(Accent.turn(c, -14), Accent.lighten(c, 0.35f), 90);
	}

	/** A sample for the layout editor, so the widget can be placed with no music playing. */
	private static NowPlaying sample() {
		String title = MusicBridge.supported() ? "Nothing playing" : "Music Player";
		String artist = MusicBridge.supported() ? "Play a song on Spotify, YouTube..." : "Works on Windows";
		return new NowPlaying("sample", "", title, artist, "", false, 42, 175, System.nanoTime(), null, -1, null, -1, "", 64, false);
	}

	/** Whether there's music to show right now (before the slide in or out). */
	private static boolean playingNow() {
		NowPlaying song = MusicBridge.current();
		return song != null && (song.playing() || Modules.MUSIC_PAUSED.get());
	}

	@Override
	public boolean visible(Minecraft mc) {
		return playingNow() || appear.get() > 0.01f;
	}

	@Override
	public int width(Minecraft mc) {
		return W;
	}

	private float cardHeight(NowPlaying song) {
		return CARD_H + (volume(song) ? VOLUME_ROW : 0);
	}

	@Override
	public int height(Minecraft mc) {
		NowPlaying song = MusicBridge.current();
		return (int) Math.ceil(LYRICS_H * lyricsShown.get() + cardHeight(song != null ? song : last != null ? last : sample()));
	}

	@Override
	public void draw(Draw d, int x, int y, Minecraft mc, boolean preview) {
		NowPlaying current = MusicBridge.current();
		appear.set(playingNow() || preview);
		if (preview) appear.snap(1);
		if (current != null) last = current;
		NowPlaying song = current != null ? current : preview ? sample() : last;
		if (song == null) return;
		long now = System.currentTimeMillis();
		float dt = lastFrame == 0 ? 0 : Math.min(0.1f, (now - lastFrame) / 1000f);
		lastFrame = now;
		follow(song, now);
		boolean withLyrics = false;
		if (Modules.MUSIC_LYRICS.get() && !preview && current != null) {
			lyrics.update(current);
			withLyrics = lyrics.any();
		}
		lyricsShown.set(withLyrics);

		float a = appear.get();
		float old = Paint.opacity(Paint.opacity() * a);
		float top = y - (1 - a) * 8;
		boolean shadow = textShadow();
		float shown = lyricsShown.get();
		if (shown > 0.01f) {
			float lyricOld = Paint.opacity(Paint.opacity() * shown);
			drawLyrics(d, current, x + W / 2f, top, shadow);
			Paint.opacity(lyricOld);
		}
		float cardTop = top + LYRICS_H * shown;
		drawCard(d, song, x, cardTop, now, dt, shadow);
		Paint.opacity(old);
	}

	/** Notices a new song (for the cross-fade) and play / pause (for the button's turn). */
	private void follow(NowPlaying song, long now) {
		String key = song.title() + "\u0000" + song.artist();
		if (!key.equals(songKey)) {
			if (songKey != null) {
				int split = songKey.indexOf('\u0000');
				oldTitle = songKey.substring(0, split);
				oldArtist = songKey.substring(split + 1);
				changedAt = now;
			} else {
				oldTitle = "";
				changedAt = now - CHANGE_MS;
			}
			songKey = key;
			shownProgress = -1;
		}
		if (song.playing() != wasPlaying) {
			toggledAt = now;
			wasPlaying = song.playing();
		}
		playing.set(song.playing());
	}

	/** The line being sung, big and centred, filling in as it's sung; the next line faint under it. */
	private void drawLyrics(Draw d, NowPlaying song, float cx, float top, boolean shadow) {
		float t = lyrics.change();
		float lineTop = top + (23 * 1.3f * S - LINE_CAP) / 2f;
		String previous = lyrics.previous();
		if (t < 1 && !previous.isBlank()) {
			float old = Paint.opacity(Paint.opacity() * (1 - t));
			String shownPrevious = Type.fit(previous, W, LINE_CAP, Weight.SEMIBOLD);
			glowText(d, shownPrevious, cx - Type.width(shownPrevious, LINE_CAP, Weight.SEMIBOLD) / 2f, lineTop - 4.5f * t, 1, shadow);
			Paint.opacity(old);
		}
		String line = lyrics.current().isBlank() ? "· · ·" : Type.fit(lyrics.current(), W, LINE_CAP, Weight.SEMIBOLD);
		float sung = lyrics.current().isBlank() ? 1 : lyrics.sung(song);
		float old = Paint.opacity(Paint.opacity() * t);
		glowText(d, line, cx - Type.width(line, LINE_CAP, Weight.SEMIBOLD) / 2f, lineTop + 4.5f * (1 - t), sung, shadow);
		String next = lyrics.next();
		if (!next.isBlank()) {
			String shownNext = Type.fit(next, W, NEXT_CAP, Weight.MEDIUM);
			float nextTop = top + (23 * 1.3f + 6) * S + (14 * 1.6f * S - NEXT_CAP) / 2f;
			Hud.text(d, shownNext, cx - Type.width(shownNext, NEXT_CAP, Weight.MEDIUM) / 2f, nextTop, NEXT_CAP, Weight.MEDIUM, Tokens.TEXT_3, shadow);
		}
		Paint.opacity(old);
	}

	/** A lyric line in white with a soft lilac glow, white as far as it's been sung and faint after. */
	private static void glowText(Draw d, String text, float x, float capTop, float sung, boolean shadow) {
		float w = Type.width(text, LINE_CAP, Weight.SEMIBOLD);
		Paint.shadow(d, x, capTop, x + w, capTop + LINE_CAP, LINE_CAP / 2f, 0, 0, 30 * S, 0, 0x26CDB6FF);
		Hud.karaoke(d, text, x, capTop, LINE_CAP, Weight.SEMIBOLD, sung, 0xFFFFFFFF, 0x57F6F3FF, shadow);
	}

	private void drawCard(Draw d, NowPlaying song, float x, float top, long now, float dt, boolean shadow) {
		float h = cardHeight(song);
		float r = radius(18 * S);
		float strength = glassStrength();
		if (strength > 0.01f) {
			float old = Paint.opacity(Paint.opacity() * Math.min(1, strength * 1.15f));
			Paint.shadow(d, x, top, x + W, top + h, r, 0, 24 * S, 50 * S, -24 * S, 0xE6000000);
			Paint.rect(d, x, top, x + W, top + h, r, Tokens.GLASS);
			Paint.stroke(d, x, top, x + W, top + h, r, Hud.hairline(d), 0x1AFFFFFF);
			Paint.stroke(d, x + Hud.hairline(d), top + Hud.hairline(d), x + W - Hud.hairline(d), top + h - Hud.hairline(d), r, Hud.hairline(d),
					Grad.down(0x12FFFFFF, 0x00FFFFFF));
			Paint.opacity(old);
		}
		float change = Ease.OUT.apply((now - changedAt) / (double) CHANGE_MS);
		float ax = x + PAD_L;
		float ay = top + (CARD_H - ART) / 2f;
		float metaX;
		if (Modules.MUSIC_ART.get()) {
			drawArt(d, song, ax, ay, change);
			metaX = ax + ART + GAP;
		} else {
			metaX = x + PAD_L + 2;
		}
		float controlsX = x + W - PAD_R - CONTROLS;
		float metaW = controlsX - GAP - metaX;
		drawControls(d, controlsX, top + CARD_H / 2f, now);

		float titleCap = 14.5f * 0.727f * S;
		float artistCap = 12.5f * 0.727f * S;
		float titleTop = top + 12 * S + (14.5f * 1.6f - 14.5f * 0.727f) / 2f * S;
		float artistTop = top + (12 + 14.5f * 1.6f) * S + (12.5f * 1.6f - 12.5f * 0.727f) / 2f * S;
		if (change < 1 && !oldTitle.isEmpty()) {
			float old = Paint.opacity(Paint.opacity() * (1 - change));
			meta(d, oldTitle, oldArtist, metaX, titleTop - 4 * change, artistTop - 4 * change, metaW, titleCap, artistCap, shadow);
			Paint.opacity(old);
		}
		float in = Paint.opacity(Paint.opacity() * change);
		meta(d, song.title(), song.artist(), metaX, titleTop + 4 * (1 - change), artistTop + 4 * (1 - change), metaW, titleCap, artistCap, shadow);
		Paint.opacity(in);

		if (progress(song)) {
			double position = song.positionNow();
			float target = song.duration() > 0 ? (float) Math.max(0, Math.min(1, position / song.duration())) : 0;
			if (shownProgress < 0 || Math.abs(target - shownProgress) < 0.004f) shownProgress = target;
			else shownProgress += (target - shownProgress) * (1 - (float) Math.exp(-dt * 9));
			float barTop = top + (12 + 14.5f * 1.6f + 12.5f * 1.6f + 8) * S;
			float barH = 3 * S;
			Paint.rect(d, metaX, barTop, metaX + metaW, barTop + barH, barH / 2f, 0x1AFFFFFF);
			float fx = metaX + metaW * shownProgress;
			if (fx > metaX + barH / 2f) Paint.rect(d, metaX, barTop, fx, barTop + barH, barH / 2f, fill());
			float timeCap = 10.5f * 0.727f * S;
			float timeTop = barTop + barH + 4 * S + (10.5f * 1.6f - 10.5f * 0.727f) / 2f * S;
			String played = NowPlaying.time(position);
			String length = NowPlaying.time(song.duration());
			Hud.text(d, played, metaX, timeTop, timeCap, Weight.SEMIBOLD, Tokens.TEXT_3, shadow);
			Hud.text(d, length, metaX + metaW - Type.width(length, timeCap, Weight.SEMIBOLD), timeTop, timeCap, Weight.SEMIBOLD, Tokens.TEXT_3, shadow);
		}

		if (volume(song)) {
			float cy = top + CARD_H + VOLUME_ROW / 2f - 3;
			boolean silent = song.muted() || song.volume() == 0;
			Paint.icon(d, silent ? "volume-x" : "volume-2", x + PAD_L, cy - 3.5f, 7, Tokens.TEXT_2);
			String level = song.muted() ? "Muted" : song.volume() + "%";
			float small = 3.6f;
			float levelW = Math.max(Type.width(level, small, Weight.SEMIBOLD), Type.width("100%", small, Weight.SEMIBOLD));
			float bx1 = x + PAD_L + 11;
			float bx2 = x + W - PAD_R - levelW - 6;
			float vh = 3 * S;
			Paint.rect(d, bx1, cy - vh / 2f, bx2, cy + vh / 2f, vh / 2f, 0x1AFFFFFF);
			float vx = bx1 + (bx2 - bx1) * (song.muted() ? 0 : song.volume() / 100f);
			if (vx > bx1 + vh / 2f) Paint.rect(d, bx1, cy - vh / 2f, vx, cy + vh / 2f, vh / 2f, fill());
			Hud.text(d, level, x + W - PAD_R - Type.width(level, small, Weight.SEMIBOLD), cy - small / 2f, small, Weight.SEMIBOLD, Tokens.TEXT_3,
					shadow);
		}
	}

	private static void meta(Draw d, String title, String artist, float x, float titleTop, float artistTop, float width, float titleCap,
			float artistCap, boolean shadow) {
		Hud.text(d, Type.fit(title.isBlank() ? "Unknown song" : title, width, titleCap, Weight.SEMIBOLD), x, titleTop, titleCap, Weight.SEMIBOLD,
				Tokens.TEXT, shadow);
		Hud.text(d, Type.fit(artist, width, artistCap, Weight.MEDIUM), x, artistTop, artistCap, Weight.MEDIUM, Tokens.TEXT_2, shadow);
	}

	/** The cover with soft corners, fading in for a new song over the website's violet tile with the Eventide mark, and the app's icon on its corner. */
	private static void drawArt(Draw d, NowPlaying song, float x, float y, float change) {
		float r = 12 * S;
		boolean hasArt = song.art() != null;
		if (!hasArt || change < 1) {
			Paint.rect(d, x, y, x + ART, y + ART, r, ART_FILL);
			Paint.stroke(d, x, y, x + ART, y + ART, r, Hud.hairline(d), Grad.down(0x33FFFFFF, 0x00FFFFFF));
			float mark = 30 * S;
			Logo.mark(d, x + (ART - mark) / 2f, y + (ART - mark) / 2f, mark, 1, System.currentTimeMillis());
		}
		if (hasArt) {
			float old = Paint.opacity(Paint.opacity() * change);
			MusicArt.COVER.draw(d, song.art(), song.artVersion(), x, y, ART, r);
			Paint.opacity(old);
		}
		if (song.icon() != null) {
			float badge = ART * 0.36f;
			float bx = x + ART - badge * 0.75f;
			float by = y + ART - badge * 0.75f;
			Paint.circle(d, bx + badge / 2f, by + badge / 2f, badge / 2f + 1, Tokens.GLASS_SOLID);
			MusicArt.ICON.draw(d, song.icon(), song.iconVersion(), bx, by, badge, badge / 2f);
		}
	}

	/** Back, the light round play / pause button (turning into each other with a little bounce), and next. */
	private void drawControls(Draw d, float x, float cy, long now) {
		float icon = 17 * S;
		Paint.icon(d, "skip-back", x, cy - icon / 2f, icon, Tokens.TEXT_2);
		float pr = 17 * S * (0.88f + 0.12f * Ease.SPRING.apply(Math.min(1, (now - toggledAt) / 420.0)));
		float pcx = x + (17 + 10 + 17) * S;
		Paint.circle(d, pcx, cy, pr, 0xFFF1E8FF);
		float p = playing.get();
		float glyph = 15 * S;
		float pause = glyph * (0.6f + 0.4f * p);
		float old = Paint.opacity(Paint.opacity() * p);
		Paint.icon(d, "pause", pcx - pause / 2f, cy - pause / 2f, pause, 1, true, 0xFF140A24, null);
		Paint.opacity(old);
		float play = glyph * (0.6f + 0.4f * (1 - p));
		old = Paint.opacity(Paint.opacity() * (1 - p));
		Paint.icon(d, "play", pcx - play / 2f + 0.35f, cy - play / 2f, play, 1, true, 0xFF140A24, null);
		Paint.opacity(old);
		Paint.icon(d, "skip-forward", x + (17 + 10 + 34 + 10) * S, cy - icon / 2f, icon, Tokens.TEXT_2);
	}
}
