package dev.eventide.client.knife;

/**
 * The finishes a knife can wear. Each is drawn by Eventide's knife shader from its number (see
 * knife_surface.glsl), so they're crisp at any size and cost no textures; {@link #plain} is the colour
 * used when a shader pack is drawing instead. {@link #swatch} is a few colours for the finish grid.
 */
public enum Finish {
	STOCK(0, "Stock", 0xFFBEC2C8, 0xFFBEC2C8, 0xFF8C9096),
	FADE(1, "Fade", 0xFFF07A3A, 0xFFFFD84A, 0xFF7A3CD8),
	DOPPLER_PINK(2, "Doppler (Pink)", 0xFFB04ACB, 0xFFFF6CCB, 0xFF3A1060),
	DOPPLER_BLUE(3, "Doppler (Blue)", 0xFF1D5EE8, 0xFF74C8FF, 0xFF06124F),
	DOPPLER_RUBY(4, "Doppler (Ruby)", 0xFFB0101E, 0xFFFF4A52, 0xFF3A0006),
	DOPPLER_PEARL(5, "Doppler (Black Pearl)", 0xFF2A2060, 0xFF8070D8, 0xFF08060F),
	MARBLE_FADE(6, "Marble Fade", 0xFFE8302A, 0xFFFFD22E, 0xFF2050F0),
	CRIMSON_WEB(7, "Crimson Web", 0xFF8C0A0E, 0xFFA8121A, 0xFF120203),
	CASE_HARDENED(8, "Case Hardened", 0xFF4A62B0, 0xFFC09A48, 0xFF8C8478),
	TIGER_TOOTH(9, "Tiger Tooth", 0xFFFFB430, 0xFFFFC84A, 0xFFD85A0A),
	SLAUGHTER(10, "Slaughter", 0xFFC0283A, 0xFFF27090, 0xFFF26626),
	DAMASCUS(11, "Damascus Steel", 0xFF5A5C60, 0xFF9C9EA2, 0xFF242528),
	GAMMA(12, "Gamma", 0xFF1E9E52, 0xFF7CF2B0, 0xFF1A8CF0),
	EMERALD(13, "Emerald", 0xFF0A7A44, 0xFF46E88C, 0xFF003A1C),
	BLUE_STEEL(14, "Blue Steel", 0xFF4A5A92, 0xFF8C98B4, 0xFF5A4A80),
	STAINLESS(15, "Stainless", 0xFFDADCE0, 0xFFF0F2F4, 0xFFA8ACB2),
	BLACK_LAMINATE(16, "Black Laminate", 0xFF141416, 0xFF3A3A40, 0xFF5A381E),
	NIGHT(17, "Night", 0xFF222226, 0xFF3A3A40, 0xFF101012),
	FOREST_DDPAT(18, "Forest DDPAT", 0xFF46542E, 0xFF6E724C, 0xFF4E3822),
	URBAN_MASKED(19, "Urban Masked", 0xFF6A6C70, 0xFFA4A6AA, 0xFF2E3034),
	SAFARI_MESH(20, "Safari Mesh", 0xFF9E8658, 0xFFB09C70, 0xFF2A2418),
	SCORCHED(21, "Scorched", 0xFF3A2416, 0xFF6E5442, 0xFF0E0C0A),
	BOREAL_FOREST(22, "Boreal Forest", 0xFF545A3E, 0xFF34401E, 0xFF4E3824),
	VIOLET(23, "Eventide Violet", 0xFF8C2CF0, 0xFFEA7CFF, 0xFF2E0A6C);

	/** The number the shader knows it by. */
	public final int id;
	public final String label;
	final int plain;
	/** Three colours that sum the finish up, for its swatch. */
	public final int[] swatch;

	Finish(int id, String label, int plain, int light, int dark) {
		this.id = id;
		this.label = label;
		this.plain = plain;
		this.swatch = new int[] {plain, light, dark};
	}

	public static String[] labels() {
		Finish[] all = values();
		String[] out = new String[all.length];
		for (int i = 0; i < all.length; i++) out[i] = all[i].label;
		return out;
	}

	public static Finish byLabel(String label) {
		for (Finish finish : values()) {
			if (finish.label.equalsIgnoreCase(label) || finish.name().equalsIgnoreCase(label)) return finish;
		}
		return null;
	}

	static Finish byId(int id) {
		for (Finish finish : values()) {
			if (finish.id == id) return finish;
		}
		return STOCK;
	}

	/** The usual name for a wear amount (0 factory new .. 1 battle-scarred). */
	public static String wearName(double wear) {
		if (wear < 0.07) return "Factory New";
		if (wear < 0.15) return "Minimal Wear";
		if (wear < 0.38) return "Field-Tested";
		if (wear < 0.45) return "Well-Worn";
		return "Battle-Scarred";
	}
}
