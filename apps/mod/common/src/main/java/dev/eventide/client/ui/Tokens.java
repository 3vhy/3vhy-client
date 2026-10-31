package dev.eventide.client.ui;

/**
 * Eventide's design tokens: every colour, size and timing the menus, the Orbit and the HUD share, in one
 * place. The palette is a true near-black night: surfaces are told apart by lightness alone (ground,
 * surface, raised, each a step lighter), text is white with two neutral greys that both read at 4.5:1, and
 * one hue, the logo's violet, goes only on what's on or chosen (Theme's Accent colour can change it). No
 * gradients, glows or tinted surfaces. Colours are ARGB.
 */
public final class Tokens {
	private Tokens() {
	}

	// Brand: the logo's tile, its trident and its riptide rings (assets/brand/eventide-icon.svg), and the wordmark
	public static final int BRAND_TILE = 0xFF120D1C;
	public static final int BRAND_TRIDENT = 0xFFF3ECFF;
	public static final int BRAND_SWIRL = 0xFFA855F7;
	public static final int BRAND_WORDMARK = 0xFFF5F5F6;

	// Surfaces, darkest first, a lightness step apart
	public static final int BG_0 = 0xFF0A0A0B;
	public static final int BG_2 = 0xFF141416;
	public static final int SURFACE = 0xFF19191C;
	public static final int RAISED = 0xFF232327;
	/** Over the blurred game behind a menu: dark enough that the menu's surfaces read on it. */
	public static final int SCRIM = 0xE30A0A0B;

	// Text: white, then two greys far enough apart to read as two
	public static final int TEXT = 0xFFF5F5F6;
	public static final int TEXT_2 = 0xFFB4B4BA;
	public static final int TEXT_3 = 0xFF8E8E95;

	// Data hues, for what a value means (health, pops, a staff rank), never for chrome
	public static final int MAGENTA = 0xFFD946EF;
	public static final int PINK = 0xFFFF5AD9;
	public static final int LILAC = 0xFFCDB6FF;
	public static final int LILAC_2 = 0xFFAC86FF;

	// Signals
	public static final int GOLD = 0xFFFFD27A;
	public static final int MINT = 0xFF6EE7B7;
	public static final int LOW = 0xFFFF5C7A;
	public static final int EMBER = 0xFFFF8A5C;

	/** HUD pieces and floating panels: a neutral dark you can see the game through, and its 1px edge. */
	public static final int GLASS = 0xB80C0C0E;
	public static final int GLASS_SOLID = 0xEB111113;
	public static final int GLASS_EDGE = 0x14FFFFFF;
	/** Round buttons, the search field and other quiet controls. */
	public static final int CONTROL = 0x0FFFFFFF;
	public static final int CONTROL_HOVER = 0x1AFFFFFF;
	public static final int CONTROL_EDGE = 0x1AFFFFFF;

	/** The accent: the logo's violet (Accent derives everything else from it). */
	public static final int PRIMARY_MID = BRAND_SWIRL;

	/** Corner radius of a floating panel (a settings sheet); cards, controls and chips step down from it. */
	public static final float R_PANEL = 14;
	public static final float R_CARD = 10;
	public static final float R_CONTROL = 8;
	public static final float R_PILL = 999;

	// Motion, in milliseconds. UI moves stay under 300ms and ease out (hover and press faster still), so
	// the menus answer at once; nothing that a key opens animates on the way in.
	public static final int T_SWITCH_KNOB = 160;
	public static final int T_SWITCH_FILL = 140;
	public static final int T_HOVER = 120;
	public static final int T_COLOR = 140;
	public static final int T_INDICATOR = 200;
	public static final int T_CAPTION = 160;
	public static final int T_SHEET = 220;
	/** How far things rise as they appear. */
	public static final float REVEAL_RISE = 6;
}
