package dev.threevhy.client.knife;

import java.util.HashMap;
import java.util.Map;

/**
 * The gloved hand that holds the knife: a fist built from code around the knife's handle, closed the way
 * a real hammer grip closes. In the hand's own frame the handle runs along +Y (index finger towards the
 * blade), the palm and the back of the hand are towards +X (the knife's edge) and the wrist is towards +Z
 * (the flat that faces you). Each finger leaves its knuckle at the back corner, crosses the far side,
 * comes down the spine side and curls across the near side towards the heel of the hand; the thumb runs
 * from the heel up across the front of the handle to just under the guard. The finished fist is a little
 * bigger than life and turned on the handle so the camera sees the thumb and the back of the hand, as a
 * classic shooter's viewmodel does. Units are the knife's pixels; each fist is made once per handle size
 * and glove design.
 */
final class KnifeHand {
	/** How much bigger than life the hand is drawn against the knife. */
	static final float SIZE = 1.2f;
	/** How far the fist is turned on the handle (degrees about the handle, towards the thumb). */
	static final float TURN = -45;
	/** Which way the forearm runs from the wrist, in the knife's frame. */
	static final float[] FOREARM;
	/** Where the wrist is, in the knife's frame, for animations that swing from it. */
	static final float[] WRIST;

	private static final float[] FOREARM_HAND = Shape.normalize(new float[] {0.55f, -0.5f, 0.67f});
	private static final float[] JOINTS = {305, 218, 150, 100};
	private static final float[] FINGER_Y = {1.36f, 0.45f, -0.45f, -1.28f};
	private static final float[] FINGER_R = {0.44f, 0.46f, 0.43f, 0.38f};
	private static final float[] UP = {0, 1, 0};
	private static final Map<String, Shape> CACHE = new HashMap<>();

	static {
		FOREARM = turned(FOREARM_HAND, 1);
		WRIST = turned(Shape.add(new float[] {1.4f, -0.1f, 2.25f}, Shape.scale(FOREARM_HAND, 0.3f)), SIZE);
	}

	private KnifeHand() {
	}

	/** The fist for a handle {@code a} wide (towards the edge) and {@code b} thick, half each way, in glove {@code design}. */
	static Shape get(float a, float b, int design) {
		String key = Math.round(a * 100) + ":" + Math.round(b * 100) + ":" + design;
		return CACHE.computeIfAbsent(key, k -> {
			Shape hand = build(a / SIZE, b / SIZE, design);
			hand.turn(TURN, SIZE);
			return hand;
		});
	}

	private static float[] turned(float[] p, float scale) {
		double t = Math.toRadians(TURN);
		float c = (float) Math.cos(t);
		float s = (float) Math.sin(t);
		return new float[] {(c * p[0] + s * p[2]) * scale, p[1] * scale, (-s * p[0] + c * p[2]) * scale};
	}

	private static float[] around(float degrees, float y, float ra, float rb) {
		double t = Math.toRadians(degrees);
		return new float[] {ra * (float) Math.cos(t), y, rb * (float) Math.sin(t)};
	}

	private static Shape build(float a, float b, int design) {
		Shape m = new Shape();
		boolean bare = design == 0;
		int skin = bare ? KnifeShading.SKIN : KnifeShading.GLOVE;
		int palm = bare ? KnifeShading.SKIN : KnifeShading.PALM;
		boolean armor = design == 1 || design == 5 || design == 7;
		for (int k = 0; k < 4; k++) {
			float y = FINGER_Y[k];
			float r = FINGER_R[k];
			float ra = a + r + 0.03f;
			float rb = b + r + 0.03f;
			float[] mcp = around(JOINTS[0], y, ra + 0.25f, rb + 0.15f);
			float[] pip = around(JOINTS[1], y, ra + 0.04f, rb + 0.04f);
			float[] dip = around(JOINTS[2], y, ra, rb);
			float[] tip = around(JOINTS[3] + 6 * k, y - 0.04f, ra - 0.06f, rb - 0.04f);
			int start = m.quads();
			tube(m, new float[][] {mcp, pip}, new float[][] {{r * 1.08f, r}, {r * 1.02f, r * 0.96f}}, skin, Shape.DOME, Shape.DOME, UP);
			tube(m, new float[][] {pip, dip}, new float[][] {{r, r * 0.95f}, {r * 0.94f, r * 0.9f}}, skin, Shape.DOME, Shape.DOME, UP);
			tube(m, new float[][] {dip, tip}, new float[][] {{r * 0.92f, r * 0.88f}, {r * 0.8f, r * 0.76f}}, skin, Shape.DOME, Shape.DOME, UP);
			if (!bare) m.restyle(start, (p, n) -> n[0] * p[0] + n[2] * p[2] < -0.3f * (float) Math.hypot(p[0], p[2]), palm);
			if (armor) {
				float[] out = Shape.normalize(new float[] {mcp[0], 0, mcp[2]});
				float[] c = Shape.add(Shape.add(mcp, Shape.scale(out, r * 0.55f)), new float[] {0.25f, 0, 0});
				float pad = design == 5 ? 0.38f : 0.3f;
				tube(m, new float[][] {Shape.add(c, new float[] {0, -r * 0.75f, 0}), Shape.add(c, new float[] {0, r * 0.75f, 0})},
						new float[][] {{pad, pad * 1.4f}, {pad, pad * 1.4f}}, KnifeShading.ARMOR, Shape.DOME, Shape.DOME, new float[] {1, 0, 0});
			}
		}
		int slabs = 7;
		float[][] body = new float[slabs][];
		float[][] bodyRadii = new float[slabs][];
		for (int i = 0; i < slabs; i++) {
			float f = i / (float) (slabs - 1);
			float bulge = (float) Math.sin(Math.PI * f);
			body[i] = new float[] {a + 0.72f + 0.1f * bulge, 0.05f - 0.15f * f, -0.95f + 3.2f * f};
			bodyRadii[i] = new float[] {2.05f - 0.35f * f, 0.72f + 0.08f * bulge};
		}
		int bodyStart = m.quads();
		tube(m, body, bodyRadii, skin, Shape.DOME, Shape.OPEN, UP);
		if (!bare) m.restyle(bodyStart, (p, n) -> n[0] < -0.45f, palm);
		float[] t0 = {a + 0.75f, -0.75f, b + 1.2f};
		float[] t1 = {a * 0.55f, 0.3f, b + 1.3f};
		float[] t2 = {-a * 0.05f, 1.05f, b + 0.98f};
		float[] t3 = {-a * 0.5f, 1.5f, b + 0.5f};
		tube(m, new float[][] {t0, t1}, new float[][] {{0.95f, 0.75f}, {0.56f, 0.52f}}, palm, Shape.DOME, Shape.DOME, null);
		tube(m, new float[][] {t1, t2}, new float[][] {{0.52f, 0.48f}, {0.47f, 0.44f}}, palm, Shape.DOME, Shape.DOME, null);
		tube(m, new float[][] {t2, t3}, new float[][] {{0.45f, 0.42f}, {0.37f, 0.34f}}, palm, Shape.DOME, Shape.DOME, null);
		float[] fore = FOREARM_HAND;
		float[] wrist0 = body[slabs - 2];
		float[] wrist = Shape.add(body[slabs - 1], Shape.scale(fore, 0.3f));
		float cuffLength = design == 6 ? 3.4f : 2.4f;
		float[] cuff0 = Shape.add(wrist, Shape.scale(fore, 0.6f));
		float[] cuff1 = Shape.add(wrist, Shape.scale(fore, cuffLength));
		tube(m, new float[][] {wrist0, wrist, cuff0}, new float[][] {{1.85f, 0.8f}, {1.45f, 0.85f}, {1.5f, 1.0f}}, skin, Shape.OPEN, Shape.OPEN, UP);
		int cuff = bare ? KnifeShading.SLEEVE : KnifeShading.CUFF;
		tube(m, new float[][] {cuff0, cuff1}, new float[][] {{1.7f, 1.15f}, {1.75f, 1.2f}}, cuff, Shape.CAP, Shape.CAP, UP);
		tube(m, new float[][] {Shape.sub(cuff1, Shape.scale(fore, 0.2f)), Shape.add(cuff1, Shape.scale(fore, 14))},
				new float[][] {{1.85f, 1.35f}, {2.4f, 2.0f}}, KnifeShading.SLEEVE, Shape.CAP, Shape.OPEN, UP);
		if (design == 4) {
			for (float f : new float[] {0.15f, 0.55f, 0.95f}) {
				float[] c = Shape.add(wrist, Shape.scale(fore, 0.6f + f * (cuffLength - 0.6f)));
				tube(m, new float[][] {Shape.sub(c, Shape.scale(fore, 0.2f)), Shape.add(c, Shape.scale(fore, 0.2f))},
						new float[][] {{1.82f, 1.27f}, {1.82f, 1.27f}}, KnifeShading.WRAP, Shape.CAP, Shape.CAP, UP);
			}
			float[] c = body[3];
			tube(m, new float[][] {Shape.sub(c, new float[] {0, 0, 0.3f}), Shape.add(c, new float[] {0, 0, 0.3f})},
					new float[][] {{1.98f, 0.9f}, {1.96f, 0.9f}}, KnifeShading.WRAP, Shape.CAP, Shape.CAP, UP);
		}
		if (design == 7) {
			float[] c = Shape.add(wrist, Shape.scale(fore, 1.1f));
			tube(m, new float[][] {Shape.sub(c, Shape.scale(fore, 0.28f)), Shape.add(c, Shape.scale(fore, 0.28f))},
					new float[][] {{1.82f, 1.27f}, {1.82f, 1.27f}}, KnifeShading.GLOVE_ACCENT, Shape.CAP, Shape.CAP, UP);
		}
		return m;
	}

	/** A tube through {@code points} with an elliptical cross-section ({rx, rz} at each point), its X radius along {@code side} (or any way, if null). */
	private static void tube(Shape m, float[][] points, float[][] radii, int material, int startEnd, int endEnd, float[] side) {
		int n = points.length;
		Shape.Ring[] rings = new Shape.Ring[n];
		for (int i = 0; i < n; i++) {
			float[] a = points[Math.max(0, i - 1)];
			float[] b = points[Math.min(n - 1, i + 1)];
			float[] t = Shape.normalize(Shape.sub(b, a));
			rings[i] = new Shape.Ring(points[i][0], points[i][1], points[i][2], t[0], t[1], t[2], radii[i][0], radii[i][1], side);
		}
		m.loft(rings, 16, 0.1f, material, startEnd, endEnd, false);
	}
}
