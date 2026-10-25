package dev.threevhy.client.knife;

import dev.threevhy.client.ThreevhyClient;
import dev.threevhy.client.compat.Compat;
import dev.threevhy.client.gui.Smooth;
import net.minecraft.resources.Identifier;

/**
 * The knives' textures, drawn in code into one 64 x 64 grey-scale atlas when first needed: brushed steel,
 * a polished edge, wood grain, a knurled grip, a cord wrap, carbon weave and plain white. Colours come
 * from the vertex tint, so one atlas serves every finish.
 */
final class KnifeAtlas {
	static final int SIZE = 64;
	static final Identifier ID = Identifier.fromNamespaceAndPath(ThreevhyClient.MOD_ID, "knife/atlas");
	private static boolean uploaded = false;

	private KnifeAtlas() {
	}

	/** The atlas, uploaded the first time it's asked for. Render thread only. */
	static Identifier get() {
		if (!uploaded) {
			uploaded = true;
			if (Compat.uploadTexture(ID, Smooth.png(SIZE, SIZE, pixels())) == null) ThreevhyClient.LOG.warn("Couldn't make the knives' textures");
		}
		return ID;
	}

	/** Forgets the upload (after a resource reload), so it's made again. */
	static void reset() {
		uploaded = false;
	}

	static int[] pixels() {
		int[] out = new int[SIZE * SIZE];
		for (int y = 0; y < SIZE; y++) {
			for (int x = 0; x < SIZE; x++) {
				out[y * SIZE + x] = grey(value(x, y));
			}
		}
		return out;
	}

	private static float value(int x, int y) {
		if (y < 16 && x < 32) return brushed(x, y);
		if (y < 16) return polished(x - 32, y);
		if (y < 32 && x < 32) return knurl(x, y - 16);
		if (y < 32) return wood(x - 32, y - 16);
		if (y < 48 && x < 32) return wrap(x, y - 32);
		if (y >= 48 && x < 32) return carbon(x, y - 48);
		return 0.96f + 0.04f * noise(x * 7 + y * 13);
	}

	/** Fine streaks along the length (u), which on a blade runs tip to base. */
	private static float brushed(int x, int y) {
		float streak = 0.5f * smoothNoise(x * 0.12f, y * 1.7f) + 0.5f * smoothNoise(x * 0.05f + 9, y * 3.1f);
		return 0.80f + 0.18f * streak + 0.03f * noise(x * 31 + y * 17);
	}

	/** A bright, smooth bevel with a soft highlight along it. */
	private static float polished(int x, int y) {
		float band = (float) Math.exp(-Math.pow((y - 6.5) / 3.2, 2));
		return 0.86f + 0.14f * band + 0.02f * smoothNoise(x * 0.2f, y * 0.4f);
	}

	private static float knurl(int x, int y) {
		int a = Math.floorMod(x + y, 6);
		int b = Math.floorMod(x - y, 6);
		float edge = Math.min(Math.min(a, 6 - a), Math.min(b, 6 - b));
		return edge < 1 ? 0.5f : 0.78f + 0.08f * edge / 3f;
	}

	private static float wood(int x, int y) {
		float rings = (float) Math.sin(y * 0.9 + smoothNoise(x * 0.08f, y * 0.3f) * 5.0);
		return 0.68f + 0.16f * rings + 0.06f * smoothNoise(x * 0.5f, y * 2.5f);
	}

	/** The cord wrap of a katana's handle: diamonds where the cords cross. */
	private static float wrap(int x, int y) {
		int a = Math.floorMod(x + y, 8);
		int b = Math.floorMod(x - y + 64, 8);
		boolean cordA = a < 3;
		boolean cordB = b < 3;
		if (cordA && cordB) return 0.95f;
		if (cordA || cordB) return 0.82f;
		return 0.35f;
	}

	private static float carbon(int x, int y) {
		boolean over = ((x / 4) + (y / 4)) % 2 == 0;
		float along = over ? (x % 4) / 3f : (y % 4) / 3f;
		return 0.35f + 0.25f * (float) Math.sin(along * Math.PI) + 0.05f * noise(x * 3 + y * 5);
	}

	private static int grey(float value) {
		int v = Math.round(Math.max(0, Math.min(1, value)) * 255);
		return 0xFF000000 | v << 16 | v << 8 | v;
	}

	private static float noise(int seed) {
		int h = seed * 374761393 + 668265263;
		h = (h ^ (h >>> 13)) * 1274126177;
		h ^= h >>> 16;
		return (h & 0xFFFF) / 65535f;
	}

	private static float smoothNoise(float x, float y) {
		int ix = (int) Math.floor(x);
		int iy = (int) Math.floor(y);
		float fx = x - ix;
		float fy = y - iy;
		fx = fx * fx * (3 - 2 * fx);
		fy = fy * fy * (3 - 2 * fy);
		float a = noise(ix * 57 + iy * 131);
		float b = noise((ix + 1) * 57 + iy * 131);
		float c = noise(ix * 57 + (iy + 1) * 131);
		float d = noise((ix + 1) * 57 + (iy + 1) * 131);
		return a + (b - a) * fx + (c - a) * fy + (a - b - c + d) * fx * fy;
	}
}
