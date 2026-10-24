package dev.threevhy.client.ui;

/**
 * 3vhy's design tokens: every colour, size and timing the menus, the Orbit and the HUD share, in one
 * place. The palette is a near-black violet night with lilac text and a violet-to-magenta accent; the
 * same values drive the download page (website/index.html), so the game and the site look alike.
 * Colours are ARGB.
 */
public final class Tokens {
	private Tokens() {
	}

	// Night sky and surfaces, darkest first
	public static final int BG_0 = 0xFF05040A;
	public static final int BG_1 = 0xFF0B0915;
	public static final int BG_2 = 0xFF131024;
	public static final int SURFACE = 0xFF19152D;
	public static final int SURFACE_HI = 0xFF221C3C;

	// Text
	public static final int TEXT = 0xFFF6F3FF;
	public static final int TEXT_2 = 0xFFB4ACD2;
	public static final int TEXT_3 = 0xFF7C7599;

	// Accent family
	public static final int INDIGO = 0xFF5F04EB;
	public static final int VIOLET = 0xFF880FF9;
	public static final int PURPLE = 0xFFB51FF4;
	public static final int MAGENTA = 0xFFD129F5;
	public static final int PINK = 0xFFFF5AD9;
	public static final int LILAC = 0xFFCDB6FF;
	public static final int LILAC_2 = 0xFFAC86FF;
	public static final int LILAC_3 = 0xFFEBA2FF;

	// Signals
	public static final int GOLD = 0xFFFFD27A;
	public static final int MINT = 0xFF8AF5C8;
	public static final int LOW = 0xFFFF5C84;
	public static final int EMBER = 0xFFFF8A5C;

	// Hairlines
	public static final int STROKE = 0x12FFFFFF;
	public static final int STROKE_HI = 0x24FFFFFF;

	// Glass: HUD pieces and floating panels
	public static final int GLASS = 0xA80A0814;
	public static final int GLASS_SOLID = 0xE60E0B1C;
	public static final int GLASS_EDGE = 0x1AFFFFFF;
	public static final int GLASS_SHINE = 0x14FFFFFF;
	/** Module cards: a soft top-to-bottom shade. */
	public static final int CARD_TOP = 0x80221C3C;
	public static final int CARD_BOTTOM = 0x6B131024;
	public static final int CARD_EDGE = 0x12FFFFFF;
	public static final int CARD_SHINE = 0x0DFFFFFF;
	/** Round buttons, the search field and other quiet controls. */
	public static final int CONTROL = 0x0FFFFFFF;
	public static final int CONTROL_HOVER = 0x1AFFFFFF;
	public static final int CONTROL_EDGE = 0x1AFFFFFF;
	/** An idle icon tile, an off switch's track. */
	public static final int TILE_OFF = 0x0FFFFFFF;
	public static final int SWITCH_OFF = 0x1FFFFFFF;
	public static final int KEY = 0x0DFFFFFF;

	// The accent gradient, violet to magenta (Accent derives other themes from the middle stop)
	public static final int PRIMARY_FROM = 0xFF7A2CFF;
	public static final int PRIMARY_MID = 0xFFA52BF5;
	public static final int PRIMARY_TO = 0xFFD12CF0;
	public static final int TILE_FROM = 0xFF7C2CFF;
	public static final int TILE_TO = 0xFFC22CF4;
	public static final int SWITCH_FROM = 0xFF8A2CFF;
	public static final int SWITCH_TO = 0xFFC62CF2;
	public static final int BAR_FROM = 0xFF8A2CFF;
	public static final int BAR_TO = 0xFFEBA2FF;
	/** The glow under lit things: purple at 90%. */
	public static final int GLOW = 0xE6B51FF4;

	// Corner radii, in layout units (a layout unit is one CSS pixel on the site)
	public static final float R_CARD = 20;
	public static final float R_TILE = 12;
	public static final float R_GLASS = 14;
	public static final float R_KEY = 9;
	public static final float R_PANEL = 24;
	public static final float R_PILL = 999;

	// Motion, in milliseconds
	public static final int T_SWITCH_KNOB = 500;
	public static final int T_SWITCH_FILL = 350;
	public static final int T_HOVER = 500;
	public static final int T_COLOR = 300;
	public static final int T_INDICATOR = 550;
	public static final int T_PRESS = 140;
	public static final int T_CAPTION = 800;
	public static final int T_REVEAL = 1000;
	public static final int T_REVEAL_MOVE = 1150;
	public static final int T_STAGGER = 80;
	public static final int T_SHEET = 450;
	public static final int T_CROSSFADE = 1200;
	public static final int T_RIPPLE = 900;
	/** How far things rise as they appear, and how far a hovered card lifts. */
	public static final float REVEAL_RISE = 22;
	public static final float HOVER_LIFT = 3;
}
