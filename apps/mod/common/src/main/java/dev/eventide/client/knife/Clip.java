package dev.eventide.client.knife;

import java.util.ArrayList;
import java.util.List;

/**
 * One knife animation as keyframes over a fixed time. Between keys every channel follows a smooth curve
 * (a monotone cubic), so the knife keeps its speed through a key instead of stopping at it, never
 * overshoots a key, and holds perfectly still where two keys agree; it starts and ends at rest. A pose is
 * {@link #CHANNELS} numbers: where the viewmodel is ({@link #X}, {@link #Y}, {@link #Z}, in blocks) and how
 * it's turned ({@link #PITCH}, {@link #YAW}, {@link #ROLL}, degrees, about the wrist), how the knife turns in
 * the hand ({@link #K_PITCH}, {@link #K_YAW}, {@link #K_ROLL}, about the knife's spin point), how far it
 * slides along its blade ({@link #SLIDE}, pixels), how open it is ({@link #OPEN}: a butterfly knife's handles
 * or an energy blade's length, 1 at rest), two extra handle angles for butterfly tricks, and a sweep in the
 * screen's plane about the fist ({@link #SWEEP}, degrees, counter-clockwise) for slashes.
 */
final class Clip {
	static final int X = 0;
	static final int Y = 1;
	static final int Z = 2;
	static final int PITCH = 3;
	static final int YAW = 4;
	static final int ROLL = 5;
	static final int K_PITCH = 6;
	static final int K_YAW = 7;
	static final int K_ROLL = 8;
	static final int SLIDE = 9;
	static final int OPEN = 10;
	static final int HANDLE_A = 11;
	static final int HANDLE_B = 12;
	/** A turn of the whole viewmodel in the screen's plane about the fist, for slashes that sweep across. */
	static final int SWEEP = 13;
	static final int CHANNELS = 14;

	/** The pose at rest: everything 0 but {@link #OPEN}. */
	static float[] rest() {
		float[] pose = new float[CHANNELS];
		pose[OPEN] = 1;
		return pose;
	}

	private record Key(float at, float[] pose) {
	}

	final String name;
	final long millis;
	private final List<Key> keys = new ArrayList<>();
	/** Each key's slope per channel, worked out once the keys are all in. */
	private float[][] slopes;

	private Clip(String name, long millis) {
		this.name = name;
		this.millis = millis;
	}

	static Clip of(String name, long millis) {
		return new Clip(name, millis);
	}

	/** A keyframe at {@code at} (0..1 through the clip); channels not given are at rest. */
	Clip key(float at, float... channelValuePairs) {
		float[] pose = rest();
		for (int i = 0; i + 1 < channelValuePairs.length; i += 2) pose[(int) channelValuePairs[i]] = channelValuePairs[i + 1];
		keys.add(new Key(at, pose));
		slopes = null;
		return this;
	}

	/** A keyframe like the one before it, with only the given channels changed. */
	Clip then(float at, float... channelValuePairs) {
		float[] pose = keys.isEmpty() ? rest() : keys.get(keys.size() - 1).pose.clone();
		for (int i = 0; i + 1 < channelValuePairs.length; i += 2) pose[(int) channelValuePairs[i]] = channelValuePairs[i + 1];
		keys.add(new Key(at, pose));
		slopes = null;
		return this;
	}

	/**
	 * The slope at each key, per channel (Fritsch and Carlson's monotone cubic): flat at the first and last
	 * key and wherever a channel turns back or holds, otherwise a blend of the two sides, kept small enough
	 * that the curve never goes past the next key.
	 */
	private float[][] slopes() {
		if (slopes != null) return slopes;
		int n = keys.size();
		float[][] out = new float[n][CHANNELS];
		for (int c = 0; c < CHANNELS; c++) {
			float[] secant = new float[Math.max(0, n - 1)];
			for (int i = 0; i + 1 < n; i++) {
				float span = keys.get(i + 1).at - keys.get(i).at;
				secant[i] = span <= 0 ? 0 : (keys.get(i + 1).pose[c] - keys.get(i).pose[c]) / span;
			}
			for (int i = 1; i + 1 < n; i++) {
				float a = secant[i - 1];
				float b = secant[i];
				if (a * b <= 0) continue;
				float h0 = keys.get(i).at - keys.get(i - 1).at;
				float h1 = keys.get(i + 1).at - keys.get(i).at;
				float w0 = 2 * h1 + h0;
				float w1 = h1 + 2 * h0;
				out[i][c] = (w0 + w1) / (w0 / a + w1 / b);
			}
		}
		slopes = out;
		return out;
	}

	/** The pose {@code t} (0..1) through the clip, into {@code out}. */
	void sample(float t, float[] out) {
		if (keys.isEmpty()) {
			System.arraycopy(rest(), 0, out, 0, CHANNELS);
			return;
		}
		Key first = keys.get(0);
		if (t <= first.at) {
			System.arraycopy(first.pose, 0, out, 0, CHANNELS);
			return;
		}
		float[][] m = slopes();
		for (int i = 1; i < keys.size(); i++) {
			Key next = keys.get(i);
			if (t <= next.at) {
				Key last = keys.get(i - 1);
				float span = next.at - last.at;
				if (span <= 0) {
					System.arraycopy(next.pose, 0, out, 0, CHANNELS);
					return;
				}
				float s = (t - last.at) / span;
				float s2 = s * s;
				float s3 = s2 * s;
				float h00 = 2 * s3 - 3 * s2 + 1;
				float h10 = s3 - 2 * s2 + s;
				float h01 = -2 * s3 + 3 * s2;
				float h11 = s3 - s2;
				for (int c = 0; c < CHANNELS; c++) {
					out[c] = h00 * last.pose[c] + h10 * span * m[i - 1][c] + h01 * next.pose[c] + h11 * span * m[i][c];
				}
				return;
			}
		}
		System.arraycopy(keys.get(keys.size() - 1).pose, 0, out, 0, CHANNELS);
	}
}
