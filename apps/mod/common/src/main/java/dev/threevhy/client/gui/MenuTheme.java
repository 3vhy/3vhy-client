package dev.threevhy.client.gui;

import dev.threevhy.client.module.Modules;

/**
 * Colours for 3vhy's menus. Laid out like NoRisk's glass panels, coloured like the 3vhy banner:
 * dark is navy glass with ice-blue edges, light is frosted white with royal-blue text.
 */
public record MenuTheme(
		int tint,
		int panel,
		int sidebar,
		int edge,
		int edgeStrong,
		int card,
		int cardHover,
		int cardOn,
		int text,
		int muted,
		int accent,
		int onAccent,
		int field,
		int good,
		int bad,
		int badge,
		int snow) {

	public static final MenuTheme DARK = new MenuTheme(
			0x50040A20,
			0xB80A1640,
			0xB80A1640,
			0x50A9D8F2,
			0xC05BC8E6,
			0x60132B6E,
			0x901C3C8C,
			0x70184C8A,
			0xFFE3F3FC,
			0xFF93AEDA,
			0xFF5BC8E6,
			0xFF061D3C,
			0xC0050C26,
			0xFF4ADE80,
			0xFFFF6B81,
			0xFFFFD166,
			0xFFE3F3FC);

	public static final MenuTheme LIGHT = new MenuTheme(
			0x40E3F3FC,
			0xD8EEF7FD,
			0xD8EEF7FD,
			0x601A4FBA,
			0xD01A4FBA,
			0x90FFFFFF,
			0xC0FFFFFF,
			0xB0DDF1FB,
			0xFF0A1846,
			0xFF4A6694,
			0xFF1A4FBA,
			0xFFFFFFFF,
			0xE0FFFFFF,
			0xFF16A34A,
			0xFFDC2645,
			0xFFD97706,
			0xFFFFFFFF);

	public static MenuTheme current() {
		return Modules.THEME_MODE.is("Light") ? LIGHT : DARK;
	}

	public boolean isLight() {
		return this == LIGHT;
	}

	/** The same colour with a different alpha (0..255). */
	public static int alpha(int argb, int alpha) {
		return (Math.max(0, Math.min(255, alpha)) << 24) | (argb & 0xFFFFFF);
	}
}
