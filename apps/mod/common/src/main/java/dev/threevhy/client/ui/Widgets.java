package dev.threevhy.client.ui;

import dev.threevhy.client.Draw;
import dev.threevhy.client.gui.Accent;
import dev.threevhy.client.ui.Type.Weight;

/**
 * 3vhy's controls, drawn the way the website draws them: switches, icon tiles, round icon buttons, the
 * gradient pill, search field, key caps, segmented pickers, sliders and glass panels. Sizes are in
 * layout units (one CSS pixel on the site); callers scale the whole screen. Animation state (hover,
 * on/off) comes in as 0..1 values from {@link Anim}s the caller keeps, so nothing here allocates state.
 */
public final class Widgets {
	private Widgets() {
	}

	/**
	 * A switch, {@code w} x {@code h} (the site's is 40 x 24): {@code knob} is how far the knob has slid
	 * (it overshoots a little on its spring) and {@code lit} how far the track has turned to the accent.
	 */
	public static void toggle(Draw d, float x, float y, float w, float h, float knob, float lit) {
		float r = h / 2f;
		if (lit > 0.01f) Paint.shadow(d, x, y, x + w, y + h, r, 0, 0, 16, -4, Accent.glow(0.9f * lit));
		Paint.rect(d, x, y, x + w, y + h, r, Tokens.SWITCH_OFF);
		if (lit > 0.01f) {
			float old = Paint.opacity(Paint.opacity() * lit);
			Paint.rect(d, x, y, x + w, y + h, r, Accent.toggle());
			Paint.opacity(old);
		}
		float inset = h * 0.125f;
		float size = h - 2 * inset;
		float kx = x + inset + knob * (w - h);
		Paint.shadow(d, kx, y + inset, kx + size, y + inset + size, size / 2f, 0, size * 0.11f, size / 3f, 0, 0x73000000);
		Paint.circle(d, kx + size / 2f, y + h / 2f, size / 2f, 0xFFFFFFFF);
	}

	/** A module's icon tile: the accent gradient with a glow when it's on, quiet glass when it's off. */
	public static void iconTile(Draw d, float x, float y, float size, String icon, float on, float hover) {
		float r = size * Tokens.R_TILE / 42f;
		if (on > 0.01f) Paint.shadow(d, x, y, x + size, y + size, r, 0, size * 0.24f, size * 0.52f, -size * 0.24f, Accent.glow(0.9f * on));
		if (on < 0.99f) {
			float old = Paint.opacity(Paint.opacity() * (1 - on));
			Paint.rect(d, x, y, x + size, y + size, r, hover > 0 ? mixAlpha(Tokens.TILE_OFF, 0x1AFFFFFF, hover) : Tokens.TILE_OFF);
			Paint.stroke(d, x, y, x + size, y + size, r, 1, Tokens.TILE_OFF);
			Paint.opacity(old);
		}
		if (on > 0.01f) {
			float old = Paint.opacity(Paint.opacity() * on);
			Paint.rect(d, x, y, x + size, y + size, r, Accent.tile());
			Paint.stroke(d, x, y, x + size, y + size, r, 1, Grad.down(0x4DFFFFFF, 0x00FFFFFF));
			Paint.opacity(old);
		}
		float iconSize = size * 0.476f;
		int color = mixColor(mixColor(Tokens.TEXT_2, Tokens.TEXT, hover * 0.6f), 0xFFFFFFFF, on);
		Paint.icon(d, icon, x + (size - iconSize) / 2f, y + (size - iconSize) / 2f, iconSize, color);
	}

	/** A round icon button (the tab bar, a card's settings button): glass with a hairline edge. */
	public static void roundButton(Draw d, float cx, float cy, float radius, String icon, float hover, float iconScale) {
		Paint.circle(d, cx, cy, radius, mixColor(Tokens.CONTROL, Tokens.CONTROL_HOVER, hover));
		Paint.ring(d, cx, cy, radius, 1, mixColor(Tokens.CONTROL_EDGE, 0x33FFFFFF, hover));
		float size = radius * iconScale;
		Paint.icon(d, icon, cx - size / 2f, cy - size / 2f, size, mixColor(Tokens.TEXT_2, Tokens.TEXT, hover));
	}

	/** The accent pill: the active tab, a primary button. A soft glow underneath, a sheen on top. */
	public static void accentPill(Draw d, float x1, float y1, float x2, float y2, float glow) {
		float r = (y2 - y1) / 2f;
		if (glow > 0.01f) Paint.shadow(d, x1, y1, x2, y2, r, 0, (y2 - y1) * 0.26f, (y2 - y1) * 0.7f, -(y2 - y1) * 0.26f, Accent.glow(0.95f * glow));
		Paint.rect(d, x1, y1, x2, y2, r, Accent.primary());
		Paint.stroke(d, x1, y1, x2, y2, r, 1, Grad.down(0x47FFFFFF, 0x00FFFFFF));
	}

	/** A key cap like the site's {@code kbd}: label centred, a thicker edge along the bottom. */
	public static float kbd(Draw d, float x, float y, String label, float cap) {
		float padX = cap * 1.0f;
		float h = cap * 2.4f;
		float w = Type.width(label, cap, Weight.SEMIBOLD) + 2 * padX;
		float r = cap * 0.85f;
		Paint.rect(d, x, y, x + w, y + h, r, 0x24FFFFFF);
		Paint.rect(d, x, y, x + w, y + h - Math.max(1, cap * 0.12f), r, 0x14FFFFFF);
		Paint.stroke(d, x, y, x + w, y + h, r, 1, 0x24FFFFFF);
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

	/** The search field: a glass pill with a magnifier, the text (or placeholder) and a key hint at the end. */
	public static void search(Draw d, float x1, float y1, float x2, float y2, String text, String placeholder, String keyHint,
			boolean focused, float hover, long nowMs) {
		field(d, x1, y1, x2, y2, "search", text, placeholder, keyHint, focused, hover, nowMs);
	}

	/** A text field like the search field, with any icon at the start. */
	public static void field(Draw d, float x1, float y1, float x2, float y2, String icon, String text, String placeholder, String keyHint,
			boolean focused, float hover, long nowMs) {
		float h = y2 - y1;
		float r = h / 2f;
		Paint.rect(d, x1, y1, x2, y2, r, mixColor(0x0AFFFFFF, 0x12FFFFFF, Math.max(hover, focused ? 1 : 0)));
		Paint.stroke(d, x1, y1, x2, y2, r, 1, focused ? 0x52CDB6FF : mixColor(Tokens.CONTROL_EDGE, 0x30FFFFFF, hover));
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
			Paint.rect(d, cx, y1 + h * 0.3f, cx + Math.max(0.8f, cap * 0.1f), y2 - h * 0.3f, 0, Tokens.LILAC);
		}
		if (keyHint != null && (empty || !focused)) keyBadge(d, badgeX, y1 + (h - badge) / 2f, badge, keyHint);
	}

	/**
	 * A segmented picker like the main menu's panel: options as icons (or labels), the chosen one on a
	 * sliding accent pill. {@code at} is the pill's animated position (an option index, fractional
	 * mid-slide) and {@code hover} the option under the mouse or -1.
	 */
	public static void segmented(Draw d, float x1, float y1, float x2, float y2, String[] icons, String[] labels, float at, int hover) {
		int n = icons != null ? icons.length : labels.length;
		float h = y2 - y1;
		float r = h / 2f;
		Paint.rect(d, x1, y1, x2, y2, r, 0x14FFFFFF);
		float pad = h * 0.1f;
		float segment = (x2 - x1 - 2 * pad) / n;
		float px = x1 + pad + at * segment;
		accentPill(d, px, y1 + pad, px + segment, y2 - pad, 1);
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

	/** A slider: a thin track filled with the accent gradient up to {@code fraction}, and a white knob. */
	public static void slider(Draw d, float x1, float cy, float x2, float fraction, float hover) {
		float t = 2f;
		Paint.rect(d, x1, cy - t, x2, cy + t, t, 0x1FFFFFFF);
		float fx = x1 + (x2 - x1) * Math.max(0, Math.min(1, fraction));
		if (fx > x1 + 0.5f) {
			Paint.shadow(d, x1, cy - t, fx, cy + t, t, 0, 0, 10, 0, Accent.glow(0.5f));
			Paint.rect(d, x1, cy - t, fx, cy + t, t, Accent.bar());
		}
		float knob = 7f + hover;
		Paint.shadow(d, fx - knob, cy - knob, fx + knob, cy + knob, knob, 0, 1.5f, 5, 0, 0x73000000);
		Paint.circle(d, fx, cy, knob, 0xFFFFFFFF);
		Paint.ring(d, fx, cy, knob, 1.6f, Accent.get());
	}

	/** A floating glass panel: deep violet glass, a lilac hairline and a long soft shadow. */
	public static void glass(Draw d, float x1, float y1, float x2, float y2, float r) {
		Paint.shadow(d, x1, y1, x2, y2, r, 0, 26, 60, -24, 0xF2000000);
		Paint.rect(d, x1, y1, x2, y2, r, 0xD6141026);
		Paint.stroke(d, x1, y1, x2, y2, r, 1, 0x29CDB6FF);
		Paint.stroke(d, x1 + 1, y1 + 1, x2 - 1, y2 - 1, r - 1, 1, Grad.down(0x0FFFFFFF, 0x00FFFFFF));
	}

	/** A module card's body: a soft violet shade with a lit top edge; {@code hover} brightens the edge. */
	public static void card(Draw d, float x1, float y1, float x2, float y2, float r, float hover) {
		if (hover > 0.01f) Paint.shadow(d, x1, y1, x2, y2, r, 0, 18, 40, -20, Paint.fade(0xCC000000, hover));
		Paint.rect(d, x1, y1, x2, y2, r, Grad.down(Tokens.CARD_TOP, Tokens.CARD_BOTTOM));
		Paint.stroke(d, x1, y1, x2, y2, r, 1, Grad.down(mixColor(0x33D6BEFF, 0x66D6BEFF, hover), mixColor(Tokens.CARD_EDGE, 0x1FFFFFFF, hover)));
	}

	/** A small rounded label like the site's eyebrow ("Combat"): lilac text on a faint violet pill. */
	public static float eyebrow(Draw d, float x, float y, String icon, String label, float cap) {
		float h = cap * 3.1f;
		float iconSize = cap * 1.6f;
		float textW = Type.width(label, cap, Weight.SEMIBOLD);
		float w = cap * 1.2f + (icon != null ? iconSize + cap * 0.85f : 0) + textW + cap * 1.5f;
		Paint.rect(d, x, y, x + w, y + h, h / 2f, 0x1A880FF9);
		Paint.stroke(d, x, y, x + w, y + h, h / 2f, 1, 0x29CDB6FF);
		float cx = x + cap * 1.2f;
		if (icon != null) {
			Paint.icon(d, icon, cx, y + (h - iconSize) / 2f, iconSize, Tokens.LILAC);
			cx += iconSize + cap * 0.85f;
		}
		Type.draw(d, label, cx, y + (h - cap) / 2f, cap, Weight.SEMIBOLD, Tokens.LILAC);
		return w;
	}

	/** A soft dot that pulses, like the site's status dot. */
	public static void pulseDot(Draw d, float cx, float cy, float r, int color, long nowMs) {
		float t = (nowMs % 2600) / 2600f;
		float ring = t < 0.6f ? t / 0.6f : 1;
		Paint.ring(d, cx, cy, r + ring * r * 1.7f, r * 0.5f, Paint.fade(color, 0.5f * (1 - ring)));
		Paint.glow(d, cx, cy, r, r * 2.5f, Paint.fade(color, 0.6f));
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
