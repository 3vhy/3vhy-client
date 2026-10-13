package dev.threevhy.client.gui;

import dev.threevhy.client.module.Modules;

/**
 * Colours for 3vhy's menus, in Lunar's style: dark is smoked glass with thin white edges and 3vhy's
 * ice-blue as the accent, light is frosted white with royal-blue text.
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
			0x66000000,
			0xC00E1014,
			0xC00B0D10,
			0x2EFFFFFF,
			0x8CFFFFFF,
			0x8A16181D,
			0xB0252830,
			0xB0222A36,
			0xFFFFFFFF,
			0xFFA3A9B6,
			0xFF5BC8E6,
			0xFF061D3C,
			0xB0090B0E,
			0xFF4ADE80,
			0xFFF05A5A,
			0xFFFFD166,
			0xFFFFFFFF);

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
