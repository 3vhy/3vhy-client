package dev.threevhy.client.knife;

/**
 * Plays a knife's animations on the clock, so they're smooth at any frame rate. A new animation fades in
 * over the one it interrupts (angles taken the short way round, so a half-done spin never unwinds), and
 * every animation ends at rest. On top: a slow breathing sway, and a sway that lags behind the mouse.
 */
final class KnifeAnimator {
	private static final long FADE_NANOS = 150_000_000L;

	private Clip clip;
	private long started;
	private float speed = 1;
	private final float[] from = Clip.rest();
	private final float[] base = Clip.rest();
	private long fadeStarted = 0;
	private boolean fading = false;
	private int slash = 0;
	private float lastYaw = Float.NaN;
	private float lastPitch;
	private float swayX;
	private float swayY;
	private long lastFrame = 0;

	/** Starts {@code next} (or goes back to rest for null), fading from wherever the knife is now. */
	void play(Clip next, float speed, long now) {
		current(now, from);
		for (int c : new int[] {Clip.PITCH, Clip.YAW, Clip.ROLL, Clip.K_PITCH, Clip.K_YAW, Clip.K_ROLL, Clip.HANDLE_A, Clip.HANDLE_B, Clip.SWEEP}) {
			from[c] = wrap(from[c]);
		}
		fadeStarted = now;
		fading = true;
		clip = next;
		started = now;
		this.speed = Math.max(0.05f, speed);
	}

	/** The next slash, taking turns between the knife's slashes so it doesn't look the same every time. */
	void slash(KnifeModel model, float speed, long now) {
		if (clip != null && clip.name.startsWith("slash") || clip != null && clip.name.equals("stab")) {
			float t = (now - started) * this.speed / (clip.millis * 1_000_000f);
			if (t < 0.55f) return;
		}
		play(model.slash(slash), speed, now);
		slash = (slash + 1) % model.slashCount();
	}

	boolean playing(Clip which) {
		return clip == which && clip != null;
	}

	/** Snaps to rest with nothing playing, for a knife that just appeared without a draw. */
	void reset() {
		clip = null;
		fading = false;
	}

	private static float wrap(float degrees) {
		float d = degrees % 360f;
		if (d >= 180) d -= 360;
		if (d < -180) d += 360;
		return d;
	}

	/** The animation's pose now, without the idle sway. */
	private void current(long now, float[] out) {
		if (clip != null) {
			float t = (now - started) * speed / (clip.millis * 1_000_000f);
			if (t >= 1) {
				clip.sample(1, base);
				clip = null;
			} else {
				clip.sample(Math.max(0, t), base);
			}
		}
		if (clip == null) {
			float[] rest = Clip.rest();
			System.arraycopy(rest, 0, base, 0, Clip.CHANNELS);
		}
		if (fading) {
			float w = Math.min(1, (now - fadeStarted) / (float) FADE_NANOS);
			w = w * w * (3 - 2 * w);
			for (int c = 0; c < Clip.CHANNELS; c++) out[c] = from[c] + (base[c] - from[c]) * w;
			if (w >= 1) fading = false;
		} else {
			System.arraycopy(base, 0, out, 0, Clip.CHANNELS);
		}
	}

	/**
	 * The pose to draw now: the animation plus breathing and mouse sway, scaled by {@code idle} (0 turns them
	 * off). {@code cameraYaw}/{@code cameraPitch} are where the camera looks this frame.
	 */
	void pose(long now, float cameraYaw, float cameraPitch, float idle, float[] out) {
		current(now, out);
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
	}
}
