package dev.threevhy.client.gui;

import dev.threevhy.client.Draw;
import dev.threevhy.client.ThreevhyClient;
import dev.threevhy.client.compat.Compat;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Smooth, anti-aliased shapes and icons for the 67-style menus (the Panels menu, the Music HUD and
 * the menu logo): rounded boxes, outlines, circles, rings and line icons. They're drawn in the
 * screen's real pixels whatever the GUI scale, so edges stay clean instead of stepping in GUI-pixel
 * blocks. Each curved piece is a small white mask texture, worked out once per pixel size (with
 * 4x4 samples per pixel for soft edges) and tinted when drawn; straight parts are plain fills.
 */
public final class Smooth {
	private static final int SAMPLES = 4;
	private static final Map<String, Identifier> MASKS = new HashMap<>();

	private Smooth() {
	}

	/** Whether a point (in pixels, from the mask's top left) is inside a shape. */
	@FunctionalInterface
	private interface Shape {
		boolean inside(double x, double y);
	}

	/** Real screen pixels per unit where {@code d} draws right now. */
	public static float density(Draw d) {
		return d.poseScale() * (float) Minecraft.getInstance().getWindow().getGuiScale();
	}

	/** A filled box with smooth round corners. */
	public static void roundRect(Draw d, float x1, float y1, float x2, float y2, float radius, int color) {
		float k = density(d);
		if (k <= 0 || x2 <= x1 || y2 <= y1) return;
		d.push();
		d.scale(1f / k);
		int left = Math.round(x1 * k);
		int top = Math.round(y1 * k);
		int right = Math.round(x2 * k);
		int bottom = Math.round(y2 * k);
		int r = Math.min(Math.round(radius * k), Math.min((right - left) / 2, (bottom - top) / 2));
		Identifier disc = r >= 1 ? disc(r) : null;
		if (disc == null) {
			d.fill(left, top, right, bottom, color);
		} else {
			int size = 2 * r;
			d.fill(left + r, top, right - r, bottom, color);
			d.fill(left, top + r, left + r, bottom - r, color);
			d.fill(right - r, top + r, right, bottom - r, color);
			d.mask(disc, left, top, 0, 0, r, r, size, size, color);
			d.mask(disc, right - r, top, r, 0, r, r, size, size, color);
			d.mask(disc, left, bottom - r, 0, r, r, r, size, size, color);
			d.mask(disc, right - r, bottom - r, r, r, r, r, size, size, color);
		}
		d.pop();
	}

	/** A smooth outline {@code thickness} units wide, inside the box's edge. */
	public static void roundOutline(Draw d, float x1, float y1, float x2, float y2, float radius, float thickness, int color) {
		float k = density(d);
		if (k <= 0 || x2 <= x1 || y2 <= y1) return;
		d.push();
		d.scale(1f / k);
		int left = Math.round(x1 * k);
		int top = Math.round(y1 * k);
		int right = Math.round(x2 * k);
		int bottom = Math.round(y2 * k);
		int t = Math.max(1, Math.round(thickness * k));
		int r = Math.max(t, Math.min(Math.round(radius * k), Math.min((right - left) / 2, (bottom - top) / 2)));
		Identifier ring = ring(r, t);
		if (ring == null) {
			r = 0;
		} else {
			int size = 2 * r;
			d.mask(ring, left, top, 0, 0, r, r, size, size, color);
			d.mask(ring, right - r, top, r, 0, r, r, size, size, color);
			d.mask(ring, left, bottom - r, 0, r, r, r, size, size, color);
			d.mask(ring, right - r, bottom - r, r, r, r, r, size, size, color);
		}
		d.fill(left + r, top, right - r, top + t, color);
		d.fill(left + r, bottom - t, right - r, bottom, color);
		d.fill(left, top + Math.max(r, t), left + t, bottom - Math.max(r, t), color);
		d.fill(right - t, top + Math.max(r, t), right, bottom - Math.max(r, t), color);
		d.pop();
	}

	public static void circle(Draw d, float cx, float cy, float radius, int color) {
		roundRect(d, cx - radius, cy - radius, cx + radius, cy + radius, radius, color);
	}

	public static void ring(Draw d, float cx, float cy, float radius, float thickness, int color) {
		roundOutline(d, cx - radius, cy - radius, cx + radius, cy + radius, radius, thickness, color);
	}

	/**
	 * Text with a soft glow in its own colour, like 67's HUD: faint copies of it nudged out in two
	 * rings around it (sized to the text), then the text itself on top.
	 */
	public static void glowText(Draw d, Component text, float x, float y, float scale, int color) {
		int rgb = color & 0xFFFFFF;
		float[][] rings = {{0.8f * scale, 0.14f}, {1.6f * scale, 0.06f}};
		for (float[] ring : rings) {
			int alpha = Math.max(4, Math.round(((color >>> 24) & 255) * ring[1]));
			for (int i = 0; i < 8; i++) {
				double angle = i * Math.PI / 4;
				d.push();
				d.translate(x + (float) Math.cos(angle) * ring[0], y + (float) Math.sin(angle) * ring[0]);
				d.scale(scale);
				d.text(text, 0, 0, alpha << 24 | rgb, false);
				d.pop();
			}
		}
		d.push();
		d.translate(x, y);
		d.scale(scale);
		d.text(text, 0, 0, color, false);
		d.pop();
	}

	/** Covers a square's corners with {@code color}, so a picture under it looks rounded. */
	public static void cornerCovers(Draw d, float x1, float y1, float x2, float y2, float radius, int color) {
		float k = density(d);
		if (k <= 0) return;
		d.push();
		d.scale(1f / k);
		int left = Math.round(x1 * k);
		int top = Math.round(y1 * k);
		int right = Math.round(x2 * k);
		int bottom = Math.round(y2 * k);
		int r = Math.max(1, Math.round(radius * k));
		Identifier cover = mask("cover_" + r, 2 * r, 2 * r, (x, y) -> sq(x - r) + sq(y - r) > (double) r * r);
		if (cover != null) {
			int size = 2 * r;
			d.mask(cover, left, top, 0, 0, r, r, size, size, color);
			d.mask(cover, right - r, top, r, 0, r, r, size, size, color);
			d.mask(cover, left, bottom - r, 0, r, r, r, size, size, color);
			d.mask(cover, right - r, bottom - r, r, r, r, r, size, size, color);
		}
		d.pop();
	}

	/**
	 * A line icon, {@code size} units square: chevron_down, chevron_right, check, cross, play,
	 * pause, prev, next, speaker, speaker_muted, search, grid, target, eye, sliders or spark.
	 */
	public static void icon(Draw d, String name, float x, float y, float size, int color) {
		float k = density(d);
		if (k <= 0) return;
		int s = Math.max(2, Math.round(size * k));
		Identifier mask = mask("icon_" + name + "_" + s, s, s, iconShape(name, s));
		if (mask == null) return;
		d.push();
		d.scale(1f / k);
		d.mask(mask, Math.round(x * k), Math.round(y * k), 0, 0, s, s, s, s, color);
		d.pop();
	}

	private static Identifier disc(int r) {
		return mask("disc_" + r, 2 * r, 2 * r, (x, y) -> sq(x - r) + sq(y - r) <= (double) r * r);
	}

	private static Identifier ring(int r, int t) {
		double inner = Math.max(0, r - t);
		return mask("ring_" + r + "_" + t, 2 * r, 2 * r, (x, y) -> {
			double d2 = sq(x - r) + sq(y - r);
			return d2 <= (double) r * r && d2 >= inner * inner;
		});
	}

	private static Shape iconShape(String name, int s) {
		double w = 0.1;
		return switch (name) {
			case "chevron_down" -> (x, y) -> line(x / s, y / s, w, 0.22, 0.36, 0.5, 0.64, 0.78, 0.36);
			case "chevron_right" -> (x, y) -> line(x / s, y / s, w, 0.38, 0.2, 0.66, 0.5, 0.38, 0.8);
			case "check" -> (x, y) -> line(x / s, y / s, 0.13, 0.2, 0.52, 0.42, 0.74, 0.8, 0.3);
			case "cross" -> (x, y) -> line(x / s, y / s, w, 0.28, 0.28, 0.72, 0.72) || line(x / s, y / s, w, 0.72, 0.28, 0.28, 0.72);
			case "play" -> (x, y) -> triangle(x / s, y / s, 0.3, 0.18, 0.3, 0.82, 0.84, 0.5);
			case "pause" -> (x, y) -> box(x / s, y / s, 0.26, 0.2, 0.42, 0.8) || box(x / s, y / s, 0.58, 0.2, 0.74, 0.8);
			case "prev" -> (x, y) -> box(x / s, y / s, 0.16, 0.2, 0.28, 0.8) || triangle(x / s, y / s, 0.84, 0.2, 0.84, 0.8, 0.3, 0.5);
			case "next" -> (x, y) -> box(x / s, y / s, 0.72, 0.2, 0.84, 0.8) || triangle(x / s, y / s, 0.16, 0.2, 0.16, 0.8, 0.7, 0.5);
			case "speaker" -> (x, y) -> speaker(x / s, y / s) || wave(x / s, y / s, 0.17, 0.25) || wave(x / s, y / s, 0.32, 0.4);
			case "speaker_muted" -> (x, y) -> speaker(x / s, y / s) || line(x / s, y / s, 0.08, 0.68, 0.36, 0.92, 0.64)
					|| line(x / s, y / s, 0.08, 0.92, 0.36, 0.68, 0.64);
			case "search" -> (x, y) -> {
				double d = Math.hypot(x / s - 0.42, y / s - 0.42);
				return (d <= 0.32 && d >= 0.21) || line(x / s, y / s, 0.11, 0.65, 0.65, 0.88, 0.88);
			};
			case "grid" -> (x, y) -> frame(x / s, y / s, 0.06, 0.06, 0.44, 0.44) || frame(x / s, y / s, 0.56, 0.06, 0.94, 0.44)
					|| frame(x / s, y / s, 0.06, 0.56, 0.44, 0.94) || frame(x / s, y / s, 0.56, 0.56, 0.94, 0.94);
			case "target" -> (x, y) -> {
				double d = Math.hypot(x / s - 0.5, y / s - 0.5);
				return (d <= 0.46 && d >= 0.35) || d <= 0.15;
			};
			case "eye" -> (x, y) -> {
				double u = x / s;
				double v = y / s;
				boolean outer = Math.hypot(u - 0.5, v - 1.08) <= 0.8 && Math.hypot(u - 0.5, v + 0.08) <= 0.8;
				boolean inner = Math.hypot(u - 0.5, v - 1.2) <= 0.8 && Math.hypot(u - 0.5, v + 0.2) <= 0.8;
				return (outer && !inner) || Math.hypot(u - 0.5, v - 0.5) <= 0.14;
			};
			case "sliders" -> (x, y) -> line(x / s, y / s, 0.09, 0.08, 0.24, 0.92, 0.24) || line(x / s, y / s, 0.09, 0.08, 0.5, 0.92, 0.5)
					|| line(x / s, y / s, 0.09, 0.08, 0.76, 0.92, 0.76) || Math.hypot(x / s - 0.66, y / s - 0.24) <= 0.12
					|| Math.hypot(x / s - 0.32, y / s - 0.5) <= 0.12 || Math.hypot(x / s - 0.58, y / s - 0.76) <= 0.12;
			case "spark" -> (x, y) -> Math.pow(Math.abs(x / s - 0.5), 2 / 3.0) + Math.pow(Math.abs(y / s - 0.5), 2 / 3.0)
					<= Math.pow(0.47, 2 / 3.0);
			default -> (x, y) -> false;
		};
	}

	private static boolean speaker(double u, double v) {
		if (box(u, v, 0.06, 0.36, 0.26, 0.64)) return true;
		if (u < 0.26 || u > 0.5) return false;
		double spread = 0.14 + (u - 0.26) / 0.24 * 0.22;
		return Math.abs(v - 0.5) <= spread;
	}

	private static boolean wave(double u, double v, double inner, double outer) {
		double d = Math.hypot(u - 0.5, v - 0.5);
		return u > 0.55 && d >= inner && d <= outer && Math.abs(v - 0.5) <= (u - 0.5) * 1.3;
	}

	private static boolean box(double u, double v, double x1, double y1, double x2, double y2) {
		return u >= x1 && u <= x2 && v >= y1 && v <= y2;
	}

	private static boolean frame(double u, double v, double x1, double y1, double x2, double y2) {
		double t = 0.09;
		return box(u, v, x1, y1, x2, y2) && !box(u, v, x1 + t, y1 + t, x2 - t, y2 - t);
	}

	private static boolean triangle(double u, double v, double ax, double ay, double bx, double by, double cx, double cy) {
		double d1 = (u - bx) * (ay - by) - (ax - bx) * (v - by);
		double d2 = (u - cx) * (by - cy) - (bx - cx) * (v - cy);
		double d3 = (u - ax) * (cy - ay) - (cx - ax) * (v - ay);
		boolean negative = d1 < 0 || d2 < 0 || d3 < 0;
		boolean positive = d1 > 0 || d2 > 0 || d3 > 0;
		return !(negative && positive);
	}

	/** Whether (u, v) is within {@code width / 2} of the polyline through the given points. */
	private static boolean line(double u, double v, double width, double... points) {
		for (int i = 0; i + 3 < points.length; i += 2) {
			if (segmentDistance(u, v, points[i], points[i + 1], points[i + 2], points[i + 3]) <= width / 2) return true;
		}
		return false;
	}

	private static double segmentDistance(double u, double v, double ax, double ay, double bx, double by) {
		double dx = bx - ax;
		double dy = by - ay;
		double t = Math.max(0, Math.min(1, ((u - ax) * dx + (v - ay) * dy) / (dx * dx + dy * dy)));
		return Math.hypot(u - (ax + t * dx), v - (ay + t * dy));
	}

	private static double sq(double value) {
		return value * value;
	}

	/** The mask texture for {@code key}, made (and kept) on first use; null if it couldn't be made. */
	private static Identifier mask(String key, int w, int h, Shape shape) {
		if (MASKS.containsKey(key)) return MASKS.get(key);
		byte[] alpha = new byte[w * h];
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int hits = 0;
				for (int sy = 0; sy < SAMPLES; sy++) {
					for (int sx = 0; sx < SAMPLES; sx++) {
						if (shape.inside(x + (sx + 0.5) / SAMPLES, y + (sy + 0.5) / SAMPLES)) hits++;
					}
				}
				alpha[y * w + x] = (byte) Math.round(hits * 255.0 / (SAMPLES * SAMPLES));
			}
		}
		Identifier id = Identifier.fromNamespaceAndPath(ThreevhyClient.MOD_ID, "smooth/" + key);
		Identifier made = Compat.uploadTexture(id, png(w, h, alpha)) != null ? id : null;
		MASKS.put(key, made);
		return made;
	}

	/** A white RGBA PNG whose alpha is {@code alpha}. */
	private static byte[] png(int w, int h, byte[] alpha) {
		try {
			ByteArrayOutputStream rows = new ByteArrayOutputStream();
			for (int y = 0; y < h; y++) {
				rows.write(0);
				for (int x = 0; x < w; x++) {
					rows.write(255);
					rows.write(255);
					rows.write(255);
					rows.write(alpha[y * w + x]);
				}
			}
			Deflater deflater = new Deflater();
			deflater.setInput(rows.toByteArray());
			deflater.finish();
			ByteArrayOutputStream packed = new ByteArrayOutputStream();
			byte[] buffer = new byte[8192];
			while (!deflater.finished()) packed.write(buffer, 0, deflater.deflate(buffer));
			deflater.end();

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			out.write(new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'});
			ByteArrayOutputStream header = new ByteArrayOutputStream();
			writeInt(header, w);
			writeInt(header, h);
			header.write(new byte[] {8, 6, 0, 0, 0});
			chunk(out, "IHDR", header.toByteArray());
			chunk(out, "IDAT", packed.toByteArray());
			chunk(out, "IEND", new byte[0]);
			return out.toByteArray();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private static void chunk(ByteArrayOutputStream out, String type, byte[] data) throws IOException {
		writeInt(out, data.length);
		byte[] name = type.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
		out.write(name);
		out.write(data);
		CRC32 crc = new CRC32();
		crc.update(name);
		crc.update(data);
		writeInt(out, (int) crc.getValue());
	}

	private static void writeInt(ByteArrayOutputStream out, int value) {
		out.write(value >>> 24);
		out.write(value >>> 16);
		out.write(value >>> 8);
		out.write(value);
	}
}
