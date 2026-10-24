package dev.threevhy.client.hud;

import dev.threevhy.client.Draw;
import dev.threevhy.client.gui.Accent;
import dev.threevhy.client.module.Modules;
import dev.threevhy.client.music.MusicArt;
import dev.threevhy.client.music.MusicBridge;
import dev.threevhy.client.music.NowPlaying;
import dev.threevhy.client.ui.Grad;
import dev.threevhy.client.ui.Paint;
import dev.threevhy.client.ui.Tokens;
import dev.threevhy.client.ui.Type;
import dev.threevhy.client.ui.Type.Weight;
import net.minecraft.client.Minecraft;

/**
 * The Music Player, on violet glass: the cover (with the music app's logo on its corner), the title in
 * bold and the artist under it, previous / play-pause / next on the right, the time played over a
 * progress bar with the song's length at its end, and Windows' volume along the bottom.
 */
final class MusicWidget extends HudWidget {
	private static final int W = 200;
	private static final float PAD = 8;
	private static final float ART = 40;
	private static final float CONTROLS_W = 44;
	private static final float VOLUME_ROW = 15;

	MusicWidget() {
		super("music", Modules.MUSIC, Anchor.TOP_CENTER);
	}

	private static boolean progress(NowPlaying song) {
		return Modules.MUSIC_PROGRESS.get() && (song == null || song.hasTimeline());
	}

	private static boolean volume(NowPlaying song) {
		return Modules.MUSIC_VOLUME.get() && song != null && song.hasVolume();
	}

	/** The player's colour: the accent's gradient by default, or a gradient around the colour picked. */
	private static Grad fill() {
		int c = Modules.MUSIC_COLOR.get() | 0xFF000000;
		if (c == (Tokens.PRIMARY_MID | 0xFF000000)) return Accent.bar();
		return new Grad(Accent.turn(c, -14), Accent.turn(c, 10), 90);
	}

	/** What to draw: the real song, or a sample in the layout editor so the widget can be placed. */
	private static NowPlaying song(boolean preview) {
		NowPlaying song = MusicBridge.current();
		if (song != null || !preview) return song;
		String title = MusicBridge.supported() ? "Nothing playing" : "Music Player";
		String artist = MusicBridge.supported() ? "Play a song on Spotify, YouTube..." : "Works on Windows";
		return new NowPlaying("sample", "", title, artist, "", false, 42, 175, System.nanoTime(), null, -1, null, -1, "", 64, false);
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
		return Math.round(PAD + ART + (volume(song) ? VOLUME_ROW : 0) + PAD);
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
		glass(d, x, y, W, h, 10);
		boolean shadow = textShadow();
		Grad fill = fill();

		float textX = x + PAD;
		if (Modules.MUSIC_ART.get()) {
			drawCover(d, song, x + PAD, y + PAD);
			textX = x + PAD + ART + 8;
		}
		float controlsX = x + W - PAD - CONTROLS_W;
		drawControls(d, controlsX, y + PAD + 7, song.playing(), fill);

		float textW = controlsX - 6 - textX;
		String title = song.title().isBlank() ? "Unknown song" : song.title();
		Hud.text(d, Type.fit(title, textW, Hud.CAP, Weight.BOLD), textX, y + PAD + 2.5f, Hud.CAP, Weight.BOLD, Tokens.TEXT, shadow);
		Hud.text(d, Type.fit(song.artist(), textW, Hud.SMALL, Weight.MEDIUM), textX, y + PAD + 11.5f, Hud.SMALL, Weight.MEDIUM, Tokens.TEXT_2,
				shadow);

		if (progress(song)) {
			double position = song.positionNow();
			String played = NowPlaying.time(position);
			String length = NowPlaying.time(song.duration());
			float small = 3.5f;
			float barY = y + PAD + 31;
			float lengthW = Type.width(length, small, Weight.SEMIBOLD);
			float barX2 = x + W - PAD;
			Hud.text(d, played, textX, barY - small - 3, small, Weight.SEMIBOLD, Tokens.TEXT_3, shadow);
			Hud.text(d, length, barX2 - lengthW, barY - small - 3, small, Weight.SEMIBOLD, Tokens.TEXT_3, shadow);
			double fraction = song.duration() > 0 ? Math.max(0, Math.min(1, position / song.duration())) : 0;
			slider(d, textX, barX2, barY, (float) fraction, fill);
		}

		if (volume(song)) {
			float rowY = y + PAD + ART + 7;
			boolean silent = song.muted() || song.volume() == 0;
			Paint.icon(d, silent ? "volume-x" : "volume-2", x + PAD, rowY - 4, 8, Tokens.TEXT_2);
			String level = song.muted() ? "Muted" : song.volume() + "%";
			float small = 3.9f;
			float levelW = Math.max(Type.width(level, small, Weight.SEMIBOLD), Type.width("100%", small, Weight.SEMIBOLD));
			slider(d, x + PAD + 13, x + W - PAD - levelW - 6, rowY, song.muted() ? 0 : song.volume() / 100f, fill);
			Hud.text(d, level, x + W - PAD - Type.width(level, small, Weight.SEMIBOLD), rowY - small / 2f, small, Weight.SEMIBOLD, Tokens.TEXT_2,
					shadow);
		}
	}

	/** The cover with soft corners, and the music app's logo in a round badge on its bottom right corner. */
	private static void drawCover(Draw d, NowPlaying song, float x, float y) {
		Paint.shadow(d, x, y, x + ART, y + ART, 7, 0, 3, 8, -2, 0x8C000000);
		if (!MusicArt.COVER.draw(d, song.art(), song.artVersion(), x, y, ART, 7)) {
			Paint.rect(d, x, y, x + ART, y + ART, 7, Accent.tile());
			Paint.icon(d, "music", x + ART / 2f - 9, y + ART / 2f - 9, 18, 0xFFFFFFFF);
		}
		if (song.icon() != null) {
			float bx = x + ART - 10;
			float by = y + ART - 10;
			Paint.circle(d, bx + 6, by + 6, 8, Tokens.GLASS_SOLID);
			MusicArt.ICON.draw(d, song.icon(), song.iconVersion(), bx, by, 12, 6);
		}
	}

	/** Previous, play or pause on the accent, and next. */
	private static void drawControls(Draw d, float x, float cy, boolean playing, Grad fill) {
		Paint.icon(d, "skip-back", x, cy - 4.5f, 9, Tokens.TEXT_2);
		float rx = x + 22;
		Paint.shadow(d, rx - 8, cy - 8, rx + 8, cy + 8, 8, 0, 0, 8, -2, Accent.glow(0.6f));
		Paint.circle(d, rx, cy, 8, fill);
		Paint.icon(d, playing ? "pause" : "play", rx - 4, cy - 4, 8, 1, true, 0xFFFFFFFF, null);
		Paint.icon(d, "skip-forward", x + 35, cy - 4.5f, 9, Tokens.TEXT_2);
	}

	/** A thin rounded track, filled up to {@code fraction}, with a small white knob there. */
	private static void slider(Draw d, float x1, float x2, float cy, float fraction, Grad fill) {
		Hud.bar(d, x1, cy - 1.2f, x2, 2.4f, fraction, fill);
		float at = x1 + (x2 - x1) * Math.max(0, Math.min(1, fraction));
		Paint.circle(d, at, cy, 2.8f, 0xFFFFFFFF);
	}
}
