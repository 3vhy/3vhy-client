package dev.threevhy.client.ui;

import dev.threevhy.client.ThreevhyClient;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The line icons in 3vhy's menus and HUD, and the 3vhy logo mark: Lucide's icons (ISC licence) and
 * brand.txt in assets/threevhy/icons, read from their SVG path data and drawn into an anti-aliased mask
 * at exactly the pixel size they're shown at, so they're as sharp as on the website at any GUI scale.
 * Masks are made once per size and kept by {@link Paint}.
 */
public final class IconShapes {
	/** Lucide icons are drawn on a 24 x 24 grid with 2-unit strokes. */
	private static final float GRID = 24f;
	private static final float STROKE = 2f;
	/** Line segments a curve is split into. */
	private static final int CURVE_STEPS = 10;

	/** One icon: its outlines as polylines {x0, y0, x1, y1, ...}, and which of them are filled. */
	private record Icon(List<float[]> lines, List<Boolean> filled) {
	}

	private static Map<String, Icon> icons = null;

	private IconShapes() {
	}

	public static boolean exists(String name) {
		return load().containsKey(name);
	}

	/**
	 * An icon as a white mask {@code size} pixels square: alpha is how much of each pixel the icon
	 * covers. {@code weight} scales the stroke (1 is Lucide's), and {@code solid} fills every closed shape
	 * too, for a solid heart or play button. Unknown names give an empty mask.
	 */
	public static int[] mask(String name, int size, float weight, boolean solid) {
		int[] pixels = new int[size * size];
		Icon icon = load().get(name);
		if (icon == null) return pixels;
		float scale = size / GRID;
		float half = Math.max(0.62f, STROKE * weight * scale / 2f);
		for (int py = 0; py < size; py++) {
			for (int px = 0; px < size; px++) {
				float x = px + 0.5f;
				float y = py + 0.5f;
				float coverage = 0;
				for (int i = 0; i < icon.lines.size() && coverage < 1; i++) {
					float[] line = icon.lines.get(i);
					float d = distance(line, x / scale, y / scale) * scale;
					coverage = Math.max(coverage, clamp(half + 0.5f - d));
					if ((solid || icon.filled.get(i)) && closed(line)) {
						boolean inside = inside(line, x / scale, y / scale);
						coverage = Math.max(coverage, inside ? 1f : clamp(0.5f - d));
					}
				}
				pixels[py * size + px] = Math.round(coverage * 255) << 24 | 0xFFFFFF;
			}
		}
		return pixels;
	}

	private static float clamp(float value) {
		return Math.max(0f, Math.min(1f, value));
	}

	private static boolean closed(float[] line) {
		int n = line.length;
		return n >= 6 && Math.abs(line[0] - line[n - 2]) < 0.01f && Math.abs(line[1] - line[n - 1]) < 0.01f;
	}

	private static float distance(float[] line, float x, float y) {
		float best = Float.MAX_VALUE;
		for (int i = 0; i + 3 < line.length; i += 2) {
			float ax = line[i];
			float ay = line[i + 1];
			float dx = line[i + 2] - ax;
			float dy = line[i + 3] - ay;
			float len = dx * dx + dy * dy;
			float t = len == 0 ? 0 : Math.max(0, Math.min(1, ((x - ax) * dx + (y - ay) * dy) / len));
			float ex = x - (ax + t * dx);
			float ey = y - (ay + t * dy);
			best = Math.min(best, ex * ex + ey * ey);
		}
		if (line.length == 2) best = (x - line[0]) * (x - line[0]) + (y - line[1]) * (y - line[1]);
		return (float) Math.sqrt(best);
	}

	/** Even-odd test against a closed polyline. */
	private static boolean inside(float[] line, float x, float y) {
		boolean in = false;
		int n = line.length / 2;
		for (int i = 0, j = n - 1; i < n; j = i++) {
			float xi = line[2 * i];
			float yi = line[2 * i + 1];
			float xj = line[2 * j];
			float yj = line[2 * j + 1];
			if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) in = !in;
		}
		return in;
	}

	private static synchronized Map<String, Icon> load() {
		if (icons != null) return icons;
		Map<String, Icon> loaded = new HashMap<>();
		for (String file : new String[] {"lucide.txt", "brand.txt"}) read(file, loaded);
		icons = loaded;
		return icons;
	}

	private static void read(String file, Map<String, Icon> loaded) {
		try (InputStream in = IconShapes.class.getResourceAsStream("/assets/threevhy/icons/" + file)) {
			if (in == null) throw new IOException(file + " is missing");
			BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			String row;
			while ((row = reader.readLine()) != null) {
				if (row.isBlank() || row.startsWith("#")) continue;
				String[] parts = row.split("\\|");
				List<float[]> lines = new ArrayList<>();
				List<Boolean> filled = new ArrayList<>();
				for (int i = 1; i < parts.length; i++) {
					boolean fill = parts[i].startsWith("f:");
					for (float[] line : parsePath(fill ? parts[i].substring(2) : parts[i])) {
						lines.add(line);
						filled.add(fill);
					}
				}
				loaded.put(parts[0], new Icon(lines, filled));
			}
		} catch (IOException | RuntimeException e) {
			ThreevhyClient.LOG.warn("Couldn't read 3vhy's icons from {}", file, e);
		}
	}

	/** SVG path data to polylines, one per subpath; curves and arcs are flattened. */
	static List<float[]> parsePath(String d) {
		List<float[]> out = new ArrayList<>();
		PathReader r = new PathReader(d);
		List<Float> current = new ArrayList<>();
		float x = 0;
		float y = 0;
		float startX = 0;
		float startY = 0;
		float ctrlX = 0;
		float ctrlY = 0;
		char last = ' ';
		char command = ' ';
		while (r.more()) {
			char next = r.command();
			if (next != 0) {
				command = next;
			} else if (command == 'M') {
				command = 'L';
			} else if (command == 'm') {
				command = 'l';
			}
			boolean rel = Character.isLowerCase(command);
			float ox = rel ? x : 0;
			float oy = rel ? y : 0;
			switch (Character.toUpperCase(command)) {
				case 'M' -> {
					flush(current, out);
					x = ox + r.number();
					y = oy + r.number();
					startX = x;
					startY = y;
					add(current, x, y);
				}
				case 'L' -> {
					x = ox + r.number();
					y = oy + r.number();
					add(current, x, y);
				}
				case 'H' -> {
					x = (rel ? x : 0) + r.number();
					add(current, x, y);
				}
				case 'V' -> {
					y = (rel ? y : 0) + r.number();
					add(current, x, y);
				}
				case 'C', 'S' -> {
					float c1x;
					float c1y;
					if (Character.toUpperCase(command) == 'C') {
						c1x = ox + r.number();
						c1y = oy + r.number();
					} else {
						boolean smooth = "CcSs".indexOf(last) >= 0;
						c1x = smooth ? 2 * x - ctrlX : x;
						c1y = smooth ? 2 * y - ctrlY : y;
					}
					float c2x = ox + r.number();
					float c2y = oy + r.number();
					float ex = ox + r.number();
					float ey = oy + r.number();
					for (int i = 1; i <= CURVE_STEPS; i++) {
						float t = i / (float) CURVE_STEPS;
						float u = 1 - t;
						add(current, u * u * u * x + 3 * u * u * t * c1x + 3 * u * t * t * c2x + t * t * t * ex,
								u * u * u * y + 3 * u * u * t * c1y + 3 * u * t * t * c2y + t * t * t * ey);
					}
					ctrlX = c2x;
					ctrlY = c2y;
					x = ex;
					y = ey;
				}
				case 'Q', 'T' -> {
					float cx;
					float cy;
					if (Character.toUpperCase(command) == 'Q') {
						cx = ox + r.number();
						cy = oy + r.number();
					} else {
						boolean smooth = "QqTt".indexOf(last) >= 0;
						cx = smooth ? 2 * x - ctrlX : x;
						cy = smooth ? 2 * y - ctrlY : y;
					}
					float ex = ox + r.number();
					float ey = oy + r.number();
					for (int i = 1; i <= CURVE_STEPS; i++) {
						float t = i / (float) CURVE_STEPS;
						float u = 1 - t;
						add(current, u * u * x + 2 * u * t * cx + t * t * ex, u * u * y + 2 * u * t * cy + t * t * ey);
					}
					ctrlX = cx;
					ctrlY = cy;
					x = ex;
					y = ey;
				}
				case 'A' -> {
					float rx = Math.abs(r.number());
					float ry = Math.abs(r.number());
					float rotation = r.number();
					boolean large = r.flag();
					boolean sweep = r.flag();
					float ex = ox + r.number();
					float ey = oy + r.number();
					arc(current, x, y, rx, ry, rotation, large, sweep, ex, ey);
					x = ex;
					y = ey;
				}
				case 'Z' -> {
					add(current, startX, startY);
					flush(current, out);
					x = startX;
					y = startY;
					add(current, x, y);
				}
				default -> throw new IllegalArgumentException("Unknown path command " + command + " in " + d);
			}
			last = command;
		}
		flush(current, out);
		return out;
	}

	private static void add(List<Float> line, float x, float y) {
		line.add(x);
		line.add(y);
	}

	private static void flush(List<Float> line, List<float[]> out) {
		if (line.size() >= 4) {
			float[] points = new float[line.size()];
			for (int i = 0; i < points.length; i++) points[i] = line.get(i);
			out.add(points);
		} else if (line.size() == 2 && !out.isEmpty()) {
			line.clear();
			return;
		}
		line.clear();
	}

	/** An SVG elliptical arc from (x1, y1) to (x2, y2), sampled into the polyline (SVG spec, appendix F.6). */
	private static void arc(List<Float> line, float x1, float y1, float rx, float ry, float rotationDeg, boolean large,
			boolean sweep, float x2, float y2) {
		if (rx == 0 || ry == 0) {
			add(line, x2, y2);
			return;
		}
		double phi = Math.toRadians(rotationDeg);
		double cos = Math.cos(phi);
		double sin = Math.sin(phi);
		double dx = (x1 - x2) / 2.0;
		double dy = (y1 - y2) / 2.0;
		double x1p = cos * dx + sin * dy;
		double y1p = -sin * dx + cos * dy;
		double lambda = (x1p * x1p) / (rx * rx) + (y1p * y1p) / (ry * ry);
		double rxs = rx;
		double rys = ry;
		if (lambda > 1) {
			rxs *= Math.sqrt(lambda);
			rys *= Math.sqrt(lambda);
		}
		double num = rxs * rxs * rys * rys - rxs * rxs * y1p * y1p - rys * rys * x1p * x1p;
		double den = rxs * rxs * y1p * y1p + rys * rys * x1p * x1p;
		double coef = den == 0 ? 0 : Math.sqrt(Math.max(0, num / den)) * (large == sweep ? -1 : 1);
		double cxp = coef * rxs * y1p / rys;
		double cyp = -coef * rys * x1p / rxs;
		double cx = cos * cxp - sin * cyp + (x1 + x2) / 2.0;
		double cy = sin * cxp + cos * cyp + (y1 + y2) / 2.0;
		double start = Math.atan2((y1p - cyp) / rys, (x1p - cxp) / rxs);
		double end = Math.atan2((-y1p - cyp) / rys, (-x1p - cxp) / rxs);
		double delta = end - start;
		if (sweep && delta < 0) delta += 2 * Math.PI;
		if (!sweep && delta > 0) delta -= 2 * Math.PI;
		int steps = Math.max(4, (int) Math.ceil(Math.abs(delta) / (Math.PI / 16)));
		for (int i = 1; i <= steps; i++) {
			double a = start + delta * i / steps;
			double ux = rxs * Math.cos(a);
			double uy = rys * Math.sin(a);
			add(line, (float) (cos * ux - sin * uy + cx), (float) (sin * ux + cos * uy + cy));
		}
	}

	/** Reads numbers and command letters out of SVG path data. */
	private static final class PathReader {
		private final String s;
		private int i = 0;

		PathReader(String s) {
			this.s = s;
		}

		private void skip() {
			while (i < s.length() && (s.charAt(i) == ' ' || s.charAt(i) == ',' || s.charAt(i) == '\n' || s.charAt(i) == '\t')) i++;
		}

		boolean more() {
			skip();
			return i < s.length();
		}

		/** The next command letter, or 0 when the next thing is a number (an implied repeat). */
		char command() {
			skip();
			char c = s.charAt(i);
			if (Character.isLetter(c) && c != 'e' && c != 'E') {
				i++;
				return c;
			}
			return 0;
		}

		boolean flag() {
			skip();
			char c = s.charAt(i++);
			return c == '1';
		}

		float number() {
			skip();
			int start = i;
			if (i < s.length() && (s.charAt(i) == '-' || s.charAt(i) == '+')) i++;
			boolean dot = false;
			while (i < s.length()) {
				char c = s.charAt(i);
				if (Character.isDigit(c)) {
					i++;
				} else if (c == '.' && !dot) {
					dot = true;
					i++;
				} else if ((c == 'e' || c == 'E') && i + 1 < s.length()
						&& (Character.isDigit(s.charAt(i + 1)) || s.charAt(i + 1) == '-')) {
					i += 2;
				} else {
					break;
				}
			}
			return Float.parseFloat(s.substring(start, i));
		}
	}
}
