package dev.eventide.client.ui;

import dev.eventide.client.Draw;
import dev.eventide.client.gui.UiFont;
import net.minecraft.network.chat.Component;

/**
 * The Eventide logo: a white trident wrapped in violet riptide rings, and the lowercase "eventide" wordmark
 * in Unbounded Bold. The mark is drawn from vector paths (assets/eventide/icons/brand.txt, made from
 * assets/brand/eventide-icon.svg without its tile, since in game it sits on glass), so it's sharp at any
 * size; the same design is the app icon and the website's logo.
 */
public final class Logo {
	public static final String WORD = "eventide";
	/** The wordmark's capital height at scale 1, its letter spacing (-0.02 em) and its em, in GUI pixels. */
	private static final float BASE_CAP = 6.8f;
	private static final float EM = 9.0667f;
	private static final float TRACKING = -0.02f * EM;
	/** How far below the drawing position a capital's top sits, at scale 1. */
	private static final float CAP_TOP = 0.2f;
	/** The x-height as a share of the capital height, for centring the word on the mark. */
	private static final float X_HEIGHT = 0.571f / 0.75f;
	private static final Component[] LETTERS = new Component[WORD.length()];

	private Logo() {
	}

	/** The mark in a {@code size} square at (x, y); {@code alpha} fades it, {@code nowMs} breathes its glow. */
	public static void mark(Draw d, float x, float y, float size, float alpha, long nowMs) {
		float old = Paint.opacity(Paint.opacity() * alpha);
		float breath = 0.85f + 0.15f * (float) Math.sin(nowMs % 4000 / 4000.0 * Math.PI * 2);
		Paint.glow(d, x + size * 0.5f, y + size * 0.5f, size * 0.24f, size * 0.55f, Paint.fade(Tokens.BRAND_SWIRL, 0.38f * breath));
		Paint.icon(d, "eventide-back", x, y, size, 1f, false, Paint.fade(Tokens.BRAND_SWIRL, 0.45f), null);
		Paint.icon(d, "eventide-trident", x, y, size, 1f, false, Tokens.BRAND_TRIDENT, null);
		Paint.icon(d, "eventide-front", x, y, size, 1f, false, Tokens.BRAND_SWIRL, null);
		Paint.opacity(old);
	}

	private static Component letter(int i) {
		if (LETTERS[i] == null) LETTERS[i] = UiFont.brand(String.valueOf(WORD.charAt(i)));
		return LETTERS[i];
	}

	/** The wordmark with capitals (and so the tall letters) {@code cap} tall from {@code capTop}; returns its width. */
	public static float wordmark(Draw d, float x, float capTop, float cap, int color) {
		float scale = cap / BASE_CAP;
		int faded = Paint.fade(color, Paint.opacity());
		boolean draw = d != null && ((faded >>> 24) & 255) >= 5;
		float cx = 0;
		if (draw) {
			d.push();
			d.translate(x, capTop - CAP_TOP * scale);
			d.scale(scale);
		}
		for (int i = 0; i < WORD.length(); i++) {
			if (draw) {
				d.push();
				d.translate(cx, 0);
				d.text(letter(i), 0, 0, faded, false);
				d.pop();
			}
			cx += UiFont.width(letter(i)) + (i < WORD.length() - 1 ? TRACKING : 0);
		}
		if (draw) d.pop();
		return cx * scale;
	}

	public static float wordmarkWidth(float cap) {
		return wordmark(null, 0, 0, cap, 0);
	}

	/** The mark and the wordmark side by side, the word's lowercase centred on the mark; returns the total width. */
	public static float lockup(Draw d, float x, float y, float markSize, float cap, long nowMs) {
		mark(d, x, y, markSize, 1, nowMs);
		float tx = x + markSize * 1.24f;
		float xHeight = cap * X_HEIGHT;
		float capTop = y + (markSize - xHeight) / 2f - (cap - xHeight);
		return tx - x + wordmark(d, tx, capTop, cap, Tokens.BRAND_WORDMARK);
	}
}
