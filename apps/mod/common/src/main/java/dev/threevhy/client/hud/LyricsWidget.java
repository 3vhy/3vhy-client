package dev.threevhy.client.hud;

import dev.threevhy.client.Draw;
import dev.threevhy.client.Hooks;
import dev.threevhy.client.gui.Accent;
import dev.threevhy.client.module.Modules;
import dev.threevhy.client.music.MusicBridge;
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
	private final LyricTracker lyrics = new LyricTracker();
	private float anchorYaw = 0;

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
		return !lyrics.current().isEmpty() || !lyrics.previous().isEmpty();
	}

	@Override
	public int width(Minecraft mc) {
		return Math.round(WIDTH);
	}

	@Override
	public int height(Minecraft mc) {
		return Math.round(cap() * 2 * 1.5f + cap() * 1.4f + 8);
	}

	/** Follows the song; a new line is written where you're looking now. */
	private void update(Minecraft mc) {
		if (lyrics.update(MusicBridge.current())) anchorYaw = mc.gameRenderer.getMainCamera().yRot();
	}

	@Override
	public void draw(Draw d, int x, int y, Minecraft mc, boolean preview) {
		update(mc);
		String line = lyrics.current();
		if (preview && line.isEmpty() && lyrics.previous().isEmpty()) line = "Lyrics show here as the song plays";
		drawLines(d, x + WIDTH / 2f, y + 4, line, preview ? "" : lyrics.next(), 1);
	}

	/** In the sky: projects the line's spot above the horizon onto the screen, if it's in view. */
	void drawSky(Draw d, Minecraft mc) {
		if (!Modules.LYRICS.enabled() || !sky() || mc.player == null) return;
		update(mc);
		if (lyrics.current().isEmpty() && lyrics.previous().isEmpty()) return;
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
		drawLines(d, sx, sy - cap() * 1.5f, lyrics.current(), lyrics.next(), 1.25f);
		Paint.opacity(old);
		Hooks.lyricsDrawn = true;
	}

	private void drawLines(Draw d, float cx, float top, String line, String next, float glow) {
		float t = lyrics.change();
		float cap = cap();
		if (!lyrics.previous().isEmpty() && t < 1) {
			float old = Paint.opacity(Paint.opacity() * (1 - t));
			block(d, cx, top - 8 * t, lyrics.previous(), cap, glow, 1);
			Paint.opacity(old);
		}
		if (!line.isEmpty()) {
			float old = Paint.opacity(Paint.opacity() * t);
			float h = block(d, cx, top + 8 * (1 - t), line, cap, glow, line.equals(lyrics.current()) ? lyrics.sung(MusicBridge.current()) : 1);
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
