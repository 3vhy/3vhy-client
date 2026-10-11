package dev.threevhy.client.hud;

import dev.threevhy.client.Draw;
import dev.threevhy.client.gui.PixelIcons;
import dev.threevhy.client.module.Modules;
import dev.threevhy.client.music.MusicArt;
import dev.threevhy.client.music.MusicBridge;
import dev.threevhy.client.music.NowPlaying;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/**
 * The Music HUD: the song's cover, "now playing", title and artist, a badge in the music app's colour,
 * and a segmented progress bar between the time played and the time left.
 */
final class MusicWidget extends HudWidget {
	private static final int W = 176;
	private static final int PAD = 7;
	private static final int ART = 34;
	private static final int BADGE = 14;

	MusicWidget() {
		super("music", Modules.MUSIC, Anchor.TOP_CENTER);
	}

	private static boolean progress(NowPlaying song) {
		return Modules.MUSIC_PROGRESS.get() && (song == null || song.hasTimeline());
	}

	/** What to draw: the real song, or a sample in the hub so the widget can be placed. */
	private static NowPlaying song(boolean preview) {
		NowPlaying song = MusicBridge.current();
		if (song != null || !preview) return song;
		String title = MusicBridge.supported() ? "Nothing playing" : "Music HUD";
		String artist = MusicBridge.supported() ? "Play a song on Spotify, YouTube..." : "Works on Windows";
		return new NowPlaying("sample", "", title, artist, "", false, 42, 175, System.nanoTime(), null, -1, null, -1, "");
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

	@Override
	public int height(Minecraft mc) {
		return progress(MusicBridge.current()) ? 60 : 48;
	}

	@Override
	public void draw(Draw d, int x, int y, Minecraft mc, boolean preview) {
		NowPlaying song = song(preview);
		if (song == null) return;
		int h = progress(song) ? 60 : 48;
		if (backgroundAlpha() > 0) {
			if (rounded()) d.roundRect(x, y, x + W, y + h, 7, backgroundColor());
			else d.fill(x, y, x + W, y + h, backgroundColor());
		}
		boolean shadow = textShadow();
		int accent = song.appColor();

		int textX = x + PAD;
		if (Modules.MUSIC_ART.get()) {
			int ax = x + PAD;
			int ay = y + PAD;
			Identifier cover = MusicArt.COVER.texture(song.art(), song.artVersion());
			if (cover != null) {
				int[] size = MusicArt.COVER.size();
				d.texture(cover, ax, ay, ART, ART, size[0], size[1]);
			} else {
				box(d, ax, ay, ax + ART, ay + ART, 0xFF0E2159);
				PixelIcons.draw(d, PixelIcons.forModule(Modules.MUSIC), ax + (ART - 24) / 2, ay + (ART - 24) / 2, 2, Theme.SKY);
			}
			textX = ax + ART + PAD;
		}

		int bx = x + W - PAD - BADGE;
		int by = y + PAD - 1;
		Identifier icon = MusicArt.ICON.texture(song.icon(), song.iconVersion());
		if (icon != null) {
			int[] size = MusicArt.ICON.size();
			d.texture(icon, bx, by, BADGE, BADGE, size[0], size[1]);
		} else {
			badge(d, bx, by, accent, song.playing());
		}

		int textW = bx - 4 - textX;
		String state = song.playing() ? "NOW PLAYING" : "PAUSED";
		d.push();
		d.translate(textX, y + 8);
		d.scale(0.75f);
		d.text(trim(mc, state + "  ·  " + song.appName().toUpperCase(Locale.ROOT), Math.round(textW / 0.75f)), 0, 0,
				Theme.MUTED, shadow);
		d.pop();
		d.text(trim(mc, song.title().isBlank() ? "Unknown song" : song.title(), textW), textX, y + 18, Theme.ICE, shadow);
		d.text(trim(mc, song.artist(), textW), textX, y + 29, Theme.SKY, shadow);

		if (!progress(song)) return;
		double position = song.positionNow();
		String played = NowPlaying.time(position);
		String left = "-" + NowPlaying.time(song.duration() - position);
		int rowY = y + h - 13;
		d.text(played, x + PAD, rowY, Theme.ICE, shadow);
		d.text(left, x + W - PAD - textWidth(mc, left), rowY, Theme.ICE, shadow);
		int barX1 = x + PAD + textWidth(mc, played) + 6;
		int barX2 = x + W - PAD - textWidth(mc, left) - 6;
		double fraction = song.duration() > 0 ? Math.max(0, Math.min(1, position / song.duration())) : 0;
		bar(d, barX1, barX2, rowY + 4, fraction, accent, song.playing());
	}

	/** Until the app's icon arrives: a circle in the app's colour with three bars that bounce while music plays. */
	private static void badge(Draw d, int x, int y, int color, boolean playing) {
		d.roundRect(x, y, x + BADGE, y + BADGE, BADGE / 2, color);
		double time = System.currentTimeMillis() / 140.0;
		for (int i = 0; i < 3; i++) {
			int height = playing ? 3 + (int) Math.round(3 * Math.abs(Math.sin(time + i * 1.7))) : 3;
			int bx = x + 3 + i * 3;
			d.fill(bx, y + 10 - height, bx + 2, y + 10, 0xFF061D3C);
		}
	}

	/** Thin segments: played ones in the app's colour, rising near the playhead while the song plays. */
	private static void bar(Draw d, int x1, int x2, int centerY, double fraction, int color, boolean playing) {
		int segments = Math.max(1, (x2 - x1 + 1) / 3);
		int played = (int) Math.round(segments * fraction);
		double time = System.currentTimeMillis() / 120.0;
		for (int i = 0; i < segments; i++) {
			int sx = x1 + i * 3;
			int half = 2;
			int segColor = 0x607F9CCB;
			if (i < played) {
				segColor = color;
				int fromHead = played - 1 - i;
				if (playing && fromHead < 6) half = 2 + (int) Math.round(2 * Math.abs(Math.sin(time + i * 0.9)));
			}
			d.fill(sx, centerY - half, sx + 2, centerY + half, segColor);
		}
	}

	private static String trim(Minecraft mc, String text, int maxWidth) {
		if (maxWidth <= 0) return "";
		if (mc.font.width(text) <= maxWidth) return text;
		String cut = text;
		while (!cut.isEmpty() && mc.font.width(cut + "...") > maxWidth) cut = cut.substring(0, cut.length() - 1);
		return cut.stripTrailing() + "...";
	}
}
