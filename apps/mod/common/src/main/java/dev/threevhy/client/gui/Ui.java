package dev.threevhy.client.gui;

import dev.threevhy.client.Draw;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Drawing helpers shared by the hub and the mod menus: Lunar-style smoked glass with thin edges and
 * 3vhy's accent, with round corners drawn smooth by {@link Smooth} so they stay clean at any scale.
 * Every control records a click area; the screen runs the topmost one hit.
 */
public final class Ui {
	/** A clickable area. Areas added later are on top. */
	public record Hit(int x1, int y1, int x2, int y2, Runnable action, Runnable rightAction) {
		public boolean contains(double x, double y) {
			return x >= x1 && x < x2 && y >= y1 && y < y2;
		}
	}

	/** Corner radius of the big glass panels. */
	public static final int R_PANEL = 6;
	/** Corner radius of cards and list rows. */
	public static final int R_CARD = 4;
	/** Corner radius of buttons, fields and other small controls. */
	public static final int R_CONTROL = 4;

	public final Draw d;
	public final MenuTheme t;
	public final Minecraft mc = Minecraft.getInstance();
	public final int mouseX;
	public final int mouseY;
	private final List<Hit> hits;
	/** Clip rectangle for clicks (scrolling lists), or null. */
	private int[] clip = null;
	/** Whether text goes in the clean font ({@link UiFont}) instead of Minecraft's. */
	private boolean clean = false;
	/** How big text() and title() draw, and width() measures. */
	private float textScale = 1f;

	public Ui(Draw d, int mouseX, int mouseY, List<Hit> hits) {
		this.d = d;
		this.t = MenuTheme.current();
		this.mouseX = mouseX;
		this.mouseY = mouseY;
		this.hits = hits;
	}

	/** Draws this frame's text in the clean font ({@link UiFont}) when {@code on}. */
	public Ui cleanFont(boolean on) {
		clean = on;
		return this;
	}

	/** Draws (and measures) text() and title() at {@code scale}. */
	public Ui textScale(float scale) {
		textScale = scale;
		return this;
	}

	public float textScale() {
		return textScale;
	}

	public static List<Hit> newHits() {
		return new ArrayList<>();
	}

	public boolean hover(int x1, int y1, int x2, int y2) {
		if (clip != null && (mouseX < clip[0] || mouseX >= clip[2] || mouseY < clip[1] || mouseY >= clip[3])) return false;
		return mouseX >= x1 && mouseX < x2 && mouseY >= y1 && mouseY < y2;
	}

	public void hit(int x1, int y1, int x2, int y2, Runnable action) {
		hit(x1, y1, x2, y2, action, null);
	}

	public void hit(int x1, int y1, int x2, int y2, Runnable action, Runnable rightAction) {
		if (clip != null) {
			x1 = Math.max(x1, clip[0]);
			y1 = Math.max(y1, clip[1]);
			x2 = Math.min(x2, clip[2]);
			y2 = Math.min(y2, clip[3]);
			if (x2 <= x1 || y2 <= y1) return;
		}
		hits.add(new Hit(x1, y1, x2, y2, action, rightAction));
	}

	/** Limits drawing and clicks to a rectangle until endClip(). */
	public void clip(int x1, int y1, int x2, int y2) {
		clip = new int[] {x1, y1, x2, y2};
		d.enableScissor(x1, y1, x2, y2);
	}

	public void endClip() {
		clip = null;
		d.disableScissor();
	}

	// Text

	public int width(String text) {
		return Math.round((clean ? mc.font.width(UiFont.semibold(text)) : mc.font.width(text)) * textScale);
	}

	public static Component bold(String text) {
		return Component.literal(text).withStyle(ChatFormatting.BOLD);
	}

	public int boldWidth(String text) {
		return Math.round(mc.font.width(clean ? UiFont.bold(text) : bold(text)) * textScale);
	}

	/** Bold text in the font this Ui draws with. */
	private Component boldText(String text) {
		return clean ? UiFont.bold(text) : bold(text);
	}

	/**
	 * Text with extra room between the letters, like Lunar's uppercase labels, at {@code scale}.
	 * Returns how wide it was.
	 */
	public float spaced(String text, float x, float y, float scale, float spacing, boolean bold, int color) {
		float cx = x;
		for (int i = 0; i < text.length(); i++) {
			String letter = String.valueOf(text.charAt(i));
			Component part = bold ? boldText(letter) : clean ? UiFont.semibold(letter) : Component.literal(letter);
			d.push();
			d.translate(cx, y);
			d.scale(scale);
			d.text(part, 0, 0, color, false);
			d.pop();
			cx += mc.font.width(part) * scale + spacing;
		}
		return text.isEmpty() ? 0 : cx - spacing - x;
	}

	public float spacedWidth(String text, float scale, float spacing, boolean bold) {
		float w = 0;
		for (int i = 0; i < text.length(); i++) {
			String letter = String.valueOf(text.charAt(i));
			Component part = bold ? boldText(letter) : clean ? UiFont.semibold(letter) : Component.literal(letter);
			w += mc.font.width(part) * scale + spacing;
		}
		return text.isEmpty() ? 0 : w - spacing;
	}

	/** A smooth filled box with round corners. */
	public void round(float x1, float y1, float x2, float y2, float radius, int color) {
		Smooth.roundRect(d, x1, y1, x2, y2, radius, color);
	}

	/** A smooth one-unit edge with round corners. */
	public void ring(float x1, float y1, float x2, float y2, float radius, int color) {
		Smooth.roundOutline(d, x1, y1, x2, y2, radius, 1, color);
	}

	public void text(String text, int x, int y, int color) {
		scaled(clean ? UiFont.semibold(text) : Component.literal(text), x, y, color);
	}

	/** Uppercase bold, like NoRisk's titles. */
	public void title(String text, int x, int y, int color) {
		String upper = text.toUpperCase(Locale.ROOT);
		scaled(clean ? UiFont.bold(upper) : bold(upper), x, y, color);
	}

	private void scaled(Component text, float x, float y, int color) {
		if (textScale == 1f) {
			d.text(text, Math.round(x), Math.round(y), color, false);
			return;
		}
		d.push();
		d.translate(x, y);
		d.scale(textScale);
		d.text(text, 0, 0, color, false);
		d.pop();
	}

	public int titleWidth(String text) {
		return boldWidth(text.toUpperCase(Locale.ROOT));
	}

	/** Text drawn smaller, for descriptions and labels. */
	public void small(String text, float x, float y, float scale, int color) {
		d.push();
		d.translate(x, y);
		d.scale(scale);
		if (clean) d.text(UiFont.semibold(text), 0, 0, color, false);
		else d.text(text, 0, 0, color, false);
		d.pop();
	}

	public String trim(String text, int maxWidth) {
		if (maxWidth <= 0) return "";
		if (width(text) <= maxWidth) return text;
		String ellipsis = "...";
		String cut = text;
		while (!cut.isEmpty() && width(cut + ellipsis) > maxWidth) cut = cut.substring(0, cut.length() - 1);
		return cut.stripTrailing() + ellipsis;
	}

	public String trimTitle(String text, int maxWidth) {
		String upper = text.toUpperCase(Locale.ROOT);
		if (boldWidth(upper) <= maxWidth) return upper;
		String cut = upper;
		while (!cut.isEmpty() && boldWidth(cut + "...") > maxWidth) cut = cut.substring(0, cut.length() - 1);
		return cut.stripTrailing() + "...";
	}

	/** Splits text into at most maxLines lines that fit maxWidth (at the given scale), the last one trimmed. */
	public List<String> wrap(String text, int maxWidth, float scale, int maxLines) {
		int width = Math.round(maxWidth / scale);
		List<String> lines = new ArrayList<>();
		StringBuilder line = new StringBuilder();
		String[] words = text.split(" ");
		for (int i = 0; i < words.length; i++) {
			String next = line.isEmpty() ? words[i] : line + " " + words[i];
			if (width(next) <= width || line.isEmpty()) {
				line = new StringBuilder(next);
				continue;
			}
			if (lines.size() == maxLines - 1) {
				StringBuilder rest = new StringBuilder(line);
				for (int j = i; j < words.length; j++) rest.append(' ').append(words[j]);
				lines.add(trim(rest.toString(), width));
				return lines;
			}
			lines.add(line.toString());
			line = new StringBuilder(words[i]);
		}
		if (!line.isEmpty()) lines.add(trim(line.toString(), width));
		return lines;
	}

	/** A glass panel with a thin edge and soft round corners, like the launcher's cards. */
	public void panel(int x1, int y1, int x2, int y2, int fill) {
		round(x1, y1, x2, y2, R_PANEL, fill);
		ring(x1, y1, x2, y2, R_PANEL, t.edge());
	}

	/** A card or list row: rounded, with a thin edge. */
	public void card(int x1, int y1, int x2, int y2, int fill, int edge) {
		round(x1, y1, x2, y2, R_CARD, fill);
		ring(x1, y1, x2, y2, R_CARD, edge);
	}

	/**
	 * A rounded button. The primary one is the launcher's Play button: a cyan face shaded top to
	 * bottom, sitting on a darker edge, so it looks pressable. The others are glass with an edge.
	 */
	public void button(int x, int y, int w, int h, String label, boolean primary, Runnable action) {
		boolean hover = hover(x, y, x + w, y + h);
		String text = trimTitle(label, w - 8);
		if (primary) {
			int face = hover ? Draw.mix(t.accent(), 0xFFFFFFFF, 0.12) : t.accent();
			round(x, y, x + w, y + h, R_CONTROL, Draw.mix(t.accent(), 0xFF000000, 0.45));
			round(x, y, x + w, y + h - 1, R_CONTROL, face);
			d.text(boldText(text), x + (w - boldWidth(text)) / 2, y + (h - 1 - 8) / 2 + 1, t.onAccent(), false);
		} else {
			card(x, y, x + w, y + h, hover ? t.cardHover() : t.card(), hover ? t.edgeStrong() : t.edge());
			d.text(boldText(text), x + (w - boldWidth(text)) / 2, y + (h - 8) / 2, t.text(), false);
		}
		hit(x, y, x + w, y + h, action);
	}

	/** A small dark label centred under (cx, y). */
	public void tooltip(String text, int cx, int y) {
		String upper = text.toUpperCase(Locale.ROOT);
		int w = width(upper) + 10;
		round(cx - w / 2, y, cx + w / 2, y + 12, 4, 0xE0050C26);
		text(upper, cx - w / 2 + 5, y + 2, 0xFFE3F3FC);
	}

	/** A small rounded label, like ACTIVE or NEW. */
	public void pill(int x, int y, String label, int fill, int color) {
		int w = width(label) + 10;
		round(x, y, x + w, y + 12, 6, fill);
		text(label, x + 5, y + 2, color);
	}

	public int pillWidth(String label) {
		return width(label) + 10;
	}

	/** An on/off switch, 22x10: green when on, like Lunar's. */
	public void toggle(int x, int y, boolean on) {
		round(x, y, x + 22, y + 10, 5, on ? t.good() : MenuTheme.alpha(t.muted(), 0x70));
		int knob = on ? x + 13 : x + 1;
		round(knob, y + 1, knob + 8, y + 9, 4, 0xFFFFFFFF);
	}

	/** A slider track with a round knob; fraction is 0..1. */
	public void slider(int x, int y, int w, double fraction) {
		round(x, y + 3, x + w, y + 6, 1.5f, MenuTheme.alpha(t.muted(), 0x60));
		int filled = (int) Math.round(w * fraction);
		round(x, y + 3, x + filled, y + 6, 1.5f, t.accent());
		int knob = x + filled - 4;
		round(knob, y, knob + 9, y + 9, 4.5f, 0xFFFFFFFF);
		ring(knob, y, knob + 9, y + 9, 4.5f, t.accent());
	}

	/** A text field. Draws a blinking caret when focused. */
	public void field(int x, int y, int w, int h, String value, String placeholder, boolean focused, String[] icon) {
		round(x, y, x + w, y + h, R_CONTROL, t.field());
		ring(x, y, x + w, y + h, R_CONTROL, focused ? t.edgeStrong() : t.edge());
		int tx = x + 6;
		if (icon != null) {
			PixelIcons.draw(d, icon, x + 4, y + (h - 12) / 2, 1, value.isEmpty() ? t.muted() : t.accent());
			tx = x + 18;
		}
		int max = w - (tx - x) - 6;
		if (value.isEmpty()) {
			text(trim(placeholder, max), tx, y + (h - 8) / 2, t.muted());
			if (focused && (System.currentTimeMillis() / 500) % 2 == 0) d.fill(tx, y + 3, tx + 1, y + h - 3, t.text());
		} else {
			String shown = value;
			while (!shown.isEmpty() && width(shown) > max) shown = shown.substring(1);
			text(shown, tx, y + (h - 8) / 2, t.text());
			if (focused && (System.currentTimeMillis() / 500) % 2 == 0) {
				int cx = tx + width(shown) + 1;
				d.fill(cx, y + 3, cx + 1, y + h - 3, t.text());
			}
		}
	}

	/** A small round dot, e.g. an on/off or online indicator. */
	public void dot(int x, int y, int size, int color) {
		round(x, y, x + size, y + size, size / 2f, color);
	}

	/** Thin scrollbar on the right of a list. */
	public void scrollbar(int x, int y, int h, double scroll, int maxScroll, int contentHeight) {
		if (maxScroll <= 0) return;
		round(x, y, x + 2, y + h, 1, MenuTheme.alpha(t.muted(), 0x30));
		int thumb = Math.max(14, h * h / Math.max(1, contentHeight));
		int ty = y + (int) Math.round((h - thumb) * (scroll / maxScroll));
		round(x, ty, x + 2, ty + thumb, 1, t.accent());
	}
}
