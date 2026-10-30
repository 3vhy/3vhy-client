package dev.threevhy.client.knife;

import dev.threevhy.client.render.ShaderPacks;

/**
 * How knives and gloves tell 3vhy's shader (shaders/core/knife) what they're made of. Every vertex
 * carries its material, the finish, a pattern seed and how close it is to the cutting edge in its
 * colour, and the knife, the glint, the wear and the light boost in its overlay slot; the shader turns
 * those into the finish, the reflections and the light. The numbers here match the ones in
 * knife_surface.glsl. With an Iris shader pack on, the pack can't run 3vhy's shader, so everything is
 * drawn with Minecraft's own entity shading and a plain colour per material instead.
 */
public final class KnifeShading {
	public static final int BLADE = 0;
	public static final int EDGE = 1;
	public static final int GUARD = 2;
	public static final int HARDWARE = 3;
	public static final int RUBBER = 4;
	public static final int POLYMER = 5;
	public static final int WOOD = 6;
	public static final int CORD = 7;
	public static final int CARBON = 8;
	public static final int RING = 9;
	public static final int ENERGY = 10;
	public static final int HANDLE_PAINT = 11;
	public static final int GLOVE = 16;
	public static final int GLOVE_ACCENT = 17;
	public static final int PALM = 18;
	public static final int ARMOR = 19;
	public static final int SKIN = 20;
	public static final int SLEEVE = 21;
	public static final int WRAP = 22;
	public static final int CUFF = 23;

	private KnifeShading() {
	}

	/** Whether 3vhy's own shader can draw (no Iris shader pack in the way). */
	public static boolean custom() {
		return !ShaderPacks.inUse();
	}

	/** The packed colour for a vertex: material, finish (or glove design), seed and edge nearness (0..1). */
	static int color(int material, int finish, int seed, float edge) {
		int g = (finish & 63) | ((seed >> 8) & 3) << 6;
		int a = Math.round(Math.max(0, Math.min(1, edge)) * 255);
		return a << 24 | (material & 255) << 16 | g << 8 | (seed & 255);
	}

	/**
	 * The packed overlay slot for a whole draw: which knife (for its blade length and holes), the glint
	 * after a swing (0..1 along the blade, 0 for none), the wear (0..1) and the light boost (0..1).
	 */
	static int frame(int knife, float glint, float wear, float boost) {
		int g = Math.round(Math.max(0, Math.min(1, glint)) * 63);
		int x = (knife & 63) | g << 6;
		int y = Math.round(Math.max(0, Math.min(1, wear)) * 100) | Math.round(Math.max(0, Math.min(1, boost)) * 100) << 7;
		return (y & 0xFFFF) << 16 | (x & 0xFFFF);
	}

	/** A plain colour for a material in a finish, for drawing without 3vhy's shader. */
	static int plain(int material, int finish, int design, int variant) {
		if (material >= GLOVE) return Gloves.plainColor(material, design, variant);
		return switch (material) {
			case BLADE, EDGE, GUARD, RING, HANDLE_PAINT -> Finish.byId(finish).plain;
			case HARDWARE -> 0xFF50535A;
			case RUBBER, POLYMER -> 0xFF18181B;
			case WOOD -> 0xFF6E4425;
			case CORD -> 0xFF26262A;
			case CARBON -> 0xFF202022;
			case ENERGY -> 0xFFE7B5FF;
			default -> 0xFF808080;
		};
	}
}
