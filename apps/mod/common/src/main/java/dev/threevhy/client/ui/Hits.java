package dev.threevhy.client.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * The clickable areas a screen drew this frame, topmost last. Screens rebuild it every frame while
 * drawing, then look up mouse clicks in it, so what you click is always exactly what you see.
 */
public final class Hits {
	/** A clickable area with what left and right clicks do (either may be null). */
	public record Hit(float x1, float y1, float x2, float y2, Runnable left, Runnable right) {
		public boolean contains(double x, double y) {
			return x >= x1 && x < x2 && y >= y1 && y < y2;
		}
	}

	private final List<Hit> hits = new ArrayList<>();
	/** Clicks only count inside this rectangle (a scrolling list), or anywhere when null. */
	private float[] clip = null;

	public void clear() {
		hits.clear();
		clip = null;
	}

	public void clip(float x1, float y1, float x2, float y2) {
		clip = new float[] {x1, y1, x2, y2};
	}

	public void unclip() {
		clip = null;
	}

	public void add(float x1, float y1, float x2, float y2, Runnable left) {
		add(x1, y1, x2, y2, left, null);
	}

	public void add(float x1, float y1, float x2, float y2, Runnable left, Runnable right) {
		if (clip != null) {
			x1 = Math.max(x1, clip[0]);
			y1 = Math.max(y1, clip[1]);
			x2 = Math.min(x2, clip[2]);
			y2 = Math.min(y2, clip[3]);
			if (x2 <= x1 || y2 <= y1) return;
		}
		hits.add(new Hit(x1, y1, x2, y2, left, right));
	}

	/** The topmost area under (x, y) with an action for {@code button} (0 left, 1 right), or null. */
	public Runnable at(double x, double y, int button) {
		for (int i = hits.size() - 1; i >= 0; i--) {
			Hit hit = hits.get(i);
			if (!hit.contains(x, y)) continue;
			Runnable action = button == 0 ? hit.left : button == 1 ? hit.right : null;
			if (action != null) return action;
		}
		return null;
	}

	/** Whether anything clickable is under (x, y). */
	public boolean over(double x, double y) {
		for (Hit hit : hits) {
			if (hit.contains(x, y)) return true;
		}
		return false;
	}
}
