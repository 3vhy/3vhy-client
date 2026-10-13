package dev.threevhy.client.hud;

import dev.threevhy.client.Draw;
import dev.threevhy.client.gui.PixelIcons;
import dev.threevhy.client.module.Modules;
import dev.threevhy.client.music.MusicArt;
import dev.threevhy.client.music.MusicBridge;
import dev.threevhy.client.music.NowPlaying;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

/**
 * The Music HUD, like a media player: the cover (with the music app's icon on its corner), the title
 * in bold and the artist under it, previous / play-pause / next on the right, a progress bar with
 * the time played and the song's length, and Windows' volume along the bottom.
 */
final class MusicWidget extends HudWidget {
	private static final int W = 168;
	private static final int PAD = 6;
	private static final int ART = 30;
	private static final int CONTROLS_W = 31;
	private static final int VOLUME_ROW = 12;
	private static final int WHITE = 0xFFFFFFFF;
	private static final int GREY = 0xFFA3A9B6;
	private static final int TRACK = 0x47FFFFFF;

	MusicWidget() {
		super("music", Modules.MUSIC, Anchor.TOP_CENTER);
	}

	private static boolean progress(NowPlaying song) {
		return Modules.MUSIC_PROGRESS.get() && (song == null || song.hasTimeline());
	}

	private static boolean volume(NowPlaying song) {
		return Modules.MUSIC_VOLUME.get() && song != null && song.hasVolume();
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
		return PAD + ART + (volume(song) ? VOLUME_ROW : 0) + PAD - 1;
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
		if (alpha > 0) {
			int fill = (alpha << 24) | 0x0E1014;
			if (rounded()) d.roundRect(x, y, x + W, y + h, 6, fill);
			else d.fill(x, y, x + W, y + h, fill);
		}
		boolean shadow = textShadow();

		int textX = x + PAD;
		if (Modules.MUSIC_ART.get()) {
			drawCover(d, song, x + PAD, y + PAD);
			textX = x + PAD + ART + 6;
		}

		int controlsX = x + W - PAD - CONTROLS_W;
		drawControls(d, controlsX, y + PAD, song.playing());

		int textW = controlsX - 5 - textX;
		String title = song.title().isBlank() ? "Unknown song" : song.title();
		Component bold = Component.literal(trimBold(mc, title.toUpperCase(Locale.ROOT), textW)).withStyle(Style.EMPTY.withBold(true));
		d.text(bold, textX, y + PAD + 1, WHITE, shadow);
		small(d, trim(mc, song.artist().toUpperCase(Locale.ROOT), Math.round(textW / 0.75f)), textX, y + PAD + 11, 0.75f, GREY,
				shadow);

		if (progress(song)) {
			double position = song.positionNow();
			int barX1 = textX;
			int barX2 = x + W - PAD;
			String played = NowPlaying.time(position);
			String length = NowPlaying.time(song.duration());
			small(d, played, barX1, y + PAD + 19, 0.6f, GREY, shadow);
			small(d, length, barX2 - Math.round(mc.font.width(length) * 0.6f), y + PAD + 19, 0.6f, GREY, shadow);
			double fraction = song.duration() > 0 ? Math.max(0, Math.min(1, position / song.duration())) : 0;
			slider(d, barX1, barX2, y + PAD + 27, fraction);
		}

		if (volume(song)) {
			int rowY = y + PAD + ART + 4;
			speaker(d, x + PAD, rowY, song.muted() || song.volume() == 0);
			int barX1 = x + PAD + 11;
			int barX2 = barX1 + 46;
			slider(d, barX1, barX2, rowY + 3, song.muted() ? 0 : song.volume() / 100.0);
			String level = song.muted() ? "MUTED" : song.volume() + "%";
			small(d, level, barX2 + 6, rowY + 1, 0.6f, GREY, shadow);
		}
	}

	/** The cover, outlined, with the music app's icon in its bottom right corner. */
	private static void drawCover(Draw d, NowPlaying song, int x, int y) {
		Identifier cover = MusicArt.COVER.texture(song.art(), song.artVersion());
		if (cover != null) {
			int[] size = MusicArt.COVER.size();
			d.texture(cover, x, y, ART, ART, size[0], size[1]);
		} else {
			box(d, x, y, x + ART, y + ART, 0xFF1C1F26);
			PixelIcons.draw(d, PixelIcons.forModule(Modules.MUSIC), x + (ART - 24) / 2, y + (ART - 24) / 2, 2, Theme.SKY);
		}
		d.outline(x, y, x + ART, y + ART, 0x26FFFFFF);
		Identifier icon = MusicArt.ICON.texture(song.icon(), song.iconVersion());
		if (icon != null) {
			int bx = x + ART - 9;
			int by = y + ART - 9;
			d.roundRect(bx - 1, by - 1, bx + 11, by + 11, 3, 0xFF0E1014);
			int[] size = MusicArt.ICON.size();
			d.texture(icon, bx, by, 10, 10, size[0], size[1]);
		}
	}

	/** Previous, play or pause in a ring of 3vhy blue, and next: what's playing, like a media player shows it. */
	private static void drawControls(Draw d, int x, int y, boolean playing) {
		int cy = y + 6;
		// Previous: a bar and a triangle pointing left
		d.fill(x, cy - 2, x + 1, cy + 3, WHITE);
		for (int i = 0; i < 3; i++) d.fill(x + 1 + i, cy - i, x + 2 + i, cy + 1 + i, WHITE);
		// Play / pause in a ring
		int rx = x + 9;
		d.roundOutline(rx, y, rx + 13, y + 13, 6, Theme.CYAN);
		d.roundOutline(rx + 1, y + 1, rx + 12, y + 12, 5, 0x805BC8E6);
		if (playing) {
			d.fill(rx + 4, y + 4, rx + 6, y + 9, WHITE);
			d.fill(rx + 7, y + 4, rx + 9, y + 9, WHITE);
		} else {
			for (int i = 0; i < 3; i++) d.fill(rx + 5 + i, y + 4 + i, rx + 6 + i, y + 9 - i, WHITE);
		}
		// Next: a triangle pointing right and a bar
		int nx = x + 26;
		for (int i = 0; i < 3; i++) d.fill(nx + i, cy - 2 + i, nx + 1 + i, cy + 3 - i, WHITE);
		d.fill(nx + 4, cy - 2, nx + 5, cy + 3, WHITE);
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
			d.fill(x + 6, y + 2, x + 7, y + 5, GREY);
			d.fill(x + 8, y + 1, x + 9, y + 6, GREY);
		}
	}

	/** A thin track, white up to {@code fraction}, with a round white knob there. */
	private static void slider(Draw d, int x1, int x2, int y, double fraction) {
		d.fill(x1, y, x2, y + 2, TRACK);
		int at = x1 + (int) Math.round((x2 - x1) * fraction);
		d.fill(x1, y, at, y + 2, WHITE);
		d.roundRect(at - 2, y - 2, at + 3, y + 4, 2, WHITE);
	}

	private static void small(Draw d, String text, float x, float y, float scale, int color, boolean shadow) {
		d.push();
		d.translate(x, y);
		d.scale(scale);
		d.text(text, 0, 0, color, shadow);
		d.pop();
	}

	private static String trim(Minecraft mc, String text, int maxWidth) {
		if (maxWidth <= 0) return "";
		if (mc.font.width(text) <= maxWidth) return text;
		String cut = text;
		while (!cut.isEmpty() && mc.font.width(cut + "...") > maxWidth) cut = cut.substring(0, cut.length() - 1);
		return cut.stripTrailing() + "...";
	}

	private static String trimBold(Minecraft mc, String text, int maxWidth) {
		if (maxWidth <= 0) return "";
		if (boldWidth(mc, text) <= maxWidth) return text;
		String cut = text;
		while (!cut.isEmpty() && boldWidth(mc, cut + "...") > maxWidth) cut = cut.substring(0, cut.length() - 1);
		return cut.stripTrailing() + "...";
	}

	private static int boldWidth(Minecraft mc, String text) {
		return mc.font.width(Component.literal(text).withStyle(Style.EMPTY.withBold(true)));
	}
}
