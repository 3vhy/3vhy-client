package dev.threevhy.client.ui;

/**
 * A damped spring, for motion that should carry momentum: the Orbit's ring, a dragged card settling.
 * {@code stiffness} pulls toward the target and {@code damping} slows it; the website's orbit uses 55 and
 * 11.5. Steps in small slices, so a slow frame never makes it blow up.
 */
public final class Spring {
	private static final double STEP = 1.0 / 240.0;

	private final double stiffness;
	private final double damping;
	private double value;
	private double velocity;
	private double target;
	private long lastUpdate = 0;

	public Spring(double value, double stiffness, double damping) {
		this.value = value;
		this.target = value;
		this.stiffness = stiffness;
		this.damping = damping;
	}

	public Spring target(double target) {
		this.target = target;
		return this;
	}

	public double target() {
		return target;
	}

	/** Moves the value directly (dragging), keeping the target in step. */
	public Spring push(double delta) {
		value += delta;
		target = value;
		velocity = 0;
		return this;
	}

	public Spring snap(double to) {
		value = to;
		target = to;
		velocity = 0;
		return this;
	}

	public Spring kick(double velocity) {
		this.velocity += velocity;
		return this;
	}

	/** Advances to now and returns the value. Call once a frame. */
	public double update() {
		long now = System.nanoTime();
		double dt = lastUpdate == 0 ? 0 : Math.min(0.1, (now - lastUpdate) / 1e9);
		lastUpdate = now;
		while (dt > 0) {
			double h = Math.min(STEP, dt);
			velocity += ((target - value) * stiffness - velocity * damping) * h;
			value += velocity * h;
			dt -= h;
		}
		return value;
	}

	public double value() {
		return value;
	}

	public boolean resting() {
		return Math.abs(target - value) < 1e-4 && Math.abs(velocity) < 1e-4;
	}
}
