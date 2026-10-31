package dev.eventide.client.ui;

import dev.eventide.client.Draw;
import dev.eventide.client.gui.Accent;
import dev.eventide.client.ui.Type.Weight;

/**
 * Eventide's controls: switches, module icons, round icon buttons, the accent fill, search field, key caps,
 * segmented pickers, sliders, panels and cards. They're flat and neutral: each surface is one lightness
 * step off the one under it, edges are a single faint 1px line, corners share one small radius (switches
 * and knobs are round), and the accent colour goes only on what's on or chosen. Nothing at rest has a
 * glow, a sheen or a gradient. Sizes are in layout units; callers scale the whole screen.
 * Animation state (hover, on/off) comes in as 0..1 values from {@link Anim}s the caller keeps, so nothing
 * here allocates state.
 */
public final class Widgets {
	/** Surfaces, one tone step apart: a control on a card on the sky. */
	private static final int CONTROL_FILL = 0x14FFFFFF;
	private static final int CONTROL_FILL_HOVER = 0x24FFFFFF;
	private static final int CONTROL_EDGE = 0x1FFFFFFF;
	private static final int CONTROL_EDGE_HOVER = 0x38FFFFFF;
	private static final int SWITCH_OFF = 0xFF3A3A40;
	private static final int PANEL = 0xFA131315;
	private static final int PANEL_EDGE = 0x17FFFFFF;
	private static final int CARD = 0xF2161618;
	private static final int CARD_HOVER = 0xF21E1E21;
	private static final int CARD_EDGE = 0x0FFFFFFF;
	private static final int SEGMENT_CHOSEN = 0xFF2E2E33;

	private Widgets() {
	}

	/** A switch, {@code w} x {@code h}: {@code knob} is how far the knob has slid and {@code lit} how far the track has turned to the accent. */
	public static void toggle(Draw d, float x, float y, float w, float h, float knob, float lit) {
		float r = h / 2f;
		Paint.rect(d, x, y, x + w, y + h, r, mixColor(SWITCH_OFF, Accent.get(), lit));
		float inset = h * 0.125f;
		float size = h - 2 * inset;
		float kx = x + inset + Math.max(0, Math.min(1, knob)) * (w - h);
		Paint.circle(d, kx + size / 2f, y + h / 2f, size / 2f, 0xFFFFFFFF);
	}

	/** A module's icon in a {@code size} box: grey when it's off, white when it's on, no tile behind it. */
	public static void iconTile(Draw d, float x, float y, float size, String icon, float on, float hover) {
		int color = mixColor(mixColor(Tokens.TEXT_3, Tokens.TEXT_2, hover), Tokens.TEXT, on);
		Paint.icon(d, icon, x, y, size, color);
	}

	/** A round icon button (the tab bar, a card's settings button): one tone step, lighter on hover. */
	public static void roundButton(Draw d, float cx, float cy, float radius, String icon, float hover, float iconScale) {
		Paint.circle(d, cx, cy, radius, mixColor(CONTROL_FILL, CONTROL_FILL_HOVER, hover));
		float size = radius * iconScale;
		Paint.icon(d, icon, cx - size / 2f, cy - size / 2f, size, mixColor(Tokens.TEXT_2, Tokens.TEXT, hover));
	}

	/** Text and icons on a {@link #primary} fill. */
	public static final int ON_PRIMARY = 0xFF0A0A0B;

	/**
	 * A primary button, or the chosen one of a few options: made primary by inversion, white with
	 * {@link #ON_PRIMARY} on it, so the accent stays on what's switched on. {@code hover} dims it a step.
	 */
	public static void primary(Draw d, float x1, float y1, float x2, float y2, float hover) {
		float r = Math.min(Tokens.R_CONTROL, (y2 - y1) / 2f);
		Paint.rect(d, x1, y1, x2, y2, r, hover > 0.75f ? 0xFFE4E4E7 : 0xFFF5F5F6);
	}

	/** A control (a button, a picker, a slot): a tone step with a 1px edge, both a step lighter on hover. */
	public static void control(Draw d, float x1, float y1, float x2, float y2, float r, float hover) {
		Paint.rect(d, x1, y1, x2, y2, r, mixColor(CONTROL_FILL, CONTROL_FILL_HOVER, hover));
		Paint.stroke(d, x1, y1, x2, y2, r, 1, mixColor(CONTROL_EDGE, CONTROL_EDGE_HOVER, hover));
	}

	/** A key cap: the label on a tone step with a 1px edge. */
	public static float kbd(Draw d, float x, float y, String label, float cap) {
		float padX = cap * 1.0f;
		float h = cap * 2.4f;
		float w = Type.width(label, cap, Weight.SEMIBOLD) + 2 * padX;
		float r = cap * 0.6f;
		Paint.rect(d, x, y, x + w, y + h, r, CONTROL_FILL);
		Paint.stroke(d, x, y, x + w, y + h, r, 1, CONTROL_EDGE);
		Type.draw(d, label, x + padX, y + (h - cap) / 2f - cap * 0.04f, cap, Weight.SEMIBOLD, Tokens.TEXT);
		return w;
	}

	public static float kbdWidth(String label, float cap) {
		return Type.width(label, cap, Weight.SEMIBOLD) + 2 * cap;
	}

	/** A small square key badge on a module card (C for Zoom), {@code size} tall. */
	public static float keyBadge(Draw d, float rightX, float y, float size, String label) {
		float cap = size * 0.34f;
		float w = Math.max(size, Type.width(label, cap, Weight.SEMIBOLD) + size * 0.6f);
		float x = rightX - w;
		float r = size * 0.27f;
		Paint.rect(d, x, y, x + w, y + size, r, Tokens.CONTROL);
		Paint.stroke(d, x, y, x + w, y + size, r, 1, Tokens.CONTROL_EDGE);
		Type.center(d, label, x + w / 2f, y + (size - cap) / 2f, cap, Weight.SEMIBOLD, Tokens.TEXT_2);
		return w;
	}

	/** The search field: a magnifier, the text (or placeholder) and a key hint at the end. */
	public static void search(Draw d, float x1, float y1, float x2, float y2, String text, String placeholder, String keyHint,
			boolean focused, float hover, long nowMs) {
		field(d, x1, y1, x2, y2, "search", text, placeholder, keyHint, focused, hover, nowMs);
	}

	/** A text field like the search field, with any icon at the start; focused, its edge takes the accent. */
	public static void field(Draw d, float x1, float y1, float x2, float y2, String icon, String text, String placeholder, String keyHint,
			boolean focused, float hover, long nowMs) {
		float h = y2 - y1;
		float r = Math.min(Tokens.R_CONTROL, h / 2f);
		Paint.rect(d, x1, y1, x2, y2, r, focused || hover > 0.5f ? CONTROL_FILL_HOVER : CONTROL_FILL);
		Paint.stroke(d, x1, y1, x2, y2, r, 1, focused ? Accent.get() : hover > 0.5f ? CONTROL_EDGE_HOVER : CONTROL_EDGE);
		float iconSize = h * 0.4f;
		float ix = x1 + h * 0.43f;
		Paint.icon(d, icon, ix, y1 + (h - iconSize) / 2f, iconSize, focused ? Tokens.TEXT : Tokens.TEXT_2);
		float cap = h * 0.24f;
		float tx = ix + iconSize + h * 0.27f;
		float badge = h * 0.5f;
		float badgeX = x2 - h * 0.39f;
		float maxW = badgeX - badge - tx - h * 0.2f;
		boolean empty = text.isEmpty();
		String shown = empty ? placeholder : text;
		while (!empty && shown.length() > 1 && Type.width(shown, cap, Weight.SEMIBOLD) > maxW) shown = shown.substring(1);
		Type.draw(d, Type.fit(shown, maxW, cap, Weight.SEMIBOLD), tx, y1 + (h - cap) / 2f, cap, Weight.SEMIBOLD, empty ? Tokens.TEXT_2 : Tokens.TEXT);
		if (focused && (nowMs / 530) % 2 == 0) {
			float cx = tx + (empty ? 0 : Type.width(shown, cap, Weight.SEMIBOLD) + cap * 0.15f);
			Paint.rect(d, cx, y1 + h * 0.3f, cx + Math.max(0.8f, cap * 0.1f), y2 - h * 0.3f, 0, Tokens.TEXT);
		}
		if (keyHint != null && (empty || !focused)) keyBadge(d, badgeX, y1 + (h - badge) / 2f, badge, keyHint);
	}

	/**
	 * A segmented picker: options as icons (or labels), the chosen one on a sliding raised step in white, the
	 * rest grey. {@code at} is the step's animated position (an option index, fractional mid-slide) and
	 * {@code hover} the option under the mouse or -1.
	 */
	public static void segmented(Draw d, float x1, float y1, float x2, float y2, String[] icons, String[] labels, float at, int hover) {
		int n = icons != null ? icons.length : labels.length;
		float h = y2 - y1;
		float r = Math.min(Tokens.R_CONTROL, h / 2f);
		Paint.rect(d, x1, y1, x2, y2, r, CONTROL_FILL);
		float pad = h * 0.1f;
		float segment = (x2 - x1 - 2 * pad) / n;
		float px = x1 + pad + at * segment;
		Paint.rect(d, px, y1 + pad, px + segment, y2 - pad, Math.max(2, r - pad), SEGMENT_CHOSEN);
		int nearest = Math.round(at);
		for (int i = 0; i < n; i++) {
			float cx = x1 + pad + segment * (i + 0.5f);
			boolean active = i == nearest;
			int color = active ? 0xFFFFFFFF : i == hover ? Tokens.TEXT : Tokens.TEXT_2;
			if (icons != null) {
				float size = h * 0.42f;
				Paint.icon(d, icons[i], cx - size / 2f, y1 + (h - size) / 2f, size, color);
			} else {
				float cap = h * 0.27f;
				Type.center(d, labels[i], cx, y1 + (h - cap) / 2f, cap, Weight.SEMIBOLD, color);
			}
		}
	}

	/** A slider: a thin track filled with the accent up to {@code fraction}, and a white knob that grows a little under the mouse. */
	public static void slider(Draw d, float x1, float cy, float x2, float fraction, float hover) {
		float t = 1.5f;
		Paint.rect(d, x1, cy - t, x2, cy + t, t, 0x1FFFFFFF);
		float fx = x1 + (x2 - x1) * Math.max(0, Math.min(1, fraction));
		if (fx > x1 + 0.5f) Paint.rect(d, x1, cy - t, fx, cy + t, t, Accent.get());
		float knob = 6f + hover;
		Paint.circle(d, fx, cy, knob, 0xFFFFFFFF);
	}

	/** A floating panel (a settings sheet, a toolbar): opaque, a 1px edge, and the one shadow it earns by floating. */
	public static void glass(Draw d, float x1, float y1, float x2, float y2, float r) {
		Paint.shadow(d, x1, y1, x2, y2, r, 0, 16, 40, -16, 0x99000000);
		Paint.rect(d, x1, y1, x2, y2, r, PANEL);
		Paint.stroke(d, x1, y1, x2, y2, r, 1, PANEL_EDGE);
	}

	/** A module card's body: one tone step off the sky with a 1px edge; {@code hover} lightens it a step. */
	public static void card(Draw d, float x1, float y1, float x2, float y2, float r, float hover) {
		Paint.rect(d, x1, y1, x2, y2, r, mixColor(CARD, CARD_HOVER, hover));
		Paint.stroke(d, x1, y1, x2, y2, r, 1, CARD_EDGE);
	}

	/** A small status dot (a connection that's up). */
	public static void pulseDot(Draw d, float cx, float cy, float r, int color, long nowMs) {
		Paint.circle(d, cx, cy, r, color);
	}

	/** Blends two colours, alpha included. */
	public static int mixColor(int a, int b, float t) {
		return Draw.mix(a, b, Math.max(0, Math.min(1, t)));
	}

	private static int mixAlpha(int a, int b, float t) {
		return mixColor(a, b, t);
	}
}
