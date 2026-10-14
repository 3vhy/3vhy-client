package dev.threevhy.client.hud;

import dev.threevhy.client.Draw;
import dev.threevhy.client.gui.PixelIcons;
import dev.threevhy.client.gui.Smooth;
import dev.threevhy.client.gui.UiFont;
import dev.threevhy.client.module.Modules;
import dev.threevhy.client.music.MusicArt;
import dev.threevhy.client.music.MusicBridge;
import dev.threevhy.client.music.NowPlaying;
import java.util.Locale;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

/**
 * The Music HUD, like 67's: the cover (with the music app's logo on its corner), the title in bold
 * and the artist in grey under it, previous / play-pause / next on the right, the time played over
 * a progress bar with the song's length at its end, and Windows' volume along the bottom.
 * Text is in the clean font ({@link UiFont}) unless Theme's Menu font is set to Minecraft.
 */
final class MusicWidget extends HudWidget {
	private static final int W = 200;
	private static final int PAD = 8;
	private static final int ART = 40;
	private static final int CONTROLS_W = 38;
	private static final int VOLUME_ROW = 16;
	private static final int WHITE = 0xFFFFFFFF;
	private static final int GREY = 0xFFA3A9B6;
	private static final int TRACK = 0x38FFFFFF;
	private static final int FILL = 0x111214;
	private static final int BADGE = 0xFF111214;
	private static final int ARTIST = 0xFFB4B8C0;

	MusicWidget() {
		super("music", Modules.MUSIC, Anchor.TOP_CENTER);
	}

	private static boolean progress(NowPlaying song) {
		return Modules.MUSIC_PROGRESS.get() && (song == null || song.hasTimeline());
	}

	private static boolean volume(NowPlaying song) {
		return Modules.MUSIC_VOLUME.get() && song != null && song.hasVolume();
	}

	private static int accent() {
		return Modules.MUSIC_COLOR.get() | 0xFF000000;
	}

	/** What to draw: the real song, or a sample in the hub so the widget can be placed. */
	private static NowPlaying song(boolean preview) {
		NowPlaying song = MusicBridge.current();
		if (song != null || !preview) return song;
		String title = MusicBridge.supported() ? "Nothing playing" : "Music HUD";
		String artist = MusicBridge.supported() ? "Play a song on Spotify, YouTube..." : "Works on Windows";
		return new NowPlaying("sample", "", title, artist, "", false, 42, 175, System.nanoTime(), null, -1, null, -1, "", 64,
				false);
	}

	@Override
	public boolean visible(Minecraft mc) {
		NowPlaying song = MusicBridge.current();
		return song != null && (song.playing() || Modules.MUSIC_PAUSED.get());
	}

	@Override
	public int width(Minecraft mc) {
		return W;
	}

	private static int heightFor(NowPlaying song) {
		return PAD + ART + (volume(song) ? VOLUME_ROW : 0) + PAD;
	}

	@Override
	public int height(Minecraft mc) {
		NowPlaying song = MusicBridge.current();
		return heightFor(song != null ? song : song(true));
	}

	@Override
	public void draw(Draw d, int x, int y, Minecraft mc, boolean preview) {
		NowPlaying song = song(preview);
		if (song == null) return;
		int h = heightFor(song);
		int alpha = backgroundAlpha();
		int fill = (alpha << 24) | FILL;
		if (alpha > 0) Smooth.roundRect(d, x, y, x + W, y + h, 8, fill);
		boolean shadow = textShadow();
		int accent = accent();

		int textX = x + PAD;
		if (Modules.MUSIC_ART.get()) {
			drawCover(d, song, x + PAD, y + PAD);
			textX = x + PAD + ART + 8;
		}

		int controlsX = x + W - PAD - CONTROLS_W;
		drawControls(d, controlsX, y + PAD - 1, song.playing(), accent);

		int textW = controlsX - 6 - textX;
		String title = song.title().isBlank() ? "Unknown song" : song.title();
		d.text(heading(fit(mc, title.toUpperCase(Locale.ROOT), textW, MusicWidget::heading)), textX, y + PAD + 2, WHITE, shadow);
		small(d, fit(mc, song.artist().toUpperCase(Locale.ROOT), Math.round(textW / 0.8f), MusicWidget::plain), textX, y + PAD + 13,
				0.8f, ARTIST, shadow);

		if (progress(song)) {
			double position = song.positionNow();
			String played = NowPlaying.time(position);
			String length = NowPlaying.time(song.duration());
			int lengthW = Math.round(mc.font.width(plain(length)) * 0.6f);
			int barX1 = textX;
			int barX2 = x + W - PAD - lengthW - 8;
			small(d, played, barX1, y + PAD + 24, 0.6f, GREY, shadow);
			small(d, length, x + W - PAD - lengthW, y + PAD + 31, 0.6f, GREY, shadow);
			double fraction = song.duration() > 0 ? Math.max(0, Math.min(1, position / song.duration())) : 0;
			slider(d, barX1, barX2, y + PAD + 32, fraction, accent);
		}

		if (volume(song)) {
			int rowY = y + PAD + ART + 6;
			boolean silent = song.muted() || song.volume() == 0;
			speaker(d, x + PAD + 1, rowY, silent);
			String level = song.muted() ? "MUTED" : song.volume() + "%";
			int levelW = Math.round(mc.font.width(plain(level)) * 0.7f);
			int barX1 = x + PAD + 17;
			int barX2 = x + W - PAD - Math.max(levelW, 18) - 8;
			slider(d, barX1, barX2, rowY + 3, song.muted() ? 0 : song.volume() / 100.0, accent);
			small(d, level, x + W - PAD - levelW, rowY + 1, 0.7f, GREY, shadow);
		}
	}

	/** The cover with soft corners, and the music app's logo in a round badge on its bottom right corner. */
	private static void drawCover(Draw d, NowPlaying song, int x, int y) {
		if (!MusicArt.COVER.draw(d, song.art(), song.artVersion(), x, y, ART, 5)) {
			Smooth.roundRect(d, x, y, x + ART, y + ART, 5, 0xFF2A2D35);
			PixelIcons.draw(d, PixelIcons.forModule(Modules.MUSIC), x + (ART - 24) / 2, y + (ART - 24) / 2, 2, Theme.SKY);
		}
		if (song.icon() != null) {
			int bx = x + ART - 10;
			int by = y + ART - 10;
			Smooth.circle(d, bx + 6, by + 6, 8, BADGE);
			MusicArt.ICON.draw(d, song.icon(), song.iconVersion(), bx, by, 12, 6);
		}
	}

	/** Previous, play or pause in a ring, and next, like a media player. */
	private static void drawControls(Draw d, int x, int y, boolean playing, int accent) {
		float cy = y + 8;
		Smooth.icon(d, "prev", x - 1, cy - 5, 10, WHITE);
		float rx = x + 19;
		Smooth.ring(d, rx, cy, 8, 1.3f, accent);
		Smooth.icon(d, playing ? "pause" : "play", rx - 4.5f, cy - 4.5f, 9, WHITE);
		Smooth.icon(d, "next", x + 29, cy - 5, 10, WHITE);
	}

	/** A speaker with sound waves, or with an x when muted. */
	private static void speaker(Draw d, int x, int y, boolean muted) {
		Smooth.icon(d, muted ? "speaker_muted" : "speaker", x - 1, y - 1.5f, 10, WHITE);
	}

	/** A thin rounded track, coloured up to {@code fraction}, with a round white knob there. */
	private static void slider(Draw d, int x1, int x2, int y, double fraction, int color) {
		Smooth.roundRect(d, x1, y, x2, y + 3, 1.5f, TRACK);
		float at = (float) (x1 + (x2 - x1) * fraction);
		if (at > x1) Smooth.roundRect(d, x1, y, at, y + 3, 1.5f, color);
		Smooth.circle(d, at, y + 1.5f, 3.5f, WHITE);
	}

	private static void small(Draw d, String text, float x, float y, float scale, int color, boolean shadow) {
		d.push();
		d.translate(x, y);
		d.scale(scale);
		d.text(plain(text), 0, 0, color, shadow);
		d.pop();
	}

	/** The song's title: bold, in the clean font or Minecraft's. */
	private static Component heading(String text) {
		return UiFont.clean() ? UiFont.bold(text) : Component.literal(text).withStyle(Style.EMPTY.withBold(true));
	}

	/** Everything else: the clean font's semibold, or Minecraft's font. */
	private static Component plain(String text) {
		return UiFont.clean() ? UiFont.semibold(text) : Component.literal(text);
	}

	/** {@code text} cut down with "..." to fit {@code maxWidth} when drawn in {@code style}. */
	private static String fit(Minecraft mc, String text, int maxWidth, Function<String, Component> style) {
		if (maxWidth <= 0) return "";
		if (mc.font.width(style.apply(text)) <= maxWidth) return text;
		String cut = text;
		while (!cut.isEmpty() && mc.font.width(style.apply(cut + "...")) > maxWidth) cut = cut.substring(0, cut.length() - 1);
		return cut.stripTrailing() + "...";
	}
}
