package dev.eventide.client.gui;

import dev.eventide.client.Draw;
import dev.eventide.client.Hooks;
import dev.eventide.client.module.Modules;
import dev.eventide.client.ui.Grad;
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
 * Eventide's glass for everything Minecraft draws with its own widget art, and so for every mod that uses that
 * art too: buttons, text fields, checkboxes, tabs, scrollbars, tooltips, list selections, the lines under a
 * screen's header and over its footer, list and screen backgrounds, and the chat bar. Minecraft's widgets
 * draw through a few sprites and textures; each is swapped for its glass shape here, sized to the same box,
 * so layouts don't move. Theme > Glass buttons turns all of it off.
 */
public final class Glass {
	private static final Grad FIELD = Grad.down(0x9E1A1530, 0x9E110E22);
	private static final Grad FIELD_EDGE = Grad.down(0x3DFFFFFF, 0x14FFFFFF);
	private static final Grad THUMB = Grad.down(0xB3CDB6FF, 0x80AC86FF);
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
				Paint.rect(d, x, y + h - hair, x + w, y + h, 0, 0x29CDB6FF);
			}
			case "textures/gui/footer_separator.png", "textures/gui/inworld_footer_separator.png" -> {
				float hair = hair(d);
				Paint.rect(d, x, y, x + w, y + hair, 0, 0x29CDB6FF);
			}
			case "textures/gui/menu_list_background.png", "textures/gui/inworld_menu_list_background.png" ->
					Paint.rect(d, x, y, x + w, y + h, 0, 0x4D06040E);
			case "textures/gui/menu_background.png" -> Paint.rect(d, x, y, x + w, y + h, 0, Grad.down(0x9E0C0818, 0xB8060410));
			case "textures/gui/inworld_menu_background.png" -> Paint.rect(d, x, y, x + w, y + h, 0, Grad.down(0x6B0C0818, 0x8C060410));
			default -> {
				return false;
			}
		}
		return true;
	}

	/** A text field: dark violet glass, rounded, with a lilac edge and a faint glow while you type in it. */
	public static void field(Draw d, float x1, float y1, float x2, float y2, boolean focused) {
		float h = y2 - y1;
		float r = Math.min(h * 0.32f, 7);
		float hair = hair(d);
		if (focused) Paint.shadow(d, x1, y1, x2, y2, r, 0, 0, h * 0.5f, -h * 0.15f, Accent.glow(0.45f));
		Paint.rect(d, x1, y1, x2, y2, r, FIELD);
		Paint.stroke(d, x1, y1, x2, y2, r, hair, FIELD_EDGE);
		if (focused) Paint.stroke(d, x1, y1, x2, y2, r, Math.max(hair, 0.75f), Paint.fade(Accent.lighten(Accent.get(), 0.35f), 0.85f));
	}

	/** A checkbox: a glass square that fills with the accent and shows a tick when it's on. */
	public static void checkbox(Draw d, float x1, float y1, float x2, float y2, boolean on, boolean hover) {
		float s = y2 - y1;
		float r = s * 0.3f;
		float hair = hair(d);
		if (on) {
			Paint.shadow(d, x1, y1, x2, y2, r, 0, 0, s * 0.6f, -s * 0.15f, Accent.glow(hover ? 0.9f : 0.6f));
			Paint.rect(d, x1, y1, x2, y2, r, Accent.toggle());
			Paint.stroke(d, x1, y1, x2, y2, r, hair, Grad.down(0x59FFFFFF, 0x0DFFFFFF));
			float icon = s * 0.7f;
			Paint.icon(d, "check", x1 + (s - icon) / 2f, y1 + (s - icon) / 2f, icon, 0xFFFFFFFF);
		} else {
			Paint.rect(d, x1, y1, x2, y2, r, FIELD);
			Paint.stroke(d, x1, y1, x2, y2, r, hair, hover ? Grad.down(0x80CDB6FF, 0x33CDB6FF) : FIELD_EDGE);
		}
	}

	/** A tab: a glass pill for the open one, faint glass for the rest. */
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
			Paint.stroke(d, bx1, by1, bx2, by2, r, hair, hover ? 0x40CDB6FF : Tokens.CONTROL_EDGE);
		}
	}

	/** A slider's track, for sliders drawn by other code than Minecraft's own slider. */
	public static void track(Draw d, float x1, float y1, float x2, float y2, boolean hover) {
		float r = (y2 - y1) / 2f;
		Paint.rect(d, x1, y1, x2, y2, r, Grad.down(0xA0332C55, 0xA0241F40));
		Paint.stroke(d, x1, y1, x2, y2, r, hair(d), hover ? Grad.down(0xB3FFFFFF, 0x26FFFFFF) : Grad.down(0x8CFFFFFF, 0x14FFFFFF));
	}

	/** A slider's handle, for sliders drawn by other code than Minecraft's own slider. */
	public static void handle(Draw d, float x1, float y1, float x2, float y2, boolean hover) {
		float w = Math.max(2, (x2 - x1) * 0.35f);
		float cx = (x1 + x2) / 2f;
		float inset = (y2 - y1) * 0.16f;
		Paint.shadow(d, cx - w / 2f, y1 + inset, cx + w / 2f, y2 - inset, w / 2f, 0, 0, (y2 - y1) * 0.3f, 0, Accent.glow(hover ? 0.9f : 0.6f));
		Paint.rect(d, cx - w / 2f, y1 + inset, cx + w / 2f, y2 - inset, w / 2f, 0xF2FFFFFF);
	}

	/** A scrollbar: a thin faint track and a lilac thumb. */
	public static void scroller(Draw d, float x1, float y1, float x2, float y2, boolean track) {
		float w = x2 - x1;
		float h = y2 - y1;
		if (w <= h) {
			float cx = (x1 + x2) / 2f;
			if (track) {
				Paint.rect(d, cx - w * 0.17f, y1, cx + w * 0.17f, y2, w * 0.17f, 0x14FFFFFF);
			} else {
				Paint.shadow(d, cx - w * 0.3f, y1, cx + w * 0.3f, y2, w * 0.3f, 0, 0, w * 1.2f, 0, Accent.glow(0.35f));
				Paint.rect(d, cx - w * 0.3f, y1, cx + w * 0.3f, y2, w * 0.3f, THUMB);
			}
		} else {
			float cy = (y1 + y2) / 2f;
			if (track) Paint.rect(d, x1, cy - h * 0.17f, x2, cy + h * 0.17f, h * 0.17f, 0x14FFFFFF);
			else Paint.rect(d, x1, cy - h * 0.3f, x2, cy + h * 0.3f, h * 0.3f, THUMB);
		}
	}

	/** A tooltip's body: deep violet glass with a soft shadow. */
	public static void tooltip(Draw d, float x1, float y1, float x2, float y2) {
		float r = 4;
		Paint.shadow(d, x1, y1, x2, y2, r, 0, 2, 9, -1, 0xA6000000);
		Paint.rect(d, x1, y1, x2, y2, r, Grad.down(0xF21A1530, 0xF2100C1F));
	}

	/** A tooltip's edge: a lilac hairline, lit along the top. */
	public static void tooltipEdge(Draw d, float x1, float y1, float x2, float y2) {
		Paint.stroke(d, x1, y1, x2, y2, 4, hair(d), Grad.down(0x80CDB6FF, 0x33AC86FF));
	}

	/** The row picked in a list: a faint glass bar, with a lilac edge when the list has focus. */
	public static void selection(Draw d, float x1, float y1, float x2, float y2, boolean focused) {
		float r = Math.min(6, (y2 - y1) * 0.25f);
		float hair = hair(d);
		Paint.rect(d, x1, y1, x2, y2, r, focused ? 0x1FCDB6FF : 0x14FFFFFF);
		Paint.stroke(d, x1, y1, x2, y2, r, Math.max(hair, 0.75f), focused ? Paint.fade(Accent.lighten(Accent.get(), 0.35f), 0.8f) : 0x33FFFFFF);
	}

	/** The bar you type chat in: a glass pill along the bottom of the screen. */
	public static void chatBar(Draw d, float x1, float y1, float x2, float y2) {
		float r = Math.min(5, (y2 - y1) / 2f);
		Paint.rect(d, x1, y1, x2, y2, r, Grad.down(0xB81A1530, 0xB8100C1F));
		Paint.stroke(d, x1, y1, x2, y2, r, hair(d), FIELD_EDGE);
	}
}
