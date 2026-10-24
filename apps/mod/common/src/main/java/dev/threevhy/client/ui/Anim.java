package dev.threevhy.client.ui;

/**
 * A value that glides to a new target over a fixed time on an {@link Ease} curve, like a CSS
 * transition: changing the target mid-way starts the new glide from wherever it is now. Time comes from
 * the system clock, so motion runs at the same speed at any frame rate.
 */
public final class Anim {
	private final Ease ease;
	private final long durationNanos;
	private float from;
	private float to;
	private long startedAt;
	private boolean settled = true;

	public Anim(float value, int durationMs, Ease ease) {
		this.from = value;
		this.to = value;
		this.durationNanos = Math.max(1, durationMs) * 1_000_000L;
		this.ease = ease;
	}

	/** Glides to {@code target}; does nothing if it's already heading there. */
	public Anim set(float target) {
		if (target == to) return this;
		from = get();
		to = target;
		startedAt = System.nanoTime();
		settled = false;
		return this;
	}

	public Anim set(boolean on) {
		return set(on ? 1f : 0f);
	}

	/** Jumps straight to {@code value}, with no glide. */
	public Anim snap(float value) {
		from = value;
		to = value;
		settled = true;
		return this;
	}

	public float target() {
		return to;
	}

	public float get() {
		if (settled) return to;
		double t = (System.nanoTime() - startedAt) / (double) durationNanos;
		if (t >= 1) {
			settled = true;
			return to;
		}
		return from + (to - from) * ease.apply(t);
	}

	public boolean moving() {
		return !settled && get() != to;
	}
}
