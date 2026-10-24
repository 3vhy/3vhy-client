package dev.threevhy.client.ui;

/**
 * Easing curves, solved exactly the way a browser solves CSS {@code cubic-bezier()}, so motion in the
 * game matches the website frame for frame. The three named curves are 3vhy's design tokens.
 */
public final class Ease {
	/** {@code cubic-bezier(.16, 1, .3, 1)}: fast start, long soft landing. Most UI motion uses it. */
	public static final Ease OUT = new Ease(0.16, 1, 0.3, 1);
	/** {@code cubic-bezier(.65, 0, .35, 1)}: slow in and out, for crossfades. */
	public static final Ease IN_OUT = new Ease(0.65, 0, 0.35, 1);
	/** {@code cubic-bezier(.34, 1.56, .64, 1)}: overshoots a little, like a spring. Toggle knobs use it. */
	public static final Ease SPRING = new Ease(0.34, 1.56, 0.64, 1);
	/** Plain {@code ease-in-out}. */
	public static final Ease SINE = new Ease(0.42, 0, 0.58, 1);
	public static final Ease LINEAR = new Ease(0, 0, 1, 1);

	private final double cx;
	private final double bx;
	private final double ax;
	private final double cy;
	private final double by;
	private final double ay;

	public Ease(double x1, double y1, double x2, double y2) {
		cx = 3 * x1;
		bx = 3 * (x2 - x1) - cx;
		ax = 1 - cx - bx;
		cy = 3 * y1;
		by = 3 * (y2 - y1) - cy;
		ay = 1 - cy - by;
	}

	/** The eased value at {@code t} (0..1 is clamped). */
	public float apply(double t) {
		if (t <= 0) return 0f;
		if (t >= 1) return 1f;
		return (float) sampleY(solveX(t));
	}

	private double sampleX(double s) {
		return ((ax * s + bx) * s + cx) * s;
	}

	private double sampleY(double s) {
		return ((ay * s + by) * s + cy) * s;
	}

	private double sampleDerivativeX(double s) {
		return (3 * ax * s + 2 * bx) * s + cx;
	}

	private double solveX(double x) {
		double s = x;
		for (int i = 0; i < 8; i++) {
			double error = sampleX(s) - x;
			if (Math.abs(error) < 1e-6) return s;
			double slope = sampleDerivativeX(s);
			if (Math.abs(slope) < 1e-6) break;
			s -= error / slope;
		}
		double lo = 0;
		double hi = 1;
		s = x;
		while (lo < hi) {
			double value = sampleX(s);
			if (Math.abs(value - x) < 1e-6) return s;
			if (x > value) lo = s;
			else hi = s;
			s = (lo + hi) / 2;
			if (hi - lo < 1e-7) break;
		}
		return s;
	}
}
