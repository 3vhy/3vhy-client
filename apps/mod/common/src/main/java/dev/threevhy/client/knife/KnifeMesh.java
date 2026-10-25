package dev.threevhy.client.knife;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Arrays;

/**
 * One rigid piece of a knife as quads, built once from simple shapes: a blade from its spine and edge
 * outlines (a flat grind, then a bevel down to a sharp edge), lofts for handles, guards and pommels, rings
 * and boxes. Units are pixels (a sixteenth of a block); the knife's grip is at the origin, the blade points
 * up (+Y), its edge faces +X and its flats face ±Z. Every quad has a {@link Mat} that picks its texture
 * and colour when drawn, so one mesh serves every finish.
 */
final class KnifeMesh {
	/** What a quad is made of: its place in the texture atlas and how it's coloured. */
	enum Mat {
		BLADE(0, 0, 32, 16, false),
		EDGE(32, 0, 64, 16, false),
		METAL(32, 32, 64, 48, false),
		HANDLE(32, 16, 64, 32, false),
		GRIP(0, 16, 32, 32, false),
		WRAP(0, 32, 32, 48, false),
		ACCENT(32, 32, 64, 48, false),
		CARBON(0, 48, 32, 64, false),
		ENERGY_CORE(32, 48, 64, 64, true),
		ENERGY_GLOW(32, 48, 64, 64, true);

		final float u0;
		final float v0;
		final float u1;
		final float v1;
		/** Drawn glowing and see-through, in its own layer. */
		final boolean glow;

		Mat(int u0, int v0, int u1, int v1, boolean glow) {
			this.u0 = (u0 + 0.5f) / KnifeAtlas.SIZE;
			this.v0 = (v0 + 0.5f) / KnifeAtlas.SIZE;
			this.u1 = (u1 - 0.5f) / KnifeAtlas.SIZE;
			this.v1 = (v1 - 0.5f) / KnifeAtlas.SIZE;
			this.glow = glow;
		}
	}

	/** One cross-section of a blade: the spine and edge points and the blade's thickness at the spine. */
	record Station(float sx, float sy, float ex, float ey, float thickness) {
	}

	/** One ring of a loft: its centre, the direction it faces and its two radii. */
	record Ring(float x, float y, float z, float ax, float ay, float az, float rx, float rz) {
		static Ring along(float x, float y, float z, float rx, float rz) {
			return new Ring(x, y, z, 0, 1, 0, rx, rz);
		}
	}

	private static final int STRIDE = 8;
	private float[] data = new float[STRIDE * 4 * 64];
	private Mat[] mats = new Mat[64];
	private int quads = 0;

	int quads() {
		return quads;
	}

	/**
	 * A quad from four corners {x, y, z, u, v} (u and v 0..1 within the material's patch). Its normal points
	 * the way of ({@code hx}, {@code hy}, {@code hz}), so it's lit from the right side whichever way the
	 * corners wind.
	 */
	void quad(Mat mat, float[] a, float[] b, float[] c, float[] d, float hx, float hy, float hz) {
		float ux = c[0] - a[0];
		float uy = c[1] - a[1];
		float uz = c[2] - a[2];
		float vx = d[0] - b[0];
		float vy = d[1] - b[1];
		float vz = d[2] - b[2];
		float nx = uy * vz - uz * vy;
		float ny = uz * vx - ux * vz;
		float nz = ux * vy - uy * vx;
		float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
		if (length < 1e-6f) {
			nx = hx;
			ny = hy;
			nz = hz;
			length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
			if (length < 1e-6f) return;
		}
		nx /= length;
		ny /= length;
		nz /= length;
		if (nx * hx + ny * hy + nz * hz < 0) {
			nx = -nx;
			ny = -ny;
			nz = -nz;
		}
		if (quads == mats.length) {
			mats = Arrays.copyOf(mats, quads * 2);
			data = Arrays.copyOf(data, data.length * 2);
		}
		int at = quads * STRIDE * 4;
		for (float[] corner : new float[][] {a, b, c, d}) {
			data[at] = corner[0];
			data[at + 1] = corner[1];
			data[at + 2] = corner[2];
			data[at + 3] = mat.u0 + (mat.u1 - mat.u0) * clamp01(corner[3]);
			data[at + 4] = mat.v0 + (mat.v1 - mat.v0) * clamp01(corner[4]);
			data[at + 5] = nx;
			data[at + 6] = ny;
			data[at + 7] = nz;
			at += STRIDE;
		}
		mats[quads] = mat;
		quads++;
	}

	private static float clamp01(float v) {
		return Math.max(0, Math.min(1, v));
	}

	private static float[] p(float x, float y, float z, float u, float v) {
		return new float[] {x, y, z, u, v};
	}

	/**
	 * A blade through {@code stations}, base first: the flats run from the spine to a grind line
	 * {@code grind} of the way across, then bevel down to a sharp edge. With {@code spine} false the spine
	 * face is left out (for the two halves of a double-edged blade, which meet there).
	 */
	void blade(Station[] stations, Mat flat, Mat bevel, float grind, boolean spine) {
		int n = stations.length;
		for (int i = 0; i + 1 < n; i++) {
			Station a = stations[i];
			Station b = stations[i + 1];
			float ua = i / (float) (n - 1);
			float ub = (i + 1) / (float) (n - 1);
			float arx = a.sx + (a.ex - a.sx) * grind;
			float ary = a.sy + (a.ey - a.sy) * grind;
			float brx = b.sx + (b.ex - b.sx) * grind;
			float bry = b.sy + (b.ey - b.sy) * grind;
			float at = a.thickness / 2f;
			float bt = b.thickness / 2f;
			float ar = at * 0.92f;
			float br = bt * 0.92f;
			float edge = 0.03f;
			float ox = (a.ex - a.sx);
			float oy = (a.ey - a.sy);
			for (int side = -1; side <= 1; side += 2) {
				quad(flat, p(a.sx, a.sy, side * at, ua, 0), p(b.sx, b.sy, side * bt, ub, 0), p(brx, bry, side * br, ub, grind),
						p(arx, ary, side * ar, ua, grind), 0, 0, side);
				quad(bevel, p(arx, ary, side * ar, ua, 0), p(brx, bry, side * br, ub, 0), p(b.ex, b.ey, side * edge, ub, 1),
						p(a.ex, a.ey, side * edge, ua, 1), ox * 0.25f, oy * 0.25f, side);
			}
			if (spine) {
				quad(flat, p(a.sx, a.sy, -at, ua, 0), p(b.sx, b.sy, -bt, ub, 0), p(b.sx, b.sy, bt, ub, 0.15f), p(a.sx, a.sy, at, ua, 0.15f), -ox, -oy, 0);
			}
		}
		Station s = stations[0];
		Station next = stations[1];
		float rx = s.sx + (s.ex - s.sx) * grind;
		float ry = s.sy + (s.ey - s.sy) * grind;
		float t = s.thickness / 2f;
		float hx = (s.sx + s.ex - next.sx - next.ex) / 2f;
		float hy = (s.sy + s.ey - next.sy - next.ey) / 2f;
		quad(flat, p(s.sx, s.sy, t, 0, 0), p(rx, ry, t * 0.92f, 0, 0.5f), p(rx, ry, -t * 0.92f, 1, 0.5f), p(s.sx, s.sy, -t, 1, 0), hx, hy, 0);
		quad(flat, p(rx, ry, t * 0.92f, 0, 0.5f), p(s.ex, s.ey, 0.03f, 0, 1), p(s.ex, s.ey, -0.03f, 1, 1), p(rx, ry, -t * 0.92f, 1, 0.5f), hx, hy, 0);
	}

	/**
	 * A tube through {@code rings}, each a rounded cross-section ({@code squareness} 0 for an ellipse, up to
	 * about 0.8 for a rounded box), with the ends capped when asked. Texture u runs around, v along.
	 */
	void loft(Ring[] rings, int segments, float squareness, Mat mat, boolean capStart, boolean capEnd) {
		float[][][] points = new float[rings.length][segments + 1][];
		for (int r = 0; r < rings.length; r++) {
			Ring ring = rings[r];
			float[] frame = frame(ring.ax, ring.ay, ring.az);
			float v = r / (float) Math.max(1, rings.length - 1);
			for (int s = 0; s <= segments; s++) {
				double angle = 2 * Math.PI * s / segments;
				float c = (float) Math.cos(angle);
				float sn = (float) Math.sin(angle);
				float k = 2f / (2f + squareness * 6f);
				float cx = Math.signum(c) * (float) Math.pow(Math.abs(c), k);
				float cz = Math.signum(sn) * (float) Math.pow(Math.abs(sn), k);
				float ox = cx * ring.rx;
				float oz = cz * ring.rz;
				points[r][s] = p(ring.x + frame[0] * ox + frame[3] * oz, ring.y + frame[1] * ox + frame[4] * oz, ring.z + frame[2] * ox + frame[5] * oz,
						s / (float) segments, v);
			}
		}
		for (int r = 0; r + 1 < rings.length; r++) {
			Ring ring = rings[r];
			for (int s = 0; s < segments; s++) {
				float[] a = points[r][s];
				float[] b = points[r][s + 1];
				float[] c = points[r + 1][s + 1];
				float[] d = points[r + 1][s];
				float hx = (a[0] + b[0] + c[0] + d[0]) / 4f - (ring.x + rings[r + 1].x) / 2f;
				float hy = (a[1] + b[1] + c[1] + d[1]) / 4f - (ring.y + rings[r + 1].y) / 2f;
				float hz = (a[2] + b[2] + c[2] + d[2]) / 4f - (ring.z + rings[r + 1].z) / 2f;
				quad(mat, a, b, c, d, hx, hy, hz);
			}
		}
		if (capStart) cap(points[0], rings[0], mat, -1);
		if (capEnd) cap(points[rings.length - 1], rings[rings.length - 1], mat, 1);
	}

	private void cap(float[][] ring, Ring at, Mat mat, int direction) {
		float[] center = p(at.x, at.y, at.z, 0.5f, 0.5f);
		int segments = ring.length - 1;
		for (int s = 0; s < segments; s += 2) {
			float[] b = ring[s];
			float[] c = ring[s + 1];
			float[] d = ring[Math.min(segments, s + 2)];
			quad(mat, center, b, c, d, at.ax * direction, at.ay * direction, at.az * direction);
		}
	}

	/** Two directions across a ring facing (ax, ay, az): {x1, y1, z1, x2, y2, z2}, the first as close to X as it can be. */
	private static float[] frame(float ax, float ay, float az) {
		float length = (float) Math.sqrt(ax * ax + ay * ay + az * az);
		ax /= length;
		ay /= length;
		az /= length;
		float rx = 1;
		float ry = 0;
		float rz = 0;
		if (Math.abs(ax) > 0.9f) {
			rx = 0;
			ry = 1;
		}
		float d = rx * ax + ry * ay + rz * az;
		rx -= d * ax;
		ry -= d * ay;
		rz -= d * az;
		float rl = (float) Math.sqrt(rx * rx + ry * ry + rz * rz);
		rx /= rl;
		ry /= rl;
		rz /= rl;
		float zx = ay * rz - az * ry;
		float zy = az * rx - ax * rz;
		float zz = ax * ry - ay * rx;
		return new float[] {rx, ry, rz, zx, zy, zz};
	}

	/** A ring (torus) around ({@code cx}, {@code cy}, {@code cz}) in the XY plane: {@code radius} to the tube's middle, {@code tube} thick. */
	void ring(float cx, float cy, float cz, float radius, float tube, int around, int segments, Mat mat) {
		Ring[] rings = new Ring[around + 1];
		for (int i = 0; i <= around; i++) {
			double a = 2 * Math.PI * i / around;
			float x = cx + (float) Math.cos(a) * radius;
			float y = cy + (float) Math.sin(a) * radius;
			rings[i] = new Ring(x, y, cz, -(float) Math.sin(a), (float) Math.cos(a), 0, tube, tube);
		}
		loft(rings, segments, 0, mat, false, false);
	}

	/** A box from (x0, y0, z0) to (x1, y1, z1). */
	void box(float x0, float y0, float z0, float x1, float y1, float z1, Mat mat) {
		quad(mat, p(x0, y0, z1, 0, 0), p(x1, y0, z1, 1, 0), p(x1, y1, z1, 1, 1), p(x0, y1, z1, 0, 1), 0, 0, 1);
		quad(mat, p(x0, y0, z0, 0, 0), p(x1, y0, z0, 1, 0), p(x1, y1, z0, 1, 1), p(x0, y1, z0, 0, 1), 0, 0, -1);
		quad(mat, p(x0, y1, z0, 0, 0), p(x1, y1, z0, 1, 0), p(x1, y1, z1, 1, 1), p(x0, y1, z1, 0, 1), 0, 1, 0);
		quad(mat, p(x0, y0, z0, 0, 0), p(x1, y0, z0, 1, 0), p(x1, y0, z1, 1, 1), p(x0, y0, z1, 0, 1), 0, -1, 0);
		quad(mat, p(x1, y0, z0, 0, 0), p(x1, y1, z0, 1, 0), p(x1, y1, z1, 1, 1), p(x1, y0, z1, 0, 1), 1, 0, 0);
		quad(mat, p(x0, y0, z0, 0, 0), p(x0, y1, z0, 1, 0), p(x0, y1, z1, 1, 1), p(x0, y0, z1, 0, 1), -1, 0, 0);
	}

	/**
	 * Draws the quads of one layer ({@code glow} or solid) in {@code pose}: each material's colour comes
	 * from {@code palette}, multiplied by {@code alpha}.
	 */
	void emit(PoseStack.Pose pose, VertexConsumer out, int light, int overlay, Palette palette, boolean glow, float alpha) {
		for (int q = 0; q < quads; q++) {
			Mat mat = mats[q];
			if (mat.glow != glow) continue;
			int color = palette.color(mat);
			int a = Math.round(((color >>> 24) & 255) * alpha);
			if (a <= 0) continue;
			int argb = a << 24 | (color & 0xFFFFFF);
			int at = q * STRIDE * 4;
			for (int v = 0; v < 4; v++) {
				out.addVertex(pose, data[at], data[at + 1], data[at + 2]).setColor(argb).setUv(data[at + 3], data[at + 4]).setOverlay(overlay)
						.setLight(light).setNormal(pose, data[at + 5], data[at + 6], data[at + 7]);
				at += STRIDE;
			}
		}
	}

	/** Whether any quad is in the glowing layer. */
	boolean hasGlow() {
		for (int q = 0; q < quads; q++) if (mats[q].glow) return true;
		return false;
	}

	/** The colour of each material for one knife and finish. */
	interface Palette {
		int color(Mat mat);
	}
}
