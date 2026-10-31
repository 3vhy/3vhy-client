package dev.eventide.client.knife;

/**
 * The glove designs and their colourways. The shader draws each from its number and colourway (see
 * k_glove in knife_surface.glsl); the names and plain colours here are for the settings and for drawing
 * without Eventide's shader.
 */
public final class Gloves {
	/** Design names, by the number the shader knows them by. */
	public static final String[] DESIGNS = {"Bare Hands", "Tactical", "Sport", "Driver", "Hand Wraps", "Moto", "Specialist", "Bloodhound"};
	/** Each design's eight colourways. */
	public static final String[][] VARIANTS = {
		{"Light", "Tan", "Brown", "Pale", "Dark", "Olive", "Deep", "Rosy"},
		{"Grey", "Desert", "Olive", "Black Ops", "Urban", "Sand", "Rust", "Jungle"},
		{"Ocean", "Crimson", "Lime", "Bubblegum", "Gold Trim", "Neon", "Sunset", "Arctic"},
		{"Black", "Brown", "Tan", "Red", "Ivory", "Navy", "Moss", "Saddle"},
		{"Jade", "Red", "Black", "Linen", "Blue", "Leather", "Mustard", "Plum"},
		{"Red", "Blue", "Green", "Orange", "White", "Black", "Hazard", "Steel"},
		{"Forest", "Crimson", "Emerald", "Fade", "Arctic", "Night", "Navy", "Desert"},
		{"Silver", "Bronze", "Snakebite", "Copper", "Guerrilla", "Charred", "Chrome", "Blood"}
	};
	private static final int[][] MAIN = {
		{0xFFC78F6B, 0xFF9E6B4C, 0xFF6B452E, 0xFFE0AE8C, 0xFF4C3324, 0xFFB8855C, 0xFF8C5C42, 0xFFF2C2A4},
		{0xFF5C5E60, 0xFF8C7554, 0xFF4C5438, 0xFF141416, 0xFF4C5666, 0xFF9E8C6B, 0xFF664838, 0xFF333D33},
		{0xFF1442D8, 0xFFBF0F14, 0xFF26992E, 0xFFF261A6, 0xFF0F0F12, 0xFF6B1FD8, 0xFFFF8C0D, 0xFF0DB2CC},
		{0xFF0D0C0C, 0xFF522B14, 0xFF996B3D, 0xFF850F0F, 0xFFD9D4C7, 0xFF141A33, 0xFF333826, 0xFF6B4C2E},
		{0xFF1A6B2E, 0xFF9E0F0F, 0xFF0F0F12, 0xFFE0DBCC, 0xFF1A389E, 0xFF805C38, 0xFFB28C1A, 0xFF6B1A8C},
		{0xFFB20F0F, 0xFF1A4CCC, 0xFF268C26, 0xFFFF730D, 0xFFE6E6EB, 0xFF0F0F12, 0xFFF2D91A, 0xFF66666B},
		{0xFF404D2E, 0xFF8C0D14, 0xFF0D7352, 0xFFF2804C, 0xFFD9E0EB, 0xFF141416, 0xFF262E73, 0xFF806640},
		{0xFF0F0F10, 0xFF4C2E17, 0xFF2E471F, 0xFF6B421F, 0xFF404033, 0xFF261A14, 0xFF736B66, 0xFF590D0D}
	};

	private Gloves() {
	}

	/** A plain colour for a glove part, for drawing without Eventide's shader. */
	static int plainColor(int material, int design, int variant) {
		int d = Math.floorMod(design, DESIGNS.length);
		int v = Math.floorMod(variant, 8);
		int main = MAIN[d][v];
		if (material == KnifeShading.SKIN || d == 0 && material != KnifeShading.SLEEVE && material != KnifeShading.CUFF) return MAIN[0][v];
		return switch (material) {
			case KnifeShading.SLEEVE -> 0xFF1A1B1F;
			case KnifeShading.PALM, KnifeShading.ARMOR -> 0xFF161618;
			case KnifeShading.GLOVE_ACCENT, KnifeShading.WRAP, KnifeShading.CUFF -> d == 2 ? 0xFFEBEEF2 : main;
			default -> main;
		};
	}
}
