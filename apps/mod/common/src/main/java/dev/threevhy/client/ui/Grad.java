package dev.threevhy.client.ui;

import dev.threevhy.client.Draw;

/**
 * A two-stop linear gradient, angled like CSS's {@code linear-gradient}: 0 runs bottom to top, 90 left
 * to right, 135 from the top left to the bottom right. It spans the box of the shape it fills.
 */
public record Grad(int from, int to, float angle) {
	/** Horizontal, left to right. */
	public static Grad across(int from, int to) {
		return new Grad(from, to, 90);
	}

	/** Vertical, top to bottom. */
	public static Grad down(int from, int to) {
		return new Grad(from, to, 180);
	}

	public Grad withAlpha(float multiplier) {
		return new Grad(Paint.fade(from, multiplier), Paint.fade(to, multiplier), angle);
	}

	/** The colour at (x, y) for a shape whose box is (x1, y1) to (x2, y2). */
	public int at(float x, float y, float x1, float y1, float x2, float y2) {
		double a = Math.toRadians(angle);
		double dx = Math.sin(a);
		double dy = -Math.cos(a);
		double w = x2 - x1;
		double h = y2 - y1;
		double length = Math.abs(w * dx) + Math.abs(h * dy);
		if (length <= 0) return from;
		double t = ((x - (x1 + x2) / 2.0) * dx + (y - (y1 + y2) / 2.0) * dy) / length + 0.5;
		return Draw.mix(from, to, t);
	}
}
