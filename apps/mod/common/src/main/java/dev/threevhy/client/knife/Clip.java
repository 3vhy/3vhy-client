package dev.threevhy.client.knife;

import dev.threevhy.client.ui.Ease;
import java.util.ArrayList;
import java.util.List;

/**
 * One knife animation as keyframes over a fixed time, each segment eased (no snaps). A pose is
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

	private record Key(float at, float[] pose, Ease ease) {
	}

	final String name;
	final long millis;
	private final List<Key> keys = new ArrayList<>();

	private Clip(String name, long millis) {
		this.name = name;
		this.millis = millis;
	}

	static Clip of(String name, long millis) {
		return new Clip(name, millis);
	}

	/** A keyframe at {@code at} (0..1 through the clip), eased into with {@code ease}; channels not given are at rest. */
	Clip key(float at, Ease ease, float... channelValuePairs) {
		float[] pose = rest();
		for (int i = 0; i + 1 < channelValuePairs.length; i += 2) pose[(int) channelValuePairs[i]] = channelValuePairs[i + 1];
		keys.add(new Key(at, pose, ease));
		return this;
	}

	/** A keyframe like the one before it, with only the given channels changed. */
	Clip then(float at, Ease ease, float... channelValuePairs) {
		float[] pose = keys.isEmpty() ? rest() : keys.get(keys.size() - 1).pose.clone();
		for (int i = 0; i + 1 < channelValuePairs.length; i += 2) pose[(int) channelValuePairs[i]] = channelValuePairs[i + 1];
		keys.add(new Key(at, pose, ease));
		return this;
	}

	/** Starts and ends at rest unless keys say otherwise. */
	Clip rested() {
		if (keys.isEmpty() || keys.get(0).at > 0) keys.add(0, new Key(0, rest(), Ease.LINEAR));
		if (keys.get(keys.size() - 1).at < 1) keys.add(new Key(1, rest(), Ease.IN_OUT));
		return this;
	}

	/** The pose {@code t} (0..1) through the clip, into {@code out}. */
	void sample(float t, float[] out) {
		if (keys.isEmpty()) {
			System.arraycopy(rest(), 0, out, 0, CHANNELS);
			return;
		}
		Key last = keys.get(0);
		if (t <= last.at) {
			System.arraycopy(last.pose, 0, out, 0, CHANNELS);
			return;
		}
		for (int i = 1; i < keys.size(); i++) {
			Key next = keys.get(i);
			if (t <= next.at) {
				float span = next.at - last.at;
				float s = span <= 0 ? 1 : next.ease.apply((t - last.at) / span);
				for (int c = 0; c < CHANNELS; c++) out[c] = last.pose[c] + (next.pose[c] - last.pose[c]) * s;
				return;
			}
			last = next;
		}
		System.arraycopy(last.pose, 0, out, 0, CHANNELS);
	}
}
