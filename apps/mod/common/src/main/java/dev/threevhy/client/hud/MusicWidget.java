package dev.threevhy.client.hud;

import dev.threevhy.client.Draw;
import dev.threevhy.client.gui.PixelIcons;
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
import net.minecraft.resources.Identifier;

/**
 * The Music HUD, like a media player: the cover (with the music app's logo on its corner), the title
 * in bold and the artist in the accent colour under it, previous / play-pause / next on the right, a
 * progress bar between the time played and the song's length, and Windows' volume along the bottom.
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
	private static final int FILL = 0x16171B;

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
		if (alpha > 0) d.roundRect(x, y, x + W, y + h, 8, fill);
		boolean shadow = textShadow();
		int accent = accent();

		int textX = x + PAD;
		if (Modules.MUSIC_ART.get()) {
			drawCover(d, song, x + PAD, y + PAD, alpha > 0 ? fill : 0);
			textX = x + PAD + ART + 8;
		}

		int controlsX = x + W - PAD - CONTROLS_W;
		drawControls(d, controlsX, y + PAD - 1, song.playing(), accent);

		int textW = controlsX - 6 - textX;
		String title = song.title().isBlank() ? "Unknown song" : song.title();
		d.text(heading(fit(mc, title.toUpperCase(Locale.ROOT), textW, MusicWidget::heading)), textX, y + PAD + 2, WHITE, shadow);
		small(d, fit(mc, song.artist().toUpperCase(Locale.ROOT), Math.round(textW / 0.8f), MusicWidget::plain), textX, y + PAD + 13,
				0.8f, accent, shadow);

		if (progress(song)) {
			double position = song.positionNow();
			int barX1 = textX;
			int barX2 = x + W - PAD;
			String played = NowPlaying.time(position);
			String length = NowPlaying.time(song.duration());
			small(d, played, barX1, y + PAD + 23, 0.6f, GREY, shadow);
			small(d, length, barX2 - Math.round(mc.font.width(plain(length)) * 0.6f), y + PAD + 23, 0.6f, GREY, shadow);
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

	/** The cover with soft corners, and the music app's logo on its bottom right corner. */
	private static void drawCover(Draw d, NowPlaying song, int x, int y, int behind) {
		Identifier cover = MusicArt.COVER.texture(song.art(), song.artVersion());
		if (cover != null) {
			int[] size = MusicArt.COVER.size();
			d.texture(cover, x, y, ART, ART, size[0], size[1]);
			if (behind != 0) roundCorners(d, x, y, ART, ART, behind);
		} else {
			d.roundRect(x, y, x + ART, y + ART, 4, 0xFF2A2D35);
			PixelIcons.draw(d, PixelIcons.forModule(Modules.MUSIC), x + (ART - 24) / 2, y + (ART - 24) / 2, 2, Theme.SKY);
		}
		Identifier icon = MusicArt.ICON.texture(song.icon(), song.iconVersion());
		if (icon != null) {
			int bx = x + ART - 10;
			int by = y + ART - 10;
			d.roundRect(bx - 2, by - 2, bx + 14, by + 14, 4, 0xFF16171B);
			int[] size = MusicArt.ICON.size();
			d.texture(icon, bx, by, 12, 12, size[0], size[1]);
		}
	}

	/** Paints the panel's colour over a square's corner pixels, so it looks rounded. */
	private static void roundCorners(Draw d, int x, int y, int w, int h, int color) {
		int right = x + w;
		int bottom = y + h;
		d.fill(x, y, x + 2, y + 1, color);
		d.fill(x, y + 1, x + 1, y + 2, color);
		d.fill(right - 2, y, right, y + 1, color);
		d.fill(right - 1, y + 1, right, y + 2, color);
		d.fill(x, bottom - 1, x + 2, bottom, color);
		d.fill(x, bottom - 2, x + 1, bottom - 1, color);
		d.fill(right - 2, bottom - 1, right, bottom, color);
		d.fill(right - 1, bottom - 2, right, bottom - 1, color);
	}

	/** Previous, play or pause in a ring, and next: what's playing, like a media player shows it. */
	private static void drawControls(Draw d, int x, int y, boolean playing, int accent) {
		int cy = y + 8;
		// Previous: a bar and a triangle pointing left
		d.fill(x, cy - 3, x + 1, cy + 4, WHITE);
		for (int i = 0; i < 4; i++) d.fill(x + 2 + i, cy - i, x + 3 + i, cy + 1 + i, WHITE);
		// Play / pause in a ring
		int rx = x + 11;
		d.roundOutline(rx, y, rx + 16, y + 16, 8, accent);
		d.roundOutline(rx + 1, y + 1, rx + 15, y + 15, 7, accent);
		if (playing) {
			d.fill(rx + 5, y + 5, rx + 7, y + 11, WHITE);
			d.fill(rx + 9, y + 5, rx + 11, y + 11, WHITE);
		} else {
			for (int i = 0; i < 4; i++) d.fill(rx + 6 + i, y + 4 + i, rx + 7 + i, y + 12 - i, WHITE);
		}
		// Next: a triangle pointing right and a bar
		int nx = x + 32;
		for (int i = 0; i < 4; i++) d.fill(nx + i, cy - 3 + i, nx + 1 + i, cy + 4 - i, WHITE);
		d.fill(nx + 5, cy - 3, nx + 6, cy + 4, WHITE);
	}

	/** A speaker with sound waves, or with an X when muted. */
	private static void speaker(Draw d, int x, int y, boolean muted) {
		d.fill(x, y + 2, x + 2, y + 5, WHITE);
		for (int i = 0; i < 3; i++) d.fill(x + 2 + i, y + 2 - i, x + 3 + i, y + 5 + i, WHITE);
		if (muted) {
			for (int i = 0; i < 3; i++) {
				d.fill(x + 6 + i, y + 2 + i, x + 7 + i, y + 3 + i, GREY);
				d.fill(x + 8 - i, y + 2 + i, x + 9 - i, y + 3 + i, GREY);
			}
		} else {
			d.fill(x + 6, y + 2, x + 7, y + 5, WHITE);
			d.fill(x + 8, y + 1, x + 9, y + 6, WHITE);
		}
	}

	/** A thin track, coloured up to {@code fraction}, with a round white knob there. */
	private static void slider(Draw d, int x1, int x2, int y, double fraction, int color) {
		d.roundRect(x1, y, x2, y + 3, 1, TRACK);
		int at = x1 + (int) Math.round((x2 - x1) * fraction);
		if (at > x1) d.roundRect(x1, y, at, y + 3, 1, color);
		d.roundRect(at - 3, y - 2, at + 4, y + 5, 3, WHITE);
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
