package dev.threevhy.client.gui;

import dev.threevhy.client.module.Modules;
import java.util.List;

/**
 * The client's colour, picked in the Panels menu's Themes panel (or Theme > Panels colour), like
 * 67's themes: the Panels menu, the module list and the menu logo use it. Rainbow cycles it.
 */
public final class Accent {
	/** A named colour in the Themes panel. */
	public record Preset(String name, int color) {
	}

	public static final List<Preset> PRESETS = List.of(
			new Preset("Pink", 0xFFFF5FA2),
			new Preset("Purple", 0xFFA77BFF),
			new Preset("Blue", 0xFF5A9CFF),
			new Preset("Red", 0xFFFF5A5F),
			new Preset("Emerald", 0xFF3DDC97),
			new Preset("Cyan", 0xFF5BC8E6));

	private Accent() {
	}

	/** The colour right now (opaque). */
	public static int get() {
		return get(0f);
	}

	/** The colour right now, a little further round the rainbow when Rainbow is on, so a list of lines ripples. */
	public static int get(float offset) {
		if (Modules.THEME_RAINBOW.get()) return rainbow((System.currentTimeMillis() % 6000L) / 6000f + offset);
		return Modules.THEME_PANEL_COLOR.get() | 0xFF000000;
	}

	/** A soft rainbow colour at {@code hue} (0..1). */
	public static int rainbow(float hue) {
		float s = 0.55f;
		float v = 1f;
		float h = (hue - (float) Math.floor(hue)) * 6f;
		int sector = (int) h;
		float f = h - sector;
		float p = v * (1 - s);
		float q = v * (1 - s * f);
		float t = v * (1 - s * (1 - f));
		float r;
		float g;
		float b;
		switch (sector) {
			case 0 -> {
				r = v;
				g = t;
				b = p;
			}
			case 1 -> {
				r = q;
				g = v;
				b = p;
			}
			case 2 -> {
				r = p;
				g = v;
				b = t;
			}
			case 3 -> {
				r = p;
				g = q;
				b = v;
			}
			case 4 -> {
				r = t;
				g = p;
				b = v;
			}
			default -> {
				r = v;
				g = p;
				b = q;
			}
		}
		return 0xFF000000 | Math.round(r * 255) << 16 | Math.round(g * 255) << 8 | Math.round(b * 255);
	}
}
