package dev.threevhy.client.knife;

/**
 * Plays a knife's animations on the clock, so they're smooth at any frame rate. A new animation takes over
 * from wherever the knife is and however fast it's moving: the difference between the two is carried along
 * and eased away (inertial blending), so a slash cut short by the next one, or an inspect interrupted by a
 * hit, never stops dead or jumps. Angles are taken the short way round, so a half-done spin never unwinds.
 * On top: a slow breathing sway, a sway that lags behind the mouse, and the way a shooter's viewmodel moves
 * with you: a figure-of-eight bob as you walk, the knife lowered and tipped while sprinting, a lean against
 * strafing, and a springy dip when you jump and land.
 */
final class KnifeAnimator {
	/** How long the hand-over between two animations takes. */
	private static final float BLEND_SECONDS = 0.2f;
	/** The step used to measure how fast the knife is moving when a new animation starts. */
	private static final long PROBE_NANOS = 4_000_000L;
	/** The channels that are angles, wrapped to the short way round. */
	private static final boolean[] ANGLE = new boolean[Clip.CHANNELS];
	/** The most a carried-over movement may add, so a fast spin settles instead of flinging the knife. */
	private static final float[] MOMENTUM_LIMIT = new float[Clip.CHANNELS];

	static {
		for (int c : new int[] {Clip.PITCH, Clip.YAW, Clip.ROLL, Clip.K_PITCH, Clip.K_YAW, Clip.K_ROLL, Clip.HANDLE_A, Clip.HANDLE_B, Clip.SWEEP}) {
			ANGLE[c] = true;
		}
		for (int c = 0; c < Clip.CHANNELS; c++) MOMENTUM_LIMIT[c] = ANGLE[c] ? 240 : c == Clip.SLIDE ? 6 : 0.5f;
	}

	private Clip clip;
	private long started;
	private float speed = 1;
	private final float[] offset = new float[Clip.CHANNELS];
	private final float[] drift = new float[Clip.CHANNELS];
	private long blendStarted = 0;
	private boolean blending = false;
	private int slash = 0;
	private float lastYaw = Float.NaN;
	private float lastPitch;
	private float swayX;
	private float swayY;
	private long lastFrame = 0;
	private double forward;
	private double strafe;
	private double vertical;
	private boolean onGround = true;
	private boolean sprinting;
	private boolean wasOnGround = true;
	private double lastVertical;
	private float bobPhase;
	private float bobAmount;
	private float sprintAmount;
	private float strafeAmount;
	private float airAmount;
	private float dip;
	private float dipSpeed;
	private final float[] current = Clip.rest();
	private final float[] earlier = Clip.rest();
	private final float[] target = Clip.rest();
	private final float[] targetSoon = Clip.rest();

	/** Starts {@code clip} (or goes back to rest for null), taking over from wherever the knife is now. */
	void play(Clip clip, float speed, long now) {
		evaluate(now, current);
		evaluate(now - PROBE_NANOS, earlier);
		float newSpeed = Math.max(0.05f, speed);
		sampleAt(clip, 0, target);
		sampleAt(clip, PROBE_NANOS * newSpeed / (clip == null ? 1f : clip.millis * 1_000_000f), targetSoon);
		float probe = PROBE_NANOS / 1e9f;
		for (int c = 0; c < Clip.CHANNELS; c++) {
			float gap = current[c] - target[c];
			if (ANGLE[c]) gap = wrap(gap);
			float carried = (current[c] - earlier[c]) / probe - (targetSoon[c] - target[c]) / probe;
			float limit = MOMENTUM_LIMIT[c] / BLEND_SECONDS;
			offset[c] = gap;
			drift[c] = Math.max(-limit, Math.min(limit, carried));
		}
		blendStarted = now;
		blending = true;
		this.clip = clip;
		started = now;
		this.speed = newSpeed;
	}

	/**
	 * The next slash, taking turns between the knife's slashes (right to left, then back) as a shooter's
	 * knife does. Clicking again starts the next one once the blade has gone through; holding the button
	 * on a block (which swings every tick) lets each slash finish first. Returns which slash started, or -1.
	 */
	int slash(KnifeModel model, float speed, long now, boolean mining) {
		if (clip != null && (clip.name.startsWith("slash") || clip.name.equals("heavy stab"))) {
			float t = progress(now);
			if (t < (mining ? 0.92f : 0.34f)) return -1;
		}
		int which = slash;
		play(model.slash(which), speed, now);
		slash = (slash + 1) % model.slashCount();
		return which;
	}

	boolean playing(Clip which) {
		return clip == which && clip != null;
	}

	private float progress(long now) {
		return clip == null ? 1 : (now - started) * speed / (clip.millis * 1_000_000f);
	}

	private static void sampleAt(Clip clip, float t, float[] out) {
		if (clip == null) {
			System.arraycopy(Clip.rest(), 0, out, 0, Clip.CHANNELS);
		} else {
			clip.sample(Math.max(0, Math.min(1, t)), out);
		}
	}

	private static float wrap(float degrees) {
		float d = degrees % 360f;
		if (d >= 180) d -= 360;
		if (d < -180) d += 360;
		return d;
	}

	/** The animation's pose at {@code at}, with the carried-over difference easing away; changes nothing. */
	private void evaluate(long at, float[] out) {
		sampleAt(clip, progress(at), out);
		if (!blending) return;
		float seconds = (at - blendStarted) / 1e9f;
		if (seconds >= BLEND_SECONDS) return;
		float s = Math.max(0, seconds / BLEND_SECONDS);
		float s2 = s * s;
		float s3 = s2 * s;
		float keep = 2 * s3 - 3 * s2 + 1;
		float carry = (s3 - 2 * s2 + s) * BLEND_SECONDS;
		for (int c = 0; c < Clip.CHANNELS; c++) out[c] += keep * offset[c] + carry * drift[c];
	}

	/**
	 * How the player moves this frame, in blocks a tick: {@code forward} and {@code strafe} (to the right)
	 * relative to where they face, and {@code vertical}.
	 */
	void moving(double forward, double strafe, double vertical, boolean onGround, boolean sprinting) {
		this.forward = forward;
		this.strafe = strafe;
		this.vertical = vertical;
		this.onGround = onGround;
		this.sprinting = sprinting;
	}

	/**
	 * The pose to draw now: the animation plus breathing and mouse sway, scaled by {@code idle} (0 turns them
	 * off), and the movement bob, scaled by {@code bob}. {@code cameraYaw}/{@code cameraPitch} are where the
	 * camera looks this frame.
	 */
	void pose(long now, float cameraYaw, float cameraPitch, float idle, float bob, float[] out) {
		evaluate(now, out);
		if (clip != null && progress(now) >= 1) clip = null;
		if (blending && now - blendStarted >= BLEND_SECONDS * 1e9f) blending = false;
		float dt = lastFrame == 0 ? 0 : Math.min(0.1f, (now - lastFrame) / 1e9f);
		lastFrame = now;
		if (Float.isNaN(lastYaw)) {
			lastYaw = cameraYaw;
			lastPitch = cameraPitch;
		}
		float dYaw = cameraYaw - lastYaw;
		if (dYaw > 180) dYaw -= 360;
		if (dYaw < -180) dYaw += 360;
		float dPitch = cameraPitch - lastPitch;
		lastYaw = cameraYaw;
		lastPitch = cameraPitch;
		if (dt > 0) {
			float targetX = Math.max(-0.06f, Math.min(0.06f, -dYaw / dt * 0.00035f));
			float targetY = Math.max(-0.05f, Math.min(0.05f, dPitch / dt * 0.00035f));
			float k = 1 - (float) Math.exp(-dt / 0.09f);
			swayX += (targetX - swayX) * k;
			swayY += (targetY - swayY) * k;
		}
		double seconds = (now % 3_600_000_000_000L) / 1e9;
		out[Clip.X] += swayX * idle;
		out[Clip.Y] += (swayY + (float) Math.sin(seconds * 2 * Math.PI * 0.24) * 0.006f) * idle;
		out[Clip.ROLL] += (swayX * 60 + (float) Math.sin(seconds * 2 * Math.PI * 0.17) * 0.9f) * idle;
		out[Clip.PITCH] += (float) Math.sin(seconds * 2 * Math.PI * 0.21 + 1) * 0.5f * idle;
		if (dt > 0) step(dt);
		float wave = (float) Math.sin(bobPhase);
		out[Clip.X] += (wave * 0.022f * bobAmount - 0.035f * strafeAmount + 0.06f * sprintAmount) * bob;
		out[Clip.Y] += (((float) Math.cos(2 * bobPhase) - 1) * 0.013f * bobAmount - 0.1f * sprintAmount - 0.04f * airAmount + dip) * bob;
		out[Clip.ROLL] += (wave * 1.5f * bobAmount + 10 * sprintAmount) * bob;
		out[Clip.PITCH] += (-6 * sprintAmount + dip * 40) * bob;
		out[Clip.SWEEP] += (8 * sprintAmount + 4 * strafeAmount) * bob;
	}

	/** Moves the bob, sprint, strafe and jump springs on by {@code dt} seconds. */
	private void step(float dt) {
		float walk = (float) Math.min(1.4, Math.hypot(forward, strafe) / 0.216);
		bobPhase = (bobPhase + dt * 2 * (float) Math.PI * 1.7f * Math.max(0.35f, walk)) % (float) (2 * Math.PI);
		bobAmount += ((onGround ? walk : 0) - bobAmount) * ease(dt, 0.12f);
		sprintAmount += ((sprinting && walk > 0.2f ? 1 : 0) - sprintAmount) * ease(dt, 0.18f);
		strafeAmount += ((float) Math.max(-1, Math.min(1, strafe / 0.216)) - strafeAmount) * ease(dt, 0.15f);
		airAmount += ((onGround ? 0 : (float) Math.max(-1, Math.min(1, vertical / 0.42))) - airAmount) * ease(dt, 0.1f);
		if (onGround && !wasOnGround && lastVertical < -0.15) dipSpeed -= (float) Math.min(1.4, -lastVertical * 1.6);
		if (!onGround && wasOnGround && vertical > 0.2) dipSpeed -= 0.35f;
		wasOnGround = onGround;
		lastVertical = vertical;
		int steps = Math.max(1, (int) Math.ceil(dt * 240));
		float h = dt / steps;
		for (int i = 0; i < steps; i++) {
			dipSpeed += (-140 * dip - 16 * dipSpeed) * h;
			dip += dipSpeed * h;
		}
	}

	private static float ease(float dt, float seconds) {
		return 1 - (float) Math.exp(-dt / seconds);
	}
}
