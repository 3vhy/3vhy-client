package dev.threevhy.client.hud;

import dev.threevhy.client.Draw;
import dev.threevhy.client.Hooks;
import dev.threevhy.client.gui.Accent;
import dev.threevhy.client.module.Modules;
import dev.threevhy.client.music.Lyrics;
import dev.threevhy.client.music.MusicBridge;
import dev.threevhy.client.music.NowPlaying;
import dev.threevhy.client.ui.Ease;
import dev.threevhy.client.ui.Paint;
import dev.threevhy.client.ui.Tokens;
import dev.threevhy.client.ui.Type;
import dev.threevhy.client.ui.Type.Weight;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

/**
 * Sky Lyrics: the words of the song playing, line by line as they're sung. In the sky (the default)
 * each new line is written high up in front of you and stays there in the world as you turn, glowing
 * violet, with the next line faint under it; on screen they sit at the top like a HUD widget instead.
 */
final class LyricsWidget extends HudWidget {
	private static final float WIDTH = 260;
	private static final float ELEVATION = 15;
	private static final long FADE_MS = 520;

	private int shownIndex = -2;
	private String current = "";
	private String previous = "";
	private long changedAt = 0;
	private float anchorYaw = 0;
	private double lineAt = 0;
	private double lineEnd = 0;

	LyricsWidget() {
		super("lyrics", Modules.LYRICS, Anchor.TOP_CENTER);
	}

	private static boolean sky() {
		return Modules.LYRICS_PLACE.is("Sky");
	}

	private static float cap() {
		return (float) (10 * Modules.LYRICS_SIZE.get() / 100.0);
	}

	@Override
	public boolean shown() {
		return super.shown() && !sky();
	}

	@Override
	public boolean visible(Minecraft mc) {
		return !current.isEmpty() || !previous.isEmpty();
	}

	@Override
	public int width(Minecraft mc) {
		return Math.round(WIDTH);
	}

	@Override
	public int height(Minecraft mc) {
		return Math.round(cap() * 2 * 1.5f + cap() * 1.4f + 8);
	}

	/** Follows the song: fetches its lyrics and notices when the next line starts. */
	private void update(Minecraft mc) {
		NowPlaying song = MusicBridge.current();
		if (song == null) {
			current = "";
			previous = "";
			shownIndex = -2;
			return;
		}
		Lyrics.want(song);
		List<Lyrics.Line> lines = Lyrics.lines(song);
		int index = Lyrics.index(lines, song.positionNow() + 0.12);
		if (index == shownIndex) return;
		shownIndex = index;
		lineAt = index < 0 ? 0 : lines.get(index).at();
		lineEnd = index + 1 < lines.size() ? lines.get(index + 1).at() : lineAt + 4;
		previous = current;
		current = index < 0 ? "" : lines.get(index).text();
		changedAt = System.currentTimeMillis();
		Camera camera = mc.gameRenderer.getMainCamera();
		anchorYaw = camera.yRot();
	}

	private String next() {
		NowPlaying song = MusicBridge.current();
		List<Lyrics.Line> lines = Lyrics.lines(song);
		for (int i = shownIndex + 1; i < lines.size(); i++) {
			if (!lines.get(i).text().isBlank()) return lines.get(i).text();
		}
		return "";
	}

	@Override
	public void draw(Draw d, int x, int y, Minecraft mc, boolean preview) {
		update(mc);
		String line = current;
		if (preview && line.isEmpty() && previous.isEmpty()) line = "Lyrics show here as the song plays";
		drawLines(d, x + WIDTH / 2f, y + 4, line, preview ? "" : next(), 1);
	}

	/** In the sky: projects the line's spot above the horizon onto the screen, if it's in view. */
	void drawSky(Draw d, Minecraft mc) {
		if (!Modules.LYRICS.enabled() || !sky() || mc.player == null) return;
		update(mc);
		if (current.isEmpty() && previous.isEmpty()) return;
		Camera camera = mc.gameRenderer.getMainCamera();
		float yaw = Mth.wrapDegrees(anchorYaw - camera.yRot());
		float up = ELEVATION + camera.xRot();
		if (Math.abs(yaw) > 75 || up > 80 || up < -80) return;
		float fov = (float) Math.toRadians(mc.options.fov().get());
		float halfH = (float) Math.tan(fov / 2);
		float w = d.width();
		float h = d.height();
		float aspect = w / Math.max(1, h);
		float sx = w / 2f + (float) Math.tan(Math.toRadians(yaw)) / (halfH * aspect) * w / 2f;
		float sy = h / 2f - (float) Math.tan(Math.toRadians(up)) / halfH * h / 2f;
		float edge = Math.min(1, Math.max(0, 1.4f - Math.abs(yaw) / 55f));
		float old = Paint.opacity(Paint.opacity() * edge);
		drawLines(d, sx, sy - cap() * 1.5f, current, next(), 1.25f);
		Paint.opacity(old);
		Hooks.lyricsDrawn = true;
	}

	/** How much of the current line has been sung, 0..1, for the karaoke fill. */
	private float sung() {
		NowPlaying song = MusicBridge.current();
		if (song == null || lineEnd <= lineAt) return 1;
		return (float) Math.max(0, Math.min(1, (song.positionNow() - lineAt) / Math.min(lineEnd - lineAt, 8)));
	}

	private void drawLines(Draw d, float cx, float top, String line, String next, float glow) {
		long now = System.currentTimeMillis();
		float t = Ease.OUT.apply((now - changedAt) / (double) FADE_MS);
		float cap = cap();
		if (!previous.isEmpty() && t < 1) {
			float old = Paint.opacity(Paint.opacity() * (1 - t));
			block(d, cx, top - 8 * t, previous, cap, glow, 1);
			Paint.opacity(old);
		}
		if (!line.isEmpty()) {
			float old = Paint.opacity(Paint.opacity() * t);
			float h = block(d, cx, top + 8 * (1 - t), line, cap, glow, line.equals(current) ? sung() : 1);
			Paint.opacity(old);
			if (Modules.LYRICS_NEXT.get() && !next.isEmpty()) {
				float small = cap * 0.62f;
				float nold = Paint.opacity(Paint.opacity() * t * 0.75f);
				String shown = Type.fit(next, WIDTH, small, Weight.SEMIBOLD);
				Hud.text(d, shown, cx - Type.width(shown, small, Weight.SEMIBOLD) / 2f, top + h + small * 0.9f, small, Weight.SEMIBOLD, Tokens.TEXT_2,
						true);
				Paint.opacity(nold);
			}
		}
	}

	/**
	 * One sung line, wrapped to two rows at most, with a violet glow: white as far as it's been sung
	 * ({@code sung}, 0..1, like karaoke) and faint after that. Returns its height.
	 */
	private static float block(Draw d, float cx, float top, String text, float cap, float glow, float sung) {
		List<String> rows = wrap(text, WIDTH, cap);
		float lineH = cap * 1.5f;
		float total = 0;
		for (String row : rows) total += Type.width(row, cap, Weight.BOLD);
		float lit = total * sung;
		float y = top;
		for (String row : rows) {
			float w = Type.width(row, cap, Weight.BOLD);
			float x = cx - w / 2f;
			Paint.shadow(d, x - cap * 0.2f, y, x + w + cap * 0.2f, y + cap, cap * 0.5f, 0, 0, cap * 1.6f, cap * 0.2f, 0x590E0820);
			Paint.shadow(d, x - cap * 0.3f, y - cap * 0.1f, x + w + cap * 0.3f, y + cap * 1.1f, cap * 0.6f, 0, 0, cap * 2.4f * glow, 0,
					Accent.glow(0.5f));
			Type.draw(d, row, x + cap * 0.07f, y + cap * 0.09f, cap, Weight.BOLD, 0xB3140A30);
			float here = Math.max(0, Math.min(w, lit));
			if (here < w) Type.draw(d, row, x, y, cap, Weight.BOLD, 0xB8D9CCFF);
			if (here >= w) {
				Type.draw(d, row, x, y, cap, Weight.BOLD, 0xFFFFFFFF);
			} else if (here > 0) {
				d.enableScissor((int) Math.floor(x - cap), (int) Math.floor(y - cap), (int) Math.ceil(x + here), (int) Math.ceil(y + cap * 2));
				Type.draw(d, row, x, y, cap, Weight.BOLD, 0xFFFFFFFF);
				d.disableScissor();
			}
			lit -= w;
			y += lineH;
		}
		return y - top - (lineH - cap);
	}

	private static List<String> wrap(String text, float width, float cap) {
		List<String> rows = new ArrayList<>();
		StringBuilder row = new StringBuilder();
		for (String word : text.split(" ")) {
			String next = row.isEmpty() ? word : row + " " + word;
			if (row.isEmpty() || Type.width(next, cap, Weight.BOLD) <= width) {
				row = new StringBuilder(next);
			} else {
				rows.add(row.toString());
				row = new StringBuilder(word);
			}
		}
		if (!row.isEmpty()) rows.add(row.toString());
		if (rows.size() > 2) rows = List.of(rows.get(0), String.join(" ", rows.subList(1, rows.size())));
		List<String> fitted = new ArrayList<>();
		for (String each : rows) fitted.add(Type.fit(each, width, cap, Weight.BOLD));
		return fitted;
	}
}
