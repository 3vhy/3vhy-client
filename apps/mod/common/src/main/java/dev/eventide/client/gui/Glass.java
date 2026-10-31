package dev.eventide.client.gui;

import dev.eventide.client.Draw;
import dev.eventide.client.Hooks;
import dev.eventide.client.module.Modules;
import dev.eventide.client.ui.Paint;
import dev.eventide.client.ui.Tokens;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.client.gui.components.AbstractStringWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.LockIconButton;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.resources.Identifier;

/**
 * Eventide's flat style for everything Minecraft draws with its own widget art, and so for every mod that uses that
 * art too: buttons, text fields, checkboxes, tabs, scrollbars, tooltips, list selections, the lines under a
 * screen's header and over its footer, list and screen backgrounds, and the chat bar. Minecraft's widgets
 * draw through a few sprites and textures; each is swapped for its glass shape here, sized to the same box,
 * so layouts don't move. Theme > Eventide buttons turns all of it off.
 */
public final class Glass {
	private static final int FIELD = 0xB8121214;
	private static final int FIELD_EDGE = 0x1FFFFFFF;
	private static final int FIELD_EDGE_HOVER = 0x3DFFFFFF;
	private static final int TRACK = 0xCC18181B;
	private static final int THUMB = 0x80FFFFFF;
	private static final int SURFACE = 0xF2161618;
	private static final Map<AbstractWidget, Long> GLAZED = new WeakHashMap<>();
	private static final int CONTAINER_GRAY = 0xFFC6C6C6;
	private static final Set<String> UNGLAZED = Collections.synchronizedSet(new LinkedHashSet<>());
	private static final Set<String> GLAZED_SPRITES = Collections.synchronizedSet(new LinkedHashSet<>());

	private Glass() {
	}

	public static boolean on() {
		return Modules.THEME_BUTTONS.get();
	}

	/** Notes that this widget was just drawn as glass. */
	public static void mark(AbstractWidget widget) {
		GLAZED.put(widget, System.nanoTime());
		Hooks.buttonsStyled = true;
	}

	/** Whether this widget was drawn as glass lately, or is a label or has art of its own that isn't Minecraft's button. */
	public static boolean covers(AbstractWidget widget) {
		if (widget instanceof ImageButton || widget instanceof LockIconButton || widget instanceof PlainTextButton || widget instanceof AbstractStringWidget) {
			return true;
		}
		Long at = GLAZED.get(widget);
		return at != null && System.nanoTime() - at < 500_000_000L || TitleScreenStyle.styled(widget);
	}

	/** Minecraft widget sprites drawn as they are because there's no glass for them (for the test's report). */
	public static Set<String> unglazed() {
		synchronized (UNGLAZED) {
			return Set.copyOf(UNGLAZED);
		}
	}

	/** Whether {@link #sprite} might draw this sprite, checked before anything is made for it. */
	public static boolean handles(Identifier sprite) {
		if (!sprite.getNamespace().equals("minecraft")) return false;
		String path = sprite.getPath();
		return (path.startsWith("widget/") || path.startsWith("tooltip/") || path.startsWith("container/anvil/text_field")) && on();
	}

	/** Whether {@link #texture} might draw this texture, checked before anything is made for it. */
	public static boolean handlesTexture(Identifier texture) {
		return texture.getNamespace().equals("minecraft") && texture.getPath().startsWith("textures/gui/") && on();
	}

	/** Minecraft widget sprites drawn as glass so far (for the test, to know the hooks are in place). */
	public static Set<String> glazed() {
		synchronized (GLAZED_SPRITES) {
			return Set.copyOf(GLAZED_SPRITES);
		}
	}

	private static float hair(Draw d) {
		return Math.max(0.5f, 1f / Math.max(1, Paint.density(d)));
	}

	/**
	 * A GUI sprite about to be drawn at (x, y), w x h: draws its glass instead and returns true, or returns
	 * false to let Minecraft draw it. {@code color} carries its fade.
	 */
	public static boolean sprite(Draw d, Identifier sprite, int x, int y, int w, int h, int color) {
		if (!sprite.getNamespace().equals("minecraft")) return false;
		String path = sprite.getPath();
		if (!path.startsWith("widget/") && !path.startsWith("tooltip/") && !path.startsWith("container/anvil/text_field")) return false;
		if (!on()) return false;
		float alpha = ((color >>> 24) & 255) / 255f;
		float old = Paint.opacity(Paint.opacity() * alpha);
		boolean drawn = true;
		switch (path) {
			case "widget/button", "widget/button_highlighted", "widget/button_disabled" -> {
				float lit = path.endsWith("highlighted") ? 1 : 0;
				if (path.endsWith("disabled")) Paint.opacity(Paint.opacity() * 0.5f);
				ButtonStyle.pill(d, x, y, x + w, y + h, lit, false);
			}
			case "widget/text_field", "widget/text_field_highlighted" -> field(d, x, y, x + w, y + h, path.endsWith("highlighted"));
			case "container/anvil/text_field", "container/anvil/text_field_disabled" -> {
				Paint.rect(d, x, y, x + w, y + h, 0, CONTAINER_GRAY);
				Paint.rect(d, x, y, x + w, y + h, Math.min(h * 0.32f, 7), Tokens.BG_2);
				if (path.endsWith("disabled")) Paint.opacity(Paint.opacity() * 0.5f);
				field(d, x, y, x + w, y + h, false);
			}
			case "widget/checkbox", "widget/checkbox_highlighted", "widget/checkbox_selected", "widget/checkbox_selected_highlighted" ->
					checkbox(d, x, y, x + w, y + h, path.contains("selected"), path.endsWith("highlighted"));
			case "widget/tab", "widget/tab_highlighted", "widget/tab_selected", "widget/tab_selected_highlighted" ->
					tab(d, x, y, x + w, y + h, path.contains("selected"), path.endsWith("highlighted"));
			case "widget/slider", "widget/slider_highlighted" -> track(d, x, y, x + w, y + h, path.endsWith("highlighted"));
			case "widget/slider_handle", "widget/slider_handle_highlighted" -> handle(d, x, y, x + w, y + h, path.endsWith("highlighted"));
			case "tooltip/background" -> tooltip(d, x + 9, y + 9, x + w - 9, y + h - 9);
			case "tooltip/frame" -> tooltipEdge(d, x + 9, y + 9, x + w - 9, y + h - 9);
			default -> {
				if (path.startsWith("widget/scroller")) scroller(d, x, y, x + w, y + h, path.contains("background"));
				else drawn = false;
			}
		}
		Paint.opacity(old);
		if (drawn) {
			if (GLAZED_SPRITES.size() < 64) GLAZED_SPRITES.add(path);
		} else if (UNGLAZED.size() < 64) {
			UNGLAZED.add(path);
		}
		return drawn;
	}

	/**
	 * A whole texture about to be drawn over (x, y), w x h: the lines under headers and over footers become a
	 * hairline, and list and screen backgrounds a violet tint. Returns whether it was drawn here.
	 */
	public static boolean texture(Draw d, Identifier texture, int x, int y, int w, int h) {
		if (!texture.getNamespace().equals("minecraft") || !texture.getPath().startsWith("textures/gui/") || !on()) return false;
		switch (texture.getPath()) {
			case "textures/gui/header_separator.png", "textures/gui/inworld_header_separator.png" -> {
				float hair = hair(d);
				Paint.rect(d, x, y + h - hair, x + w, y + h, 0, 0x1FFFFFFF);
			}
			case "textures/gui/footer_separator.png", "textures/gui/inworld_footer_separator.png" -> {
				float hair = hair(d);
				Paint.rect(d, x, y, x + w, y + hair, 0, 0x1FFFFFFF);
			}
			case "textures/gui/menu_list_background.png", "textures/gui/inworld_menu_list_background.png" ->
					Paint.rect(d, x, y, x + w, y + h, 0, 0x4D000000);
			case "textures/gui/menu_background.png" -> Paint.rect(d, x, y, x + w, y + h, 0, 0xAB0A0A0B);
			case "textures/gui/inworld_menu_background.png" -> Paint.rect(d, x, y, x + w, y + h, 0, 0x7C0A0A0B);
			default -> {
				return false;
			}
		}
		return true;
	}

	/** A text field: a dark tone step with a 1px edge that takes the accent while you type in it. */
	public static void field(Draw d, float x1, float y1, float x2, float y2, boolean focused) {
		float h = y2 - y1;
		float r = Math.min(h * 0.32f, 7);
		float hair = hair(d);
		Paint.rect(d, x1, y1, x2, y2, r, FIELD);
		Paint.stroke(d, x1, y1, x2, y2, r, hair, FIELD_EDGE);
		if (focused) Paint.stroke(d, x1, y1, x2, y2, r, Math.max(hair, 0.75f), Accent.get());
	}

	/** A checkbox: a dark square that fills with the accent and shows a tick when it's on. */
	public static void checkbox(Draw d, float x1, float y1, float x2, float y2, boolean on, boolean hover) {
		float s = y2 - y1;
		float r = s * 0.3f;
		float hair = hair(d);
		if (on) {
			Paint.rect(d, x1, y1, x2, y2, r, hover ? Accent.lighten(Accent.get(), 0.12f) : Accent.get());
			float icon = s * 0.7f;
			Paint.icon(d, "check", x1 + (s - icon) / 2f, y1 + (s - icon) / 2f, icon, 0xFFFFFFFF);
		} else {
			Paint.rect(d, x1, y1, x2, y2, r, FIELD);
			Paint.stroke(d, x1, y1, x2, y2, r, hair, hover ? FIELD_EDGE_HOVER : FIELD_EDGE);
		}
	}

	/** A tab: a pill for the open one, a faint tone for the rest. */
	public static void tab(Draw d, float x1, float y1, float x2, float y2, boolean selected, boolean hover) {
		float inset = 2;
		float bx1 = x1 + inset;
		float by1 = y1 + inset;
		float bx2 = x2 - inset;
		float by2 = y2 - 1;
		float r = (by2 - by1) / 2f;
		float hair = hair(d);
		if (selected) {
			ButtonStyle.pill(d, bx1, by1, bx2, by2, hover ? 1 : 0.6f, false);
		} else {
			Paint.rect(d, bx1, by1, bx2, by2, r, hover ? Tokens.CONTROL_HOVER : 0x08FFFFFF);
			Paint.stroke(d, bx1, by1, bx2, by2, r, hair, hover ? FIELD_EDGE_HOVER : Tokens.CONTROL_EDGE);
		}
	}

	/** A slider's track, for sliders drawn by other code than Minecraft's own slider. */
	public static void track(Draw d, float x1, float y1, float x2, float y2, boolean hover) {
		float r = (y2 - y1) / 2f;
		Paint.rect(d, x1, y1, x2, y2, r, TRACK);
		Paint.stroke(d, x1, y1, x2, y2, r, hair(d), hover ? FIELD_EDGE_HOVER : FIELD_EDGE);
	}

	/** A slider's handle, for sliders drawn by other code than Minecraft's own slider. */
	public static void handle(Draw d, float x1, float y1, float x2, float y2, boolean hover) {
		float w = Math.max(2, (x2 - x1) * 0.35f);
		float cx = (x1 + x2) / 2f;
		float inset = (y2 - y1) * 0.16f;
		Paint.rect(d, cx - w / 2f, y1 + inset, cx + w / 2f, y2 - inset, w / 2f, 0xF2FFFFFF);
	}

	/** A scrollbar: a thin faint track and a pale thumb. */
	public static void scroller(Draw d, float x1, float y1, float x2, float y2, boolean track) {
		float w = x2 - x1;
		float h = y2 - y1;
		if (w <= h) {
			float cx = (x1 + x2) / 2f;
			if (track) {
				Paint.rect(d, cx - w * 0.17f, y1, cx + w * 0.17f, y2, w * 0.17f, 0x14FFFFFF);
			} else {
				Paint.rect(d, cx - w * 0.3f, y1, cx + w * 0.3f, y2, w * 0.3f, THUMB);
			}
		} else {
			float cy = (y1 + y2) / 2f;
			if (track) Paint.rect(d, x1, cy - h * 0.17f, x2, cy + h * 0.17f, h * 0.17f, 0x14FFFFFF);
			else Paint.rect(d, x1, cy - h * 0.3f, x2, cy + h * 0.3f, h * 0.3f, THUMB);
		}
	}

	/** A tooltip's body: an opaque dark surface, with the small shadow of something that floats. */
	public static void tooltip(Draw d, float x1, float y1, float x2, float y2) {
		float r = 4;
		Paint.shadow(d, x1, y1, x2, y2, r, 0, 2, 8, -1, 0x66000000);
		Paint.rect(d, x1, y1, x2, y2, r, SURFACE);
	}

	/** A tooltip's edge: a 1px line. */
	public static void tooltipEdge(Draw d, float x1, float y1, float x2, float y2) {
		Paint.stroke(d, x1, y1, x2, y2, 4, hair(d), FIELD_EDGE);
	}

	/** The row picked in a list: a tone step, with an accent edge when the list has focus. */
	public static void selection(Draw d, float x1, float y1, float x2, float y2, boolean focused) {
		float r = Math.min(6, (y2 - y1) * 0.25f);
		float hair = hair(d);
		Paint.rect(d, x1, y1, x2, y2, r, 0x1AFFFFFF);
		Paint.stroke(d, x1, y1, x2, y2, r, Math.max(hair, 0.75f), focused ? Accent.get() : 0x2EFFFFFF);
	}

	/** The bar you type chat in: a dark bar along the bottom of the screen. */
	public static void chatBar(Draw d, float x1, float y1, float x2, float y2) {
		float r = Math.min(5, (y2 - y1) / 2f);
		Paint.rect(d, x1, y1, x2, y2, r, 0xC0121214);
		Paint.stroke(d, x1, y1, x2, y2, r, hair(d), FIELD_EDGE);
	}
}
