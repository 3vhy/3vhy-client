package dev.threevhy.client.knife;

import dev.threevhy.client.knife.KnifeMesh.Mat;
import dev.threevhy.client.knife.KnifeMesh.Ring;
import dev.threevhy.client.knife.KnifeMesh.Station;
import dev.threevhy.client.ui.Ease;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The knives, each its own shape built from {@link KnifeMesh} pieces, with its own draw, inspect and slash
 * animations ({@link Clip}). Shapes are made the first time a knife is drawn and kept.
 */
public enum KnifeModel {
	KARAMBIT("Karambit", 1.0f, 0, -5.6f),
	BUTTERFLY("Butterfly", 0.95f, -0.45f, 0.5f),
	BAYONET("Bayonet", 0.9f, 0, -1.5f),
	HUNTSMAN("Huntsman", 0.92f, 0, -1.5f),
	DAGGER("Dagger", 0.95f, 0, -1.0f),
	PHANTOM("Phantom", 0.85f, 0, -1.0f),
	KATANA("Katana", 0.66f, 0, -2.0f),
	SCYTHE("Scythe", 0.72f, 0, -1.0f);

	/** What a part of a knife does when the knife moves. */
	enum PartKind {
		/** Moves with the knife. */
		BODY,
		/** A butterfly knife's handles, turning about their pins. */
		HANDLE_A,
		HANDLE_B,
		/** An energy blade, as long as the pose's {@link Clip#OPEN}. */
		ENERGY
	}

	record Part(KnifeMesh mesh, PartKind kind, float pivotX, float pivotY) {
	}

	public final String label;
	/** How big this knife is drawn, so long blades don't fill the screen. */
	final float scale;
	/** The point it spins about in tricks, in pixels: a karambit's ring, a butterfly knife's pin. */
	final float spinX;
	final float spinY;
	private List<Part> parts;
	private Clip draw;
	private Clip inspect;
	private Clip[] slashes;
	private Clip use;

	KnifeModel(String label, float scale, float spinX, float spinY) {
		this.label = label;
		this.scale = scale;
		this.spinX = spinX;
		this.spinY = spinY;
	}

	/** Every knife's name, in menu order. */
	public static String[] labels() {
		KnifeModel[] all = values();
		String[] out = new String[all.length];
		for (int i = 0; i < all.length; i++) out[i] = all[i].label;
		return out;
	}

	/** The knife with this name (case doesn't matter), or null. */
	public static KnifeModel byLabel(String label) {
		for (KnifeModel model : values()) {
			if (model.label.equalsIgnoreCase(label) || model.name().equalsIgnoreCase(label)) return model;
		}
		return null;
	}

	List<Part> parts() {
		if (parts == null) parts = build();
		return parts;
	}

	Clip draw() {
		clips();
		return draw;
	}

	Clip inspect() {
		clips();
		return inspect;
	}

	Clip slash(int variant) {
		clips();
		return slashes[Math.floorMod(variant, slashes.length)];
	}

	int slashCount() {
		clips();
		return slashes.length;
	}

	Clip use() {
		clips();
		return use;
	}

	/** The colour of each material for this knife in a finish ("Steel", "Purple", "Crimson", "Ice" or "Gold"). */
	KnifeMesh.Palette palette(String finish) {
		int blade = switch (finish) {
			case "Purple" -> 0xFFB98DFF;
			case "Crimson" -> 0xFFEA6672;
			case "Ice" -> 0xFFC8F2FF;
			case "Gold" -> 0xFFFFD98A;
			default -> 0xFFDDE3EB;
		};
		int glow = switch (finish) {
			case "Purple" -> 0xFFB45CFF;
			case "Crimson" -> 0xFFFF3B52;
			case "Ice" -> 0xFF5BD8FF;
			case "Gold" -> 0xFFFFB23C;
			default -> 0xFFA52BF5;
		};
		int accent = switch (finish) {
			case "Steel" -> 0xFFA52BF5;
			default -> glow;
		};
		int handle = switch (this) {
			case KARAMBIT -> 0xFF2C2933;
			case BUTTERFLY -> finish.equals("Steel") ? 0xFF6F2CF0 : mix(glow, 0xFF202028, 0.35f);
			case BAYONET -> 0xFF26282E;
			case HUNTSMAN -> 0xFF9A6440;
			case DAGGER -> 0xFF6B4530;
			case PHANTOM -> 0xFFC2C8D2;
			case KATANA -> 0xFF26222E;
			case SCYTHE -> 0xFF8A5C38;
		};
		int metal = mix(0xFF4D525C, blade, 0.18f);
		int edge = mix(blade, 0xFFFFFFFF, 0.45f);
		return mat -> switch (mat) {
			case BLADE -> blade;
			case EDGE -> edge;
			case METAL -> metal;
			case HANDLE, GRIP, WRAP, CARBON -> handle;
			case ACCENT -> accent;
			case ENERGY_CORE -> mix(0xFFFFFFFF, glow, 0.25f);
			case ENERGY_GLOW -> (0x70 << 24) | (glow & 0xFFFFFF);
		};
	}

	private static int mix(int a, int b, float t) {
		int out = 0;
		for (int shift = 0; shift <= 24; shift += 8) {
			int ca = (a >>> shift) & 255;
			int cb = (b >>> shift) & 255;
			out |= (Math.round(ca + (cb - ca) * t) & 255) << shift;
		}
		return out;
	}

	private List<Part> build() {
		List<Part> out = new ArrayList<>();
		KnifeMesh body = new KnifeMesh();
		switch (this) {
			case KARAMBIT -> karambit(body);
			case BUTTERFLY -> {
				butterflyBlade(body);
				out.add(new Part(butterflyHandle(-1), PartKind.HANDLE_A, -0.45f, 0.5f));
				out.add(new Part(butterflyHandle(1), PartKind.HANDLE_B, 0.45f, 0.5f));
			}
			case BAYONET -> bayonet(body);
			case HUNTSMAN -> huntsman(body);
			case DAGGER -> dagger(body);
			case PHANTOM -> {
				phantomHilt(body);
				out.add(new Part(phantomBlade(), PartKind.ENERGY, 0, 1.3f));
			}
			case KATANA -> katana(body);
			case SCYTHE -> scythe(body);
		}
		out.add(0, new Part(body, PartKind.BODY, 0, 0));
		return List.copyOf(out);
	}

	/** Stations along a straight blade from {@code base} to {@code tip}: spine and edge x at each, thickness tapering. */
	private static Station[] straight(float base, float tip, int steps, float spineX, float edgeX, float t0, float t1, float clipFrom, float tipX) {
		Station[] st = new Station[steps + 1];
		for (int i = 0; i <= steps; i++) {
			float f = i / (float) steps;
			float y = base + (tip - base) * f;
			float sx = spineX;
			float ex = edgeX;
			if (f > clipFrom) {
				float g = (f - clipFrom) / (1 - clipFrom);
				float curve = 1 - (float) Math.sqrt(1 - g * g * 0.98f);
				sx = spineX + (tipX - spineX) * (float) Math.pow(g, 1.4);
				ex = edgeX + (tipX - edgeX) * curve;
			}
			st[i] = new Station(sx, y, ex, y, t0 + (t1 - t0) * f);
		}
		return st;
	}

	private static void karambit(KnifeMesh m) {
		Ring[] handle = new Ring[7];
		for (int i = 0; i < handle.length; i++) {
			float f = i / (float) (handle.length - 1);
			float y = -4.4f + 5.4f * f;
			float bulge = 1 + 0.12f * (float) Math.sin(f * Math.PI);
			handle[i] = new Ring(0.15f * (float) Math.sin(f * Math.PI), y, 0, 0, 1, 0, 0.72f * bulge, 0.42f);
		}
		m.loft(handle, 10, 0.55f, Mat.GRIP, true, false);
		m.ring(0, -5.75f, 0, 1.25f, 0.32f, 16, 6, Mat.METAL);
		m.box(-1.05f, 0.8f, -0.5f, 1.25f, 1.25f, 0.5f, Mat.METAL);
		int n = 14;
		Station[] st = new Station[n + 1];
		for (int i = 0; i <= n; i++) {
			float f = i / (float) n;
			double angle = Math.toRadians(180 - 158 * f);
			float cx = 3.0f;
			float cy = 1.15f;
			float rs = 4.0f + 0.25f * f;
			float width = 1.45f * (1 - (float) Math.pow(f, 1.6));
			float re = rs - width;
			st[i] = new Station(cx + (float) Math.cos(angle) * rs, cy + (float) Math.sin(angle) * rs, cx + (float) Math.cos(angle) * re,
					cy + (float) Math.sin(angle) * re, 0.55f - 0.4f * f);
		}
		m.blade(st, Mat.BLADE, Mat.EDGE, 0.42f, true);
	}

	private static void butterflyBlade(KnifeMesh m) {
		m.box(-0.75f, -0.1f, -0.17f, 0.75f, 1.3f, 0.17f, Mat.METAL);
		m.blade(straight(1.2f, 9.2f, 12, -0.55f, 0.75f, 0.34f, 0.12f, 0.62f, 0.15f), Mat.BLADE, Mat.EDGE, 0.5f, true);
	}

	/** One butterfly handle hanging from its pin at the origin, on one side of the blade ({@code side} -1 or 1). */
	private static KnifeMesh butterflyHandle(int side) {
		KnifeMesh m = new KnifeMesh();
		float z0 = side < 0 ? -0.62f : 0.2f;
		float z1 = side < 0 ? -0.2f : 0.62f;
		m.box(-0.5f, -7.4f, z0, 0.5f, 0.4f, z1, Mat.HANDLE);
		m.box(-0.38f, -7.2f, side < 0 ? z0 - 0.04f : z1, 0.38f, -0.6f, side < 0 ? z0 : z1 + 0.04f, Mat.CARBON);
		m.ring(0, 0, (z0 + z1) / 2f, 0.32f, 0.1f, 10, 4, Mat.METAL);
		if (side > 0) m.box(-0.55f, -8.0f, -0.62f, 0.55f, -7.4f, 0.62f, Mat.METAL);
		return m;
	}

	private static void bayonet(KnifeMesh m) {
		m.blade(straight(1.0f, 9.8f, 14, -0.62f, 0.85f, 0.62f, 0.2f, 0.7f, -0.25f), Mat.BLADE, Mat.EDGE, 0.45f, true);
		for (int i = 0; i < 6; i++) {
			float y = 2.6f + i * 0.55f;
			m.box(-0.82f, y, -0.12f, -0.6f, y + 0.25f, 0.12f, Mat.BLADE);
		}
		m.box(-1.35f, 0.45f, -0.55f, 1.7f, 1.05f, 0.55f, Mat.METAL);
		m.ring(1.9f, 0.75f, 0, 0.45f, 0.14f, 12, 4, Mat.METAL);
		Ring[] grip = new Ring[11];
		for (int i = 0; i < grip.length; i++) {
			float f = i / (float) (grip.length - 1);
			float ridge = i % 2 == 0 ? 1.0f : 0.9f;
			grip[i] = Ring.along(0, 0.45f - 5.6f * f, 0, 0.76f * ridge, 0.62f * ridge);
		}
		m.loft(grip, 12, 0.35f, Mat.GRIP, false, false);
		m.loft(new Ring[] {Ring.along(0, -5.15f, 0, 0.84f, 0.7f), Ring.along(0, -5.9f, 0, 0.9f, 0.74f), Ring.along(0, -6.25f, 0, 0.55f, 0.45f)}, 12, 0.3f,
				Mat.METAL, false, true);
	}

	private static void huntsman(KnifeMesh m) {
		int n = 12;
		Station[] st = new Station[n + 1];
		for (int i = 0; i <= n; i++) {
			float f = i / (float) n;
			float y = 1.0f + 7.8f * f;
			float sx = -0.75f + (f > 0.75f ? (f - 0.75f) / 0.25f * 0.4f : 0);
			float ex = f < 0.78f ? 1.0f : 1.0f - (f - 0.78f) / 0.22f * 1.35f;
			float ey = f < 0.78f ? y : y + (f - 0.78f) / 0.22f * 0.3f;
			st[i] = new Station(sx, y, ex, ey, 0.72f - 0.42f * f);
		}
		m.blade(st, Mat.BLADE, Mat.EDGE, 0.4f, true);
		m.box(-1.6f, 0.4f, -0.55f, 1.8f, 1.0f, 0.55f, Mat.METAL);
		m.box(-1.85f, 0.25f, -0.4f, -1.4f, 1.15f, 0.4f, Mat.METAL);
		m.box(1.6f, 0.25f, -0.4f, 2.05f, 1.15f, 0.4f, Mat.METAL);
		Ring[] grip = new Ring[13];
		for (int i = 0; i < grip.length; i++) {
			float f = i / (float) (grip.length - 1);
			float groove = 1 - 0.12f * (float) Math.max(0, Math.sin(f * Math.PI * 4));
			grip[i] = new Ring(0.1f * (float) Math.sin(f * Math.PI), 0.4f - 5.4f * f, 0, 0, 1, 0, 0.88f * groove, 0.56f);
		}
		m.loft(grip, 12, 0.5f, Mat.HANDLE, false, false);
		m.loft(new Ring[] {Ring.along(0.1f, -5.0f, 0, 0.92f, 0.6f), Ring.along(0.1f, -5.6f, 0, 0.75f, 0.5f)}, 12, 0.5f, Mat.METAL, false, true);
	}

	private static void dagger(KnifeMesh m) {
		int n = 10;
		Station[] right = new Station[n + 1];
		Station[] left = new Station[n + 1];
		for (int i = 0; i <= n; i++) {
			float f = i / (float) n;
			float y = 0.95f + 7.6f * f;
			float w = 0.85f * (1 - (float) Math.pow(f, 1.25));
			float t = 0.5f - 0.38f * f;
			right[i] = new Station(0, y, w, y, t);
			left[i] = new Station(0, y, -w, y, t);
		}
		m.blade(right, Mat.BLADE, Mat.EDGE, 0.15f, false);
		m.blade(left, Mat.BLADE, Mat.EDGE, 0.15f, false);
		m.box(-2.1f, 0.35f, -0.4f, 2.1f, 0.95f, 0.4f, Mat.METAL);
		m.loft(new Ring[] {new Ring(-2.1f, 0.65f, 0, 1, 0, 0, 0.01f, 0.01f), new Ring(-2.35f, 0.65f, 0, 1, 0, 0, 0.42f, 0.42f),
				new Ring(-2.7f, 0.65f, 0, 1, 0, 0, 0.01f, 0.01f)}, 10, 0, Mat.METAL, false, false);
		m.loft(new Ring[] {new Ring(2.1f, 0.65f, 0, 1, 0, 0, 0.01f, 0.01f), new Ring(2.35f, 0.65f, 0, 1, 0, 0, 0.42f, 0.42f),
				new Ring(2.7f, 0.65f, 0, 1, 0, 0, 0.01f, 0.01f)}, 10, 0, Mat.METAL, false, false);
		Ring[] grip = new Ring[9];
		for (int i = 0; i < grip.length; i++) {
			float f = i / (float) (grip.length - 1);
			grip[i] = Ring.along(0, 0.35f - 4.8f * f, 0, 0.55f + 0.08f * (float) Math.sin(f * Math.PI), 0.55f + 0.08f * (float) Math.sin(f * Math.PI));
		}
		m.loft(grip, 12, 0, Mat.WRAP, false, false);
		Ring[] pommel = new Ring[7];
		for (int i = 0; i < pommel.length; i++) {
			double a = Math.PI * i / (pommel.length - 1);
			float r = 0.85f * (float) Math.sin(a);
			pommel[i] = Ring.along(0, -5.3f + 0.85f * (float) Math.cos(a), 0, Math.max(0.01f, r), Math.max(0.01f, r));
		}
		m.loft(pommel, 12, 0, Mat.METAL, false, false);
	}

	private static void phantomHilt(KnifeMesh m) {
		Ring[] hilt = new Ring[14];
		for (int i = 0; i < hilt.length; i++) {
			float f = i / (float) (hilt.length - 1);
			float y = 1.3f - 6.3f * f;
			float r = f < 0.12f ? 0.9f : f < 0.2f ? 0.72f : f > 0.92f ? 0.85f : (i % 2 == 0 ? 0.7f : 0.64f);
			hilt[i] = Ring.along(0, y, 0, r, r);
		}
		m.loft(hilt, 14, 0, Mat.METAL, true, true);
		m.loft(new Ring[] {Ring.along(0, 0.55f, 0, 0.76f, 0.76f), Ring.along(0, 0.85f, 0, 0.76f, 0.76f)}, 14, 0, Mat.ACCENT, false, false);
		m.loft(new Ring[] {Ring.along(0, -4.35f, 0, 0.88f, 0.88f), Ring.along(0, -4.6f, 0, 0.88f, 0.88f)}, 14, 0, Mat.ACCENT, false, false);
		m.box(0.55f, -1.6f, -0.18f, 1.05f, -0.6f, 0.18f, Mat.ACCENT);
	}

	/** The energy blade, base at the origin, 10 pixels long at full length; scaled along its length as it ignites. */
	private static KnifeMesh phantomBlade() {
		KnifeMesh m = new KnifeMesh();
		float[] radii = {0.26f, 0.62f, 1.05f};
		Mat[] mats = {Mat.ENERGY_CORE, Mat.ENERGY_GLOW, Mat.ENERGY_GLOW};
		for (int layer = 0; layer < radii.length; layer++) {
			float r = radii[layer];
			Ring[] rings = new Ring[8];
			for (int i = 0; i < rings.length; i++) {
				float f = i / (float) (rings.length - 1);
				float y = f < 0.85f ? f / 0.85f * 9.4f : 9.4f + (float) Math.sin((f - 0.85f) / 0.15f * Math.PI / 2) * r;
				float rr = f < 0.85f ? r : r * (float) Math.cos((f - 0.85f) / 0.15f * Math.PI / 2);
				rings[i] = Ring.along(0, y, 0, Math.max(0.01f, rr), Math.max(0.01f, rr));
			}
			m.loft(rings, 12, 0, mats[layer], false, false);
		}
		return m;
	}

	private static void katana(KnifeMesh m) {
		int n = 18;
		Station[] st = new Station[n + 1];
		for (int i = 0; i <= n; i++) {
			float f = i / (float) n;
			float y = 1.2f + 12.6f * f;
			float bend = -0.9f * f * f;
			float sx = -0.5f + bend;
			float ex = 0.55f + bend;
			if (f > 0.9f) {
				float g = (f - 0.9f) / 0.1f;
				ex = ex + (sx + 0.15f - ex) * g;
				sx = sx + 0.15f * g;
			}
			st[i] = new Station(sx, y, ex, y + (f > 0.9f ? 0.25f * (f - 0.9f) / 0.1f : 0), 0.42f - 0.25f * f);
		}
		m.blade(st, Mat.BLADE, Mat.EDGE, 0.55f, true);
		m.box(-0.62f, 0.55f, -0.3f, 0.68f, 1.25f, 0.3f, Mat.ACCENT);
		m.loft(new Ring[] {Ring.along(0, 0.15f, 0, 1.65f, 1.35f), Ring.along(0, 0.55f, 0, 1.65f, 1.35f)}, 18, 0.15f, Mat.METAL, true, true);
		Ring[] grip = new Ring[9];
		for (int i = 0; i < grip.length; i++) {
			float f = i / (float) (grip.length - 1);
			grip[i] = Ring.along(0, 0.15f - 6.4f * f, 0, 0.62f - 0.05f * (float) Math.sin(f * Math.PI), 0.46f);
		}
		m.loft(grip, 12, 0.35f, Mat.WRAP, false, false);
		m.loft(new Ring[] {Ring.along(0, -6.25f, 0, 0.66f, 0.5f), Ring.along(0, -6.75f, 0, 0.58f, 0.44f)}, 12, 0.35f, Mat.METAL, false, true);
	}

	private static void scythe(KnifeMesh m) {
		Ring[] pole = new Ring[6];
		for (int i = 0; i < pole.length; i++) {
			float f = i / (float) (pole.length - 1);
			pole[i] = Ring.along(0.15f * (float) Math.sin(f * Math.PI), -6.5f + 10.2f * f, 0, 0.42f, 0.42f);
		}
		m.loft(pole, 10, 0, Mat.HANDLE, true, true);
		m.loft(new Ring[] {Ring.along(0, 2.4f, 0, 0.55f, 0.55f), Ring.along(0, 3.2f, 0, 0.55f, 0.55f)}, 10, 0, Mat.METAL, false, false);
		m.loft(new Ring[] {Ring.along(0, -0.8f, 0, 0.5f, 0.5f), Ring.along(0, -0.3f, 0, 0.5f, 0.5f)}, 10, 0, Mat.ACCENT, false, false);
		int n = 16;
		Station[] st = new Station[n + 1];
		for (int i = 0; i <= n; i++) {
			float f = i / (float) n;
			double angle = Math.toRadians(128 - 112 * f);
			float cx = 4.2f;
			float cy = -2.2f;
			float rs = 6.0f;
			float width = 1.55f * (1 - (float) Math.pow(f, 1.3));
			st[i] = new Station(cx + (float) Math.cos(angle) * rs, cy + (float) Math.sin(angle) * rs, cx + (float) Math.cos(angle) * (rs - width),
					cy + (float) Math.sin(angle) * (rs - width), 0.42f - 0.3f * f);
		}
		m.blade(st, Mat.BLADE, Mat.EDGE, 0.45f, true);
	}

	private void clips() {
		if (draw != null) return;
		float reach = this == KATANA || this == SCYTHE ? 1.35f : this == PHANTOM ? 1.2f : this == KARAMBIT ? 0.9f : 1f;
		slashes = new Clip[] {
				Clip.of("slash right to left", 430)
						.key(0, Ease.LINEAR)
						.key(0.2f, Ease.OUT, Clip.X, 0.05f, Clip.Y, 0.05f, Clip.SWEEP, -24, Clip.PITCH, 8)
						.key(0.5f, Ease.OUT, Clip.X, -0.2f * reach, Clip.Y, -0.06f, Clip.Z, -0.04f, Clip.SWEEP, 58 * reach, Clip.PITCH, -6)
						.key(1, Ease.IN_OUT),
				Clip.of("slash left to right", 430)
						.key(0, Ease.LINEAR)
						.key(0.2f, Ease.OUT, Clip.X, -0.1f, Clip.Y, 0.04f, Clip.SWEEP, 42, Clip.PITCH, 6)
						.key(0.5f, Ease.OUT, Clip.X, 0.12f * reach, Clip.Y, -0.04f, Clip.Z, -0.03f, Clip.SWEEP, -40 * reach, Clip.PITCH, -6)
						.key(1, Ease.IN_OUT),
				Clip.of("stab", 380)
						.key(0, Ease.LINEAR)
						.key(0.25f, Ease.OUT, Clip.Z, 0.08f, Clip.PITCH, 8, Clip.Y, 0.02f)
						.key(0.5f, Ease.OUT, Clip.Z, -0.3f, Clip.X, -0.1f, Clip.Y, 0.05f, Clip.PITCH, -6, Clip.YAW, -8)
						.key(0.68f, Ease.SINE, Clip.Z, -0.22f, Clip.X, -0.08f, Clip.Y, 0.04f, Clip.PITCH, -4, Clip.YAW, -6)
						.key(1, Ease.IN_OUT)};
		use = Clip.of("heavy stab", 640)
				.key(0, Ease.LINEAR)
				.key(0.32f, Ease.SINE, Clip.Z, 0.12f, Clip.Y, 0.06f, Clip.PITCH, 16, Clip.ROLL, -14)
				.key(0.52f, Ease.OUT, Clip.Z, -0.4f, Clip.X, -0.14f, Clip.Y, 0.02f, Clip.PITCH, -14, Clip.YAW, -10, Clip.ROLL, 10)
				.key(0.7f, Ease.SINE, Clip.Z, -0.32f, Clip.X, -0.12f, Clip.PITCH, -10, Clip.YAW, -8, Clip.ROLL, 8)
				.key(1, Ease.IN_OUT);
		switch (this) {
			case KARAMBIT -> {
				draw = Clip.of("draw", 950)
						.key(0, Ease.LINEAR, Clip.Y, -0.55f, Clip.ROLL, 50, Clip.K_ROLL, -540)
						.key(0.62f, Ease.OUT, Clip.Y, 0.02f, Clip.ROLL, -6, Clip.K_ROLL, -12)
						.key(1, Ease.IN_OUT);
				inspect = Clip.of("inspect", 3300)
						.key(0, Ease.LINEAR)
						.key(0.14f, Ease.OUT, Clip.YAW, -26, Clip.ROLL, 34, Clip.PITCH, 10, Clip.X, -0.04f, Clip.Z, 0.06f)
						.then(0.3f, Ease.SINE, Clip.K_YAW, 160)
						.then(0.42f, Ease.SINE, Clip.K_YAW, 0)
						.then(0.66f, Ease.IN_OUT, Clip.K_ROLL, -720, Clip.ROLL, 12)
						.then(0.8f, Ease.OUT, Clip.K_ROLL, -720, Clip.ROLL, 0, Clip.YAW, -20)
						.key(1, Ease.IN_OUT, Clip.K_ROLL, -720);
			}
			case BUTTERFLY -> {
				draw = Clip.of("draw", 1150)
						.key(0, Ease.LINEAR, Clip.Y, -0.5f, Clip.OPEN, 0)
						.key(0.3f, Ease.OUT, Clip.Y, 0.0f, Clip.OPEN, 0)
						.key(0.52f, Ease.SINE, Clip.OPEN, 0.45f, Clip.HANDLE_B, 140, Clip.K_ROLL, -180)
						.key(0.78f, Ease.OUT, Clip.OPEN, 1, Clip.HANDLE_B, 0, Clip.K_ROLL, -360)
						.key(1, Ease.IN_OUT, Clip.K_ROLL, -360);
				inspect = Clip.of("inspect", 3700)
						.key(0, Ease.LINEAR)
						.key(0.1f, Ease.OUT, Clip.YAW, -22, Clip.ROLL, 26, Clip.PITCH, 8, Clip.Z, 0.05f)
						.then(0.25f, Ease.SINE, Clip.OPEN, 0.5f, Clip.HANDLE_A, -150, Clip.K_ROLL, 180)
						.then(0.38f, Ease.SINE, Clip.OPEN, 0, Clip.HANDLE_A, 0, Clip.K_ROLL, 360)
						.then(0.52f, Ease.SINE, Clip.OPEN, 0.5f, Clip.HANDLE_B, 150, Clip.K_ROLL, 540)
						.then(0.66f, Ease.SINE, Clip.OPEN, 1, Clip.HANDLE_B, 0, Clip.K_ROLL, 720)
						.then(0.82f, Ease.IN_OUT, Clip.K_YAW, 180)
						.key(1, Ease.IN_OUT, Clip.K_ROLL, 720, Clip.K_YAW, 360);
			}
			case BAYONET -> {
				draw = Clip.of("draw", 820)
						.key(0, Ease.LINEAR, Clip.X, 0.35f, Clip.Y, -0.45f, Clip.K_YAW, 360, Clip.ROLL, -30)
						.key(0.68f, Ease.OUT, Clip.X, -0.01f, Clip.Y, 0.03f, Clip.K_YAW, 0, Clip.ROLL, 4)
						.key(1, Ease.IN_OUT);
				inspect = Clip.of("inspect", 3200)
						.key(0, Ease.LINEAR)
						.key(0.14f, Ease.OUT, Clip.YAW, -24, Clip.ROLL, 40, Clip.PITCH, 10, Clip.Z, 0.06f)
						.then(0.34f, Ease.SINE, Clip.K_YAW, 180)
						.then(0.5f, Ease.OUT, Clip.SLIDE, 4, Clip.K_PITCH, -180)
						.then(0.62f, Ease.SINE, Clip.SLIDE, 0, Clip.K_PITCH, -360)
						.then(0.85f, Ease.IN_OUT, Clip.K_YAW, 360)
						.key(1, Ease.IN_OUT, Clip.K_YAW, 360, Clip.K_PITCH, -360);
			}
			case HUNTSMAN -> {
				draw = Clip.of("draw", 820)
						.key(0, Ease.LINEAR, Clip.Y, -0.5f, Clip.PITCH, -60, Clip.YAW, 20)
						.key(0.55f, Ease.OUT, Clip.Y, 0.04f, Clip.PITCH, 8, Clip.YAW, -4)
						.key(1, Ease.IN_OUT);
				inspect = Clip.of("inspect", 3000)
						.key(0, Ease.LINEAR)
						.key(0.15f, Ease.OUT, Clip.YAW, -26, Clip.ROLL, 30, Clip.PITCH, 6, Clip.Z, 0.06f, Clip.X, -0.05f)
						.then(0.35f, Ease.SINE, Clip.ROLL, 75)
						.then(0.55f, Ease.SINE, Clip.K_YAW, 180)
						.then(0.72f, Ease.SINE, Clip.ROLL, 0, Clip.K_YAW, 360)
						.key(1, Ease.IN_OUT, Clip.K_YAW, 360);
			}
			case DAGGER -> {
				draw = Clip.of("draw", 900)
						.key(0, Ease.LINEAR, Clip.Y, -0.45f)
						.key(0.28f, Ease.OUT, Clip.Y, -0.04f)
						.key(0.55f, Ease.SINE, Clip.Y, 0.02f, Clip.SLIDE, 6, Clip.K_PITCH, -200)
						.key(0.82f, Ease.SINE, Clip.SLIDE, 0, Clip.K_PITCH, -360)
						.key(1, Ease.OUT, Clip.K_PITCH, -360);
				inspect = Clip.of("inspect", 3000)
						.key(0, Ease.LINEAR)
						.key(0.12f, Ease.OUT, Clip.YAW, -22, Clip.ROLL, 28, Clip.PITCH, 8, Clip.Z, 0.05f)
						.then(0.32f, Ease.SINE, Clip.K_ROLL, 360, Clip.SLIDE, 2)
						.then(0.5f, Ease.SINE, Clip.K_ROLL, 720, Clip.SLIDE, 0)
						.then(0.7f, Ease.SINE, Clip.K_YAW, 180)
						.key(1, Ease.IN_OUT, Clip.K_ROLL, 720, Clip.K_YAW, 360);
			}
			case PHANTOM -> {
				draw = Clip.of("draw", 900)
						.key(0, Ease.LINEAR, Clip.Y, -0.42f, Clip.OPEN, 0)
						.key(0.38f, Ease.OUT, Clip.Y, 0.0f, Clip.OPEN, 0)
						.key(0.62f, Ease.OUT, Clip.OPEN, 1.08f)
						.key(1, Ease.IN_OUT);
				inspect = Clip.of("inspect", 3200)
						.key(0, Ease.LINEAR)
						.key(0.12f, Ease.OUT, Clip.YAW, -20, Clip.ROLL, 24, Clip.PITCH, 8, Clip.Z, 0.04f)
						.then(0.4f, Ease.IN_OUT, Clip.K_ROLL, -360, Clip.OPEN, 0.85f)
						.then(0.62f, Ease.IN_OUT, Clip.K_ROLL, -720, Clip.OPEN, 1.08f)
						.then(0.8f, Ease.SINE, Clip.K_YAW, 180, Clip.OPEN, 1)
						.key(1, Ease.IN_OUT, Clip.K_ROLL, -720, Clip.K_YAW, 360);
			}
			case KATANA -> {
				draw = Clip.of("draw", 950)
						.key(0, Ease.LINEAR, Clip.X, -0.3f, Clip.Y, -0.2f, Clip.YAW, -40, Clip.ROLL, 30)
						.key(0.6f, Ease.OUT, Clip.X, 0.03f, Clip.YAW, 6, Clip.ROLL, -4)
						.key(1, Ease.IN_OUT);
				inspect = Clip.of("inspect", 3700)
						.key(0, Ease.LINEAR)
						.key(0.16f, Ease.OUT, Clip.ROLL, 64, Clip.YAW, -18, Clip.Y, 0.08f, Clip.Z, 0.04f)
						.then(0.42f, Ease.SINE, Clip.X, -0.12f)
						.then(0.58f, Ease.SINE, Clip.K_YAW, 180)
						.then(0.74f, Ease.SINE, Clip.X, 0.02f)
						.key(1, Ease.IN_OUT, Clip.K_YAW, 360);
			}
			case SCYTHE -> {
				draw = Clip.of("draw", 1000)
						.key(0, Ease.LINEAR, Clip.X, 0.35f, Clip.Y, -0.5f, Clip.K_ROLL, 360)
						.key(0.68f, Ease.OUT, Clip.Y, 0.03f, Clip.K_ROLL, 0, Clip.X, 0)
						.key(1, Ease.IN_OUT);
				inspect = Clip.of("inspect", 3000)
						.key(0, Ease.LINEAR)
						.key(0.14f, Ease.OUT, Clip.YAW, -16, Clip.ROLL, 20, Clip.Y, 0.06f, Clip.Z, 0.05f)
						.then(0.45f, Ease.IN_OUT, Clip.K_ROLL, 360)
						.then(0.72f, Ease.IN_OUT, Clip.K_ROLL, 720, Clip.YAW, -10)
						.key(1, Ease.IN_OUT, Clip.K_ROLL, 720);
			}
		}
	}

	/** The id used in saved settings: the name in lower case. */
	public String id() {
		return name().toLowerCase(Locale.ROOT);
	}
}
