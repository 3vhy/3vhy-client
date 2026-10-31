package dev.eventide.client.knife;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The knives, each its own shape built from {@link Shape} pieces (ground blades, lofted grips, plates,
 * pins and rings) after the real knife it's named for, with its own draw, inspect and slash animations
 * ({@link Clip}). Shapes are made the first time a knife is drawn and kept. The order is the order the
 * shader numbers them in (knife_surface.glsl's BLADE_RANGE and HOLE), so new knives go at the end.
 */
public enum KnifeModel {
	KARAMBIT("Karambit", 1.0f, 0, -5.9f, -4.71f, 0.95f, 0.48f, Family.CURVED),
	M9_BAYONET("M9 Bayonet", 0.86f, 0, -2.0f, -1.61f, 0.82f, 0.62f, Family.FIXED),
	BAYONET("Bayonet", 0.86f, 0, -2.0f, -1.61f, 0.78f, 0.6f, Family.FIXED),
	FLIP("Flip Knife", 1.0f, -0.25f, 0.45f, -2.19f, 1.0f, 0.6f, Family.FOLDING),
	BUTTERFLY("Butterfly", 0.95f, -0.45f, 0.5f, -1.53f, 0.6f, 0.66f, Family.BUTTERFLY),
	HUNTSMAN("Huntsman", 0.9f, 0, -1.5f, -1.57f, 0.86f, 0.64f, Family.FIXED),
	GUT("Gut Knife", 0.98f, 0, -1.5f, -1.47f, 0.78f, 0.6f, Family.FIXED),
	BOWIE("Bowie", 0.82f, 0, -1.8f, -1.7f, 0.9f, 0.66f, Family.FIXED),
	FALCHION("Falchion", 0.95f, -0.2f, 0.45f, -2.28f, 1.0f, 0.62f, Family.FOLDING),
	NAVAJA("Navaja", 1.0f, -0.2f, 0.4f, -2.19f, 1.0f, 0.5f, Family.FOLDING),
	STILETTO("Stiletto", 0.9f, -0.1f, 0.4f, -2.37f, 1.0f, 0.48f, Family.FOLDING),
	TALON("Talon", 0.98f, -0.25f, 0.4f, -4.63f, 1.0f, 0.55f, Family.CURVED),
	URSUS("Ursus", 0.94f, -0.25f, 0.5f, -2.29f, 1.0f, 0.6f, Family.FOLDING),
	PARACORD("Paracord", 0.98f, 0, -1.5f, -2.32f, 0.82f, 0.6f, Family.FIXED),
	SURVIVAL("Survival", 0.95f, 0, -1.5f, -1.53f, 0.84f, 0.64f, Family.FIXED),
	SKELETON("Skeleton", 0.98f, 0, -1.5f, -1.82f, 0.85f, 0.3f, Family.FIXED),
	NOMAD("Nomad", 0.96f, 0, -1.5f, -2.16f, 1.05f, 0.48f, Family.FIXED),
	CLASSIC("Classic", 0.92f, 0, -1.5f, -1.53f, 0.74f, 0.58f, Family.FIXED),
	SHADOW_DAGGERS("Shadow Daggers", 1.15f, 0, -0.6f, -0.6f, 0.62f, 0.52f, Family.PUSH),
	DAGGER("Dagger", 0.95f, 0, -1.0f, -1.63f, 0.6f, 0.6f, Family.FIXED),
	PHANTOM("Phantom", 0.85f, 0, -1.0f, -1.78f, 0.7f, 0.7f, Family.FIXED),
	KATANA("Katana", 0.66f, 0, -2.0f, -2.58f, 0.62f, 0.46f, Family.FIXED),
	SCYTHE("Scythe", 0.72f, 0, -1.0f, -2.8f, 0.42f, 0.42f, Family.FIXED);

	/** How a knife is built and handled: fixed blades, folders whose blade swings out, and the odd ones. */
	enum Family {
		FIXED,
		FOLDING,
		BUTTERFLY,
		CURVED,
		PUSH
	}

	/** What a part of a knife does when the knife moves. */
	enum PartKind {
		/** Moves with the knife. */
		BODY,
		/** A butterfly knife's handles, turning about their pins. */
		HANDLE_A,
		HANDLE_B,
		/** A folding knife's blade, swinging out about its pivot as the knife opens. */
		FOLD,
		/** An energy blade, as long as the pose's {@link Clip#OPEN}. */
		ENERGY
	}

	record Part(Shape mesh, PartKind kind, float pivotX, float pivotY) {
	}

	public final String label;
	/** How big this knife is drawn, so long blades don't fill the screen. */
	final float scale;
	/** The point it spins about in tricks, in pixels: a karambit's ring, a butterfly knife's pin. */
	final float spinX;
	final float spinY;
	/** Where your fist closes on it, in pixels along the handle: the middle of the four fingers. */
	final float grip;
	/** How wide (towards the edge) and thick the handle is where you hold it, half each way, in pixels. */
	final float handleA;
	final float handleB;
	final Family family;
	private List<Part> parts;
	private Clip draw;
	private Clip inspect;
	private Clip[] slashes;
	private Clip use;

	KnifeModel(String label, float scale, float spinX, float spinY, float grip, float handleA, float handleB, Family family) {
		this.label = label;
		this.scale = scale;
		this.spinX = spinX;
		this.spinY = spinY;
		this.grip = grip;
		this.handleA = handleA;
		this.handleB = handleB;
		this.family = family;
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
			if (model.label.equalsIgnoreCase(label) || model.name().equalsIgnoreCase(label) || model.id().equalsIgnoreCase(label)) return model;
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

	private List<Part> build() {
		List<Part> out = new ArrayList<>();
		Shape body = new Shape();
		switch (this) {
			case KARAMBIT -> karambit(body);
			case M9_BAYONET -> m9(body);
			case BAYONET -> bayonet(body);
			case FLIP -> {
				flipHandle(body);
				out.add(new Part(flipBlade(), PartKind.FOLD, -0.25f, 0.45f));
			}
			case BUTTERFLY -> {
				butterflyBlade(body);
				out.add(new Part(butterflyHandle(-1), PartKind.HANDLE_A, -0.45f, 0.5f));
				out.add(new Part(butterflyHandle(1), PartKind.HANDLE_B, 0.45f, 0.5f));
			}
			case HUNTSMAN -> huntsman(body);
			case GUT -> gut(body);
			case BOWIE -> bowie(body);
			case FALCHION -> {
				folderHandle(body, 5.6f, 0.62f, KnifeShading.POLYMER, true);
				out.add(new Part(falchionBlade(), PartKind.FOLD, -0.2f, 0.45f));
			}
			case NAVAJA -> {
				folderHandle(body, 5.8f, 0.5f, KnifeShading.WOOD, false);
				out.add(new Part(navajaBlade(), PartKind.FOLD, -0.2f, 0.4f));
			}
			case STILETTO -> {
				folderHandle(body, 6.2f, 0.48f, KnifeShading.WOOD, false);
				out.add(new Part(stilettoBlade(), PartKind.FOLD, -0.1f, 0.4f));
			}
			case TALON -> {
				talonHandle(body);
				out.add(new Part(talonBlade(), PartKind.FOLD, -0.25f, 0.4f));
			}
			case URSUS -> {
				folderHandle(body, 5.8f, 0.6f, KnifeShading.RUBBER, true);
				out.add(new Part(ursusBlade(), PartKind.FOLD, -0.25f, 0.5f));
			}
			case PARACORD -> paracord(body);
			case SURVIVAL -> survival(body);
			case SKELETON -> skeleton(body);
			case NOMAD -> nomad(body);
			case CLASSIC -> classic(body);
			case SHADOW_DAGGERS -> pushDagger(body);
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

	/** The spine and edge of a blade at {@code t} (0 at the base, 1 at the tip): {sx, sy, ex, ey, half thickness}. */
	private interface Profile {
		float[] at(float t);
	}

	private static Shape.Blade blade(int steps, Profile profile) {
		Shape.Blade b = new Shape.Blade();
		for (int i = 0; i <= steps; i++) {
			float[] s = profile.at(i / (float) steps);
			b.station(s[0], s[1], s[2], s[3], s[4]);
		}
		return b;
	}

	private static float smooth(float t) {
		t = Math.max(0, Math.min(1, t));
		return t * t * (3 - 2 * t);
	}

	/**
	 * A straight blade from {@code y0} to its tip at {@code y1}: the spine at {@code sx} and the edge at
	 * {@code ex}, both meeting at {@code tipX}. The spine runs straight until {@code clip} (0..1 along the
	 * blade) and then falls to the tip, rounded ({@code concave} false, a drop point) or hollowed (true, a
	 * clip point); the edge stays straight until {@code belly} and then sweeps up to the tip.
	 */
	private static Profile straight(float y0, float y1, float sx, float ex, float tipX, float clip, boolean concave, float belly, float h0, float h1) {
		return t -> {
			float y = y0 + (y1 - y0) * t;
			float x;
			if (t <= clip) {
				x = sx;
			} else {
				float u = (t - clip) / (1 - clip);
				float shape = concave ? 1 - (1 - u) * (1 - u) : u * u;
				x = sx + (tipX - sx) * shape;
			}
			float e;
			if (t <= belly) {
				e = ex;
			} else {
				float u = (t - belly) / (1 - belly);
				e = ex + (tipX - ex) * (float) Math.pow(u, 1.7);
			}
			return new float[] {x, y, e, y, h0 + (h1 - h0) * t};
		};
	}

	/** Saw teeth along the spine between {@code from} and {@code to} (0..1 along the blade). */
	private static Profile teeth(Profile base, float from, float to, int count, float depth) {
		return t -> {
			float[] s = base.at(t);
			if (t > from && t < to) {
				float u = (t - from) / (to - from) * count;
				float tooth = u - (float) Math.floor(u);
				float shape = tooth < 0.6f ? tooth / 0.6f : (1 - tooth) / 0.4f;
				s[0] -= depth * shape;
			}
			return s;
		};
	}

	/** A grip lofted along Y from {@code top} down to {@code bottom}, ribbed every {@code ribs} pixels if above zero. */
	private static void grip(Shape m, float top, float bottom, float rx, float rz, float squareness, float ribs, float bulge, int material, int bottomEnd) {
		int rings = Math.max(6, Math.round((top - bottom) / 0.25f));
		Shape.Ring[] out = new Shape.Ring[rings + 1];
		for (int i = 0; i <= rings; i++) {
			float f = i / (float) rings;
			float y = top + (bottom - top) * f;
			float swell = 1 + bulge * (float) Math.sin(Math.PI * f);
			float rib = ribs > 0 ? 1 - 0.07f * (float) Math.pow(Math.abs(Math.sin(Math.PI * (top - y) / ribs)), 6) : 1;
			out[i] = Shape.Ring.along(0, y, 0, rx * swell * rib, rz * swell * rib);
		}
		m.loft(out, 16, squareness, material, Shape.CAP, bottomEnd, false);
	}

	private static float[] rect(float x0, float y0, float x1, float y1) {
		return new float[] {x0, y0, x1, y0, x1, y1, x0, y1};
	}

	private static void karambit(Shape m) {
		Shape.Blade b = blade(26, t -> {
			double angle = Math.toRadians(182 - 160 * t);
			float cx = 3.2f;
			float cy = 1.0f;
			float spine = 4.3f + 0.15f * t;
			float width = 1.5f * (1 - (float) Math.pow(t, 1.5f)) + 0.02f;
			float edge = spine - width;
			return new float[] {cx + (float) Math.cos(angle) * spine, cy + (float) Math.sin(angle) * spine, cx + (float) Math.cos(angle) * edge,
					cy + (float) Math.sin(angle) * edge, 0.26f - 0.16f * t};
		});
		b.grind = 0.42f;
		b.swedgeFrom = 0.72f;
		b.swedgeWidth = 0.3f;
		m.blade(b);
		m.plate(new float[] {-1.25f, 1.15f, 0.55f, 1.15f, 0.95f, 0.4f, 0.75f, -0.3f, 1.0f, -1.0f, 0.7f, -1.6f, 0.95f, -2.3f, 0.62f, -2.9f, 0.88f, -3.6f,
				0.72f, -4.4f, 0.55f, -4.85f, -0.55f, -4.85f, -0.95f, -3.9f, -1.0f, -2.2f, -1.15f, -0.4f}, -0.48f, 0.48f, 0.22f, KnifeShading.POLYMER);
		m.plate(new float[] {-0.25f, 1.25f, 0.35f, 1.25f, 0.5f, 0.6f, -0.45f, 0.6f}, -0.52f, 0.52f, 0.12f, KnifeShading.HARDWARE);
		m.pin(0.0f, -0.6f, -0.55f, 0.55f, 0.22f, KnifeShading.HARDWARE);
		m.pin(-0.05f, -3.4f, -0.55f, 0.55f, 0.22f, KnifeShading.HARDWARE);
		m.torus(0, -5.95f, 0, 1.15f, 0.36f, 0.42f, 24, 10, KnifeShading.RING);
	}

	private static void m9(Shape m) {
		Profile shape = straight(1.2f, 12.6f, -0.95f, 1.12f, 0.2f, 0.66f, true, 0.6f, 0.3f, 0.12f);
		Shape.Blade b = blade(64, teeth(shape, 0.2f, 0.58f, 9, 0.32f));
		b.grind = 0.52f;
		b.swedgeFrom = 0.68f;
		b.swedgeWidth = 0.32f;
		b.fullerFrom = 0.08f;
		b.fullerTo = 0.5f;
		b.fullerCentre = 0.42f;
		b.fullerWidth = 0.13f;
		b.fullerDepth = 0.1f;
		m.blade(b);
		m.plate(new float[] {-2.15f, 0.55f, 2.55f, 0.55f, 2.75f, 0.95f, 2.45f, 1.25f, -1.95f, 1.25f, -2.3f, 0.95f}, -0.42f, 0.42f, 0.18f, KnifeShading.GUARD);
		m.torus(-2.55f, 0.9f, 0, 0.62f, 0.2f, 0.28f, 18, 8, KnifeShading.GUARD);
		grip(m, 0.6f, -5.3f, 0.82f, 0.62f, 0.45f, 0.55f, 0.08f, KnifeShading.POLYMER, Shape.OPEN);
		m.loft(new Shape.Ring[] {Shape.Ring.along(0, -5.25f, 0, 0.88f, 0.68f), Shape.Ring.along(0, -5.95f, 0, 0.92f, 0.7f),
				Shape.Ring.along(0, -6.3f, 0, 0.62f, 0.48f)}, 16, 0.4f, KnifeShading.HARDWARE, Shape.OPEN, Shape.DOME, false);
		m.torus(0, -6.75f, 0, 0.42f, 0.13f, 0.16f, 14, 6, KnifeShading.HARDWARE);
	}

	private static void bayonet(Shape m) {
		Profile shape = straight(1.2f, 12.4f, -0.85f, 0.95f, -0.1f, 0.74f, false, 0.7f, 0.28f, 0.11f);
		Shape.Blade b = blade(48, shape);
		b.grind = 0.55f;
		b.swedgeFrom = 0.72f;
		b.swedgeWidth = 0.3f;
		b.fullerFrom = 0.06f;
		b.fullerTo = 0.62f;
		b.fullerCentre = 0.3f;
		b.fullerWidth = 0.14f;
		b.fullerDepth = 0.1f;
		m.blade(b);
		m.plate(new float[] {-1.6f, 0.55f, 2.0f, 0.55f, 2.1f, 1.25f, -1.7f, 1.25f}, -0.45f, 0.45f, 0.2f, KnifeShading.GUARD);
		m.torus(-2.05f, 0.9f, 0, 0.6f, 0.2f, 0.26f, 18, 8, KnifeShading.GUARD);
		grip(m, 0.6f, -5.2f, 0.78f, 0.6f, 0.4f, 0.48f, 0.06f, KnifeShading.RUBBER, Shape.OPEN);
		m.loft(new Shape.Ring[] {Shape.Ring.along(0, -5.15f, 0, 0.84f, 0.66f), Shape.Ring.along(0, -5.9f, 0, 0.9f, 0.7f),
				Shape.Ring.along(0, -6.2f, 0, 0.6f, 0.45f)}, 16, 0.4f, KnifeShading.HARDWARE, Shape.OPEN, Shape.DOME, false);
	}

	private static void flipHandle(Shape m) {
		float[] scale = {-0.95f, 0.95f, 0.75f, 0.95f, 0.98f, 0.2f, 0.7f, -0.9f, 0.95f, -1.9f, 0.9f, -4.6f, 0.55f, -5.35f, -0.65f, -5.35f, -0.95f, -4.4f,
				-1.05f, -1.8f};
		m.plate(scale, 0.18f, 0.58f, 0.18f, KnifeShading.POLYMER);
		m.plate(scale, -0.58f, -0.18f, 0.18f, KnifeShading.POLYMER);
		m.plate(new float[] {-0.85f, 0.8f, 0.6f, 0.8f, 0.8f, -4.5f, -0.85f, -4.9f}, -0.2f, 0.2f, 0.05f, KnifeShading.HARDWARE);
		m.plate(new float[] {-1.2f, 0.4f, -0.95f, 0.4f, -0.95f, -3.6f, -1.2f, -3.4f}, 0.45f, 0.66f, 0.08f, KnifeShading.HARDWARE);
		m.pin(-0.25f, 0.45f, -0.66f, 0.66f, 0.32f, KnifeShading.HARDWARE);
		m.pin(-0.1f, -4.6f, -0.62f, 0.62f, 0.2f, KnifeShading.HARDWARE);
	}

	private static Shape flipBlade() {
		Shape m = new Shape();
		Shape.Blade b = blade(32, straight(0.2f, 8.6f, -0.9f, 1.0f, 0.15f, 0.55f, false, 0.55f, 0.24f, 0.1f));
		b.grind = 0.5f;
		m.blade(b);
		m.pin(-0.55f, 1.4f, -0.45f, 0.45f, 0.17f, KnifeShading.HARDWARE);
		return m;
	}

	private static void butterflyBlade(Shape m) {
		m.plate(rect(-0.75f, -0.15f, 0.75f, 1.25f), -0.17f, 0.17f, 0.08f, KnifeShading.BLADE);
		Shape.Blade b = blade(32, straight(1.2f, 9.4f, -0.62f, 0.78f, 0.05f, 0.64f, true, 0.66f, 0.17f, 0.08f));
		b.grind = 0.5f;
		b.swedgeFrom = 0.66f;
		m.blade(b);
	}

	/** One butterfly handle hanging from its pin at the origin, on one side of the blade ({@code side} -1 or 1). */
	private static Shape butterflyHandle(int side) {
		Shape m = new Shape();
		float z0 = side < 0 ? -0.66f : 0.2f;
		float z1 = side < 0 ? -0.2f : 0.66f;
		m.plate(new float[] {-0.55f, 0.45f, 0.55f, 0.45f, 0.6f, -7.5f, -0.6f, -7.5f}, z0, z1, 0.14f, KnifeShading.HANDLE_PAINT);
		m.pin(0, 0, z0 - 0.04f, z1 + 0.04f, 0.2f, KnifeShading.HARDWARE);
		m.pin(0, -7.0f, z0 - 0.04f, z1 + 0.04f, 0.16f, KnifeShading.HARDWARE);
		if (side > 0) m.plate(rect(-0.55f, -8.2f, 0.55f, -7.45f), -0.66f, 0.66f, 0.12f, KnifeShading.HARDWARE);
		return m;
	}

	private static void huntsman(Shape m) {
		Profile shape = straight(1.2f, 11.2f, -0.95f, 1.35f, -0.15f, 0.58f, true, 0.58f, 0.3f, 0.12f);
		Shape.Blade b = blade(48, teeth(shape, 0.06f, 0.3f, 6, 0.24f));
		b.grind = 0.5f;
		b.swedgeFrom = 0.6f;
		b.swedgeWidth = 0.35f;
		m.blade(b);
		m.plate(new float[] {-1.55f, 0.5f, 1.95f, 0.5f, 2.05f, 1.25f, -1.6f, 1.25f}, -0.42f, 0.42f, 0.18f, KnifeShading.HARDWARE);
		grip(m, 0.55f, -5.0f, 0.86f, 0.64f, 0.5f, 1.25f, 0.1f, KnifeShading.RUBBER, Shape.OPEN);
		m.loft(new Shape.Ring[] {Shape.Ring.along(0.05f, -4.95f, 0, 0.9f, 0.66f), Shape.Ring.along(0.05f, -5.55f, 0, 0.78f, 0.56f)}, 16, 0.5f,
				KnifeShading.HARDWARE, Shape.OPEN, Shape.DOME, false);
	}

	private static void gut(Shape m) {
		Profile base = straight(1.0f, 9.0f, -0.85f, 1.05f, 0.1f, 0.55f, false, 0.55f, 0.26f, 0.1f);
		Profile hooked = t -> {
			float[] s = base.at(t);
			if (t > 0.5f && t < 0.66f) {
				float u = (t - 0.5f) / 0.16f;
				s[0] += 0.9f * (float) Math.sin(Math.PI * Math.pow(u, 0.7));
			}
			return s;
		};
		Shape.Blade b = blade(40, hooked);
		b.grind = 0.5f;
		m.blade(b);
		m.plate(new float[] {-1.2f, 0.45f, 1.5f, 0.45f, 1.6f, 1.05f, -1.25f, 1.05f}, -0.38f, 0.38f, 0.16f, KnifeShading.HARDWARE);
		grip(m, 0.5f, -4.9f, 0.78f, 0.6f, 0.5f, 0, 0.12f, KnifeShading.RUBBER, Shape.DOME);
	}

	private static void bowie(Shape m) {
		Shape.Blade b = blade(48, straight(1.2f, 12.8f, -1.05f, 1.45f, 0.0f, 0.55f, true, 0.6f, 0.32f, 0.12f));
		b.grind = 0.48f;
		b.swedgeFrom = 0.56f;
		b.swedgeWidth = 0.38f;
		m.blade(b);
		m.plate(new float[] {-2.3f, 0.55f, 2.4f, 0.55f, 2.7f, 1.2f, -2.6f, 1.2f}, -0.4f, 0.4f, 0.18f, KnifeShading.GUARD);
		grip(m, 0.6f, -5.4f, 0.9f, 0.66f, 0.5f, 1.4f, 0.12f, KnifeShading.WOOD, Shape.OPEN);
		m.loft(new Shape.Ring[] {Shape.Ring.along(0, -5.35f, 0, 0.96f, 0.7f), Shape.Ring.along(0, -6.0f, 0, 0.8f, 0.58f)}, 16, 0.4f,
				KnifeShading.GUARD, Shape.OPEN, Shape.DOME, false);
	}

	/** A folding knife's handle: two scales on a steel liner, the pivot at the top, from the bolster down {@code length}. */
	private static void folderHandle(Shape m, float length, float half, int material, boolean clip) {
		float[] scale = {-0.95f, 0.9f, 0.8f, 0.9f, 0.95f, -0.4f, 0.85f, -length * 0.55f, 0.95f, -length + 0.6f, 0.55f, -length, -0.6f, -length,
				-0.95f, -length + 0.8f, -1.05f, -length * 0.4f};
		m.plate(scale, 0.16f, half, 0.16f, material);
		m.plate(scale, -half, -0.16f, 0.16f, material);
		m.plate(new float[] {-0.85f, 0.75f, 0.7f, 0.75f, 0.85f, -length + 0.5f, -0.85f, -length + 0.4f}, -0.18f, 0.18f, 0.05f, KnifeShading.HARDWARE);
		m.plate(new float[] {-0.98f, 1.0f, 0.9f, 1.0f, 0.95f, -0.3f, -1.05f, -0.4f}, -half - 0.02f, half + 0.02f, 0.18f, KnifeShading.HARDWARE);
		m.pin(-0.2f, 0.45f, -half - 0.06f, half + 0.06f, 0.3f, KnifeShading.HARDWARE);
		if (clip) m.plate(new float[] {-1.2f, 0.2f, -0.95f, 0.2f, -0.95f, -length * 0.62f, -1.2f, -length * 0.58f}, half - 0.05f, half + 0.12f, 0.06f,
				KnifeShading.HARDWARE);
	}

	private static Shape falchionBlade() {
		Shape m = new Shape();
		Profile base = straight(0.2f, 10.0f, -0.85f, 1.05f, -0.75f, 0.5f, true, 0.5f, 0.24f, 0.1f);
		Profile curved = t -> {
			float[] s = base.at(t);
			float bend = -0.9f * t * t;
			s[0] += bend;
			s[2] += bend;
			return s;
		};
		Shape.Blade b = blade(36, curved);
		b.grind = 0.5f;
		b.swedgeFrom = 0.55f;
		m.blade(b);
		return m;
	}

	private static Shape navajaBlade() {
		Shape m = new Shape();
		Profile base = straight(0.2f, 8.8f, -0.7f, 0.9f, 0.4f, 0.6f, true, 0.4f, 0.2f, 0.08f);
		Profile curved = t -> {
			float[] s = base.at(t);
			s[0] += 0.5f * t * t;
			s[2] += 0.5f * t * t;
			return s;
		};
		Shape.Blade b = blade(32, curved);
		b.grind = 0.45f;
		m.blade(b);
		return m;
	}

	private static Shape stilettoBlade() {
		Shape m = new Shape();
		Shape.Blade b = blade(32, straight(0.2f, 10.2f, -0.55f, 0.6f, 0.0f, 0.5f, true, 0.5f, 0.2f, 0.08f));
		b.grind = 0.5f;
		b.swedgeFrom = 0.5f;
		b.swedgeWidth = 0.45f;
		m.blade(b);
		return m;
	}

	private static void talonHandle(Shape m) {
		float[] scale = {-0.95f, 0.85f, 0.8f, 0.85f, 1.0f, -0.6f, 0.75f, -2.4f, 0.4f, -4.5f, -0.2f, -5.4f, -0.8f, -5.3f, -0.95f, -4.2f, -1.05f, -1.8f};
		m.plate(scale, 0.16f, 0.55f, 0.18f, KnifeShading.HANDLE_PAINT);
		m.plate(scale, -0.55f, -0.16f, 0.18f, KnifeShading.HANDLE_PAINT);
		m.plate(new float[] {-0.85f, 0.7f, 0.7f, 0.7f, 0.6f, -4.6f, -0.85f, -4.8f}, -0.18f, 0.18f, 0.05f, KnifeShading.HARDWARE);
		m.pin(-0.25f, 0.4f, -0.6f, 0.6f, 0.3f, KnifeShading.HARDWARE);
		m.torus(-0.4f, -5.9f, 0, 0.75f, 0.24f, 0.3f, 18, 8, KnifeShading.HARDWARE);
	}

	private static Shape talonBlade() {
		Shape m = new Shape();
		Shape.Blade b = blade(28, t -> {
			double angle = Math.toRadians(185 - 120 * t);
			float cx = 3.4f;
			float cy = 0.3f;
			float spine = 4.2f;
			float width = 1.6f * (1 - (float) Math.pow(t, 1.4f)) + 0.02f;
			return new float[] {cx + (float) Math.cos(angle) * spine, cy + (float) Math.sin(angle) * spine * 1.25f,
					cx + (float) Math.cos(angle) * (spine - width), cy + (float) Math.sin(angle) * (spine - width) * 1.25f, 0.24f - 0.14f * t};
		});
		b.grind = 0.42f;
		m.blade(b);
		return m;
	}

	private static Shape ursusBlade() {
		Shape m = new Shape();
		Shape.Blade b = blade(32, straight(0.3f, 9.6f, -0.95f, 1.2f, 0.2f, 0.62f, true, 0.62f, 0.27f, 0.1f));
		b.grind = 0.5f;
		b.swedgeFrom = 0.62f;
		m.blade(b);
		m.pin(-0.55f, 1.6f, -0.45f, 0.45f, 0.18f, KnifeShading.HARDWARE);
		return m;
	}

	private static void paracord(Shape m) {
		Shape.Blade b = blade(36, straight(1.0f, 9.6f, -0.9f, 1.15f, 0.1f, 0.6f, false, 0.55f, 0.28f, 0.1f));
		b.grind = 0.48f;
		m.blade(b);
		m.plate(new float[] {-0.9f, 1.1f, 1.15f, 1.1f, 1.0f, 0.5f, 0.6f, 0.2f, 0.75f, -0.4f, -0.8f, -0.4f}, -0.3f, 0.3f, 0.1f, KnifeShading.BLADE);
		Shape.Ring[] wrap = new Shape.Ring[22];
		for (int i = 0; i < wrap.length; i++) {
			float f = i / (float) (wrap.length - 1);
			float y = -0.3f - 4.9f * f;
			float bump = i % 2 == 0 ? 1 : 0.88f;
			wrap[i] = Shape.Ring.along(0, y, 0, 0.82f * bump, 0.6f * bump);
		}
		m.loft(wrap, 14, 0.35f, KnifeShading.CORD, Shape.OPEN, Shape.OPEN, false);
		m.torus(0, -5.75f, 0, 0.55f, 0.2f, 0.24f, 16, 8, KnifeShading.BLADE);
	}

	private static void survival(Shape m) {
		Profile shape = straight(1.0f, 10.4f, -0.95f, 1.15f, 0.1f, 0.62f, false, 0.6f, 0.3f, 0.11f);
		Shape.Blade b = blade(48, teeth(shape, 0.1f, 0.42f, 7, 0.26f));
		b.grind = 0.5f;
		m.blade(b);
		m.plate(new float[] {-1.35f, 0.45f, 1.6f, 0.45f, 1.7f, 1.05f, -1.4f, 1.05f}, -0.4f, 0.4f, 0.16f, KnifeShading.HARDWARE);
		grip(m, 0.5f, -5.1f, 0.84f, 0.64f, 0.45f, 0.5f, 0.06f, KnifeShading.RUBBER, Shape.OPEN);
		m.loft(new Shape.Ring[] {Shape.Ring.along(0, -5.05f, 0, 0.92f, 0.7f), Shape.Ring.along(0, -5.75f, 0, 0.92f, 0.7f)}, 16, 0.2f,
				KnifeShading.HARDWARE, Shape.OPEN, Shape.CAP, false);
	}

	private static void skeleton(Shape m) {
		Shape.Blade b = blade(36, straight(0.6f, 9.0f, -0.85f, 1.05f, 0.1f, 0.6f, false, 0.55f, 0.24f, 0.1f));
		b.grind = 0.48f;
		m.blade(b);
		Shape.Ring[] frame = new Shape.Ring[29];
		for (int i = 0; i < frame.length; i++) {
			double a = 2 * Math.PI * i / (frame.length - 1);
			float x = (float) Math.sin(a) * 0.85f;
			float y = -2.2f + (float) Math.cos(a) * 2.6f;
			float tx = (float) Math.cos(a) * 0.85f;
			float ty = -(float) Math.sin(a) * 2.6f;
			frame[i] = new Shape.Ring(x, y, 0, tx, ty, 0, 0.26f, 0.24f, new float[] {(float) Math.sin(a), (float) Math.cos(a) * 0.3f, 0});
		}
		m.loft(frame, 10, 0.5f, KnifeShading.BLADE, Shape.OPEN, Shape.OPEN, true);
		m.plate(new float[] {-0.85f, 0.7f, 1.05f, 0.7f, 0.85f, 0.1f, -0.85f, 0.1f}, -0.24f, 0.24f, 0.08f, KnifeShading.BLADE);
		m.torus(0, -5.25f, 0, 0.75f, 0.24f, 0.24f, 18, 8, KnifeShading.BLADE);
		m.loft(new Shape.Ring[] {Shape.Ring.along(0, -0.6f, 0, 0.22f, 0.16f), Shape.Ring.along(0, -3.6f, 0, 0.22f, 0.16f)}, 8, 0.3f,
				KnifeShading.CORD, Shape.CAP, Shape.CAP, false);
	}

	private static void nomad(Shape m) {
		Shape.Blade b = blade(36, straight(1.0f, 9.6f, -0.9f, 1.15f, 0.25f, 0.58f, false, 0.5f, 0.27f, 0.1f));
		b.grind = 0.46f;
		m.blade(b);
		m.plate(new float[] {-1.05f, 1.05f, 1.2f, 1.05f, 1.25f, 0.4f, 0.6f, -0.2f, 0.95f, -1.6f, 0.85f, -4.6f, 0.4f, -5.3f, -0.6f, -5.3f, -0.95f,
				-4.3f, -1.05f, -1.0f}, -0.48f, 0.48f, 0.2f, KnifeShading.POLYMER);
		m.pin(0.05f, -0.9f, -0.52f, 0.52f, 0.2f, KnifeShading.HARDWARE);
		m.pin(0.05f, -3.9f, -0.52f, 0.52f, 0.2f, KnifeShading.HARDWARE);
	}

	private static void classic(Shape m) {
		Shape.Blade b = blade(40, straight(1.2f, 10.0f, -0.8f, 0.95f, -0.15f, 0.62f, true, 0.66f, 0.26f, 0.1f));
		b.grind = 0.52f;
		b.swedgeFrom = 0.64f;
		b.swedgeWidth = 0.34f;
		m.blade(b);
		m.plate(new float[] {-1.9f, 0.5f, 2.0f, 0.5f, 2.3f, 0.85f, 2.0f, 1.2f, -1.9f, 1.2f, -2.2f, 0.85f}, -0.38f, 0.38f, 0.16f, KnifeShading.GUARD);
		Shape.Ring[] wrap = new Shape.Ring[18];
		for (int i = 0; i < wrap.length; i++) {
			float f = i / (float) (wrap.length - 1);
			wrap[i] = Shape.Ring.along(0, 0.5f - 5.2f * f, 0, 0.74f * (i % 2 == 0 ? 1 : 0.9f), 0.58f * (i % 2 == 0 ? 1 : 0.9f));
		}
		m.loft(wrap, 14, 0.3f, KnifeShading.CORD, Shape.OPEN, Shape.OPEN, false);
		m.loft(new Shape.Ring[] {Shape.Ring.along(0, -4.65f, 0, 0.82f, 0.64f), Shape.Ring.along(0, -5.4f, 0, 0.7f, 0.55f)}, 16, 0.3f,
				KnifeShading.GUARD, Shape.OPEN, Shape.DOME, false);
	}

	/** A push dagger: a T handle across the palm, the blade standing out between the fingers. */
	private static void pushDagger(Shape m) {
		Shape.Blade b = blade(28, straight(0.2f, 5.0f, -0.75f, 0.75f, 0.0f, 0.0f, false, 0.0f, 0.24f, 0.08f));
		b.doubleEdged = true;
		b.grind = 0.42f;
		m.blade(b);
		m.loft(new Shape.Ring[] {Shape.Ring.facing(-2.4f, -0.45f, 0, 1, 0, 0, 0.55f, 0.48f), Shape.Ring.facing(-1.0f, -0.6f, 0, 1, 0, 0, 0.62f, 0.52f),
				Shape.Ring.facing(1.0f, -0.6f, 0, 1, 0, 0, 0.62f, 0.52f), Shape.Ring.facing(2.4f, -0.45f, 0, 1, 0, 0, 0.55f, 0.48f)}, 14, 0.35f,
				KnifeShading.RUBBER, Shape.DOME, Shape.DOME, false);
		m.plate(new float[] {-0.85f, -0.1f, 0.85f, -0.1f, 0.75f, 0.35f, -0.75f, 0.35f}, -0.3f, 0.3f, 0.1f, KnifeShading.HARDWARE);
	}

	private static void dagger(Shape m) {
		Shape.Blade b = blade(28, straight(0.95f, 8.6f, -0.85f, 0.85f, 0.0f, 0.0f, false, 0.0f, 0.24f, 0.08f));
		b.doubleEdged = true;
		b.grind = 0.4f;
		m.blade(b);
		m.loft(new Shape.Ring[] {Shape.Ring.facing(-2.6f, 0.65f, 0, 1, 0, 0, 0.18f, 0.18f), Shape.Ring.facing(-2.1f, 0.65f, 0, 1, 0, 0, 0.32f, 0.36f),
				Shape.Ring.facing(2.1f, 0.65f, 0, 1, 0, 0, 0.32f, 0.36f), Shape.Ring.facing(2.6f, 0.65f, 0, 1, 0, 0, 0.18f, 0.18f)}, 12, 0.2f,
				KnifeShading.GUARD, Shape.DOME, Shape.DOME, false);
		grip(m, 0.35f, -4.5f, 0.6f, 0.6f, 0, 0.4f, 0.12f, KnifeShading.CORD, Shape.OPEN);
		m.loft(new Shape.Ring[] {Shape.Ring.along(0, -4.45f, 0, 0.7f, 0.7f), Shape.Ring.along(0, -5.2f, 0, 0.85f, 0.85f)}, 14, 0, KnifeShading.GUARD,
				Shape.OPEN, Shape.DOME, false);
	}

	private static void phantomHilt(Shape m) {
		Shape.Ring[] hilt = new Shape.Ring[14];
		for (int i = 0; i < hilt.length; i++) {
			float f = i / (float) (hilt.length - 1);
			float y = 1.3f - 6.3f * f;
			float r = f < 0.12f ? 0.9f : f < 0.2f ? 0.72f : f > 0.92f ? 0.85f : (i % 2 == 0 ? 0.7f : 0.64f);
			hilt[i] = Shape.Ring.along(0, y, 0, r, r);
		}
		m.loft(hilt, 16, 0, KnifeShading.HARDWARE, Shape.CAP, Shape.DOME, false);
		m.loft(new Shape.Ring[] {Shape.Ring.along(0, 0.55f, 0, 0.77f, 0.77f), Shape.Ring.along(0, 0.85f, 0, 0.77f, 0.77f)}, 16, 0, KnifeShading.GUARD,
				Shape.OPEN, Shape.OPEN, false);
		m.loft(new Shape.Ring[] {Shape.Ring.along(0, -4.35f, 0, 0.89f, 0.89f), Shape.Ring.along(0, -4.6f, 0, 0.89f, 0.89f)}, 16, 0, KnifeShading.GUARD,
				Shape.OPEN, Shape.OPEN, false);
	}

	/** The energy blade, base at the origin, 9.4 pixels long at full length; scaled along its length as it ignites. */
	private static Shape phantomBlade() {
		Shape m = new Shape();
		Shape.Ring[] rings = new Shape.Ring[8];
		for (int i = 0; i < rings.length; i++) {
			float f = i / (float) (rings.length - 1);
			rings[i] = Shape.Ring.along(0, f * 9.0f, 0, 0.36f, 0.36f);
		}
		m.loft(rings, 14, 0, KnifeShading.ENERGY, Shape.CAP, Shape.DOME, false);
		return m;
	}

	private static void katana(Shape m) {
		Shape.Blade b = blade(48, t -> {
			float y = 1.2f + 12.6f * t;
			float bend = -0.9f * t * t;
			float tip = t > 0.9f ? (t - 0.9f) / 0.1f : 0;
			float sx = -0.5f + bend + 0.6f * tip * tip;
			float ex = 0.55f + bend - 0.9f * (float) Math.pow(tip, 0.7);
			return new float[] {sx, y, ex, y, 0.22f - 0.1f * t};
		});
		b.grind = 0.62f;
		m.blade(b);
		m.loft(new Shape.Ring[] {Shape.Ring.along(0, 0.15f, 0, 1.65f, 1.35f), Shape.Ring.along(0, 0.55f, 0, 1.65f, 1.35f)}, 18, 0.15f,
				KnifeShading.GUARD, Shape.CAP, Shape.CAP, false);
		m.plate(rect(-0.62f, 0.55f, 0.68f, 1.25f), -0.3f, 0.3f, 0.06f, KnifeShading.HARDWARE);
		grip(m, 0.15f, -6.3f, 0.62f, 0.46f, 0.35f, 0.5f, 0, KnifeShading.CORD, Shape.OPEN);
		m.loft(new Shape.Ring[] {Shape.Ring.along(0, -6.25f, 0, 0.66f, 0.5f), Shape.Ring.along(0, -6.75f, 0, 0.58f, 0.44f)}, 14, 0.35f,
				KnifeShading.HARDWARE, Shape.OPEN, Shape.CAP, false);
	}

	private static void scythe(Shape m) {
		Shape.Ring[] pole = new Shape.Ring[6];
		for (int i = 0; i < pole.length; i++) {
			float f = i / (float) (pole.length - 1);
			pole[i] = Shape.Ring.along(0.15f * (float) Math.sin(f * Math.PI), -6.5f + 10.2f * f, 0, 0.42f, 0.42f);
		}
		m.loft(pole, 12, 0, KnifeShading.WOOD, Shape.DOME, Shape.DOME, false);
		m.loft(new Shape.Ring[] {Shape.Ring.along(0, 2.4f, 0, 0.55f, 0.55f), Shape.Ring.along(0, 3.2f, 0, 0.55f, 0.55f)}, 12, 0, KnifeShading.HARDWARE,
				Shape.OPEN, Shape.OPEN, false);
		Shape.Blade b = blade(28, t -> {
			double angle = Math.toRadians(128 - 112 * t);
			float cx = 4.2f;
			float cy = -2.2f;
			float rs = 6.0f;
			float width = 1.55f * (1 - (float) Math.pow(t, 1.3)) + 0.02f;
			return new float[] {cx + (float) Math.cos(angle) * rs, cy + (float) Math.sin(angle) * rs, cx + (float) Math.cos(angle) * (rs - width),
					cy + (float) Math.sin(angle) * (rs - width), 0.22f - 0.14f * t};
		});
		b.grind = 0.45f;
		m.blade(b);
	}

	private void clips() {
		if (draw != null) return;
		float reach = this == KATANA || this == SCYTHE ? 1.35f : this == PHANTOM || this == BOWIE ? 1.2f : this == KARAMBIT || this == TALON ? 0.9f : 1f;
		slashes = new Clip[] {
				Clip.of("slash right to left", 450)
						.key(0)
						.key(0.16f, Clip.X, 0.14f, Clip.Y, 0.34f, Clip.SWEEP, -24, Clip.PITCH, 6, Clip.K_YAW, 10)
						.key(0.34f, Clip.X, -0.5f * reach, Clip.Y, 0.55f, Clip.Z, -0.04f, Clip.SWEEP, 58 * reach, Clip.YAW, 6, Clip.K_YAW, -30)
						.key(0.46f, Clip.X, -0.72f * reach, Clip.Y, 0.3f, Clip.Z, -0.04f, Clip.SWEEP, 80 * reach, Clip.YAW, 8, Clip.PITCH, -3, Clip.K_YAW, -36)
						.key(1),
				Clip.of("slash left to right", 450)
						.key(0)
						.key(0.16f, Clip.X, -0.6f, Clip.Y, 0.42f, Clip.SWEEP, 58, Clip.YAW, 6, Clip.PITCH, 5, Clip.K_YAW, -15)
						.key(0.34f, Clip.X, -0.02f, Clip.Y, 0.5f, Clip.Z, -0.04f, Clip.SWEEP, -45 * reach, Clip.YAW, -4, Clip.K_YAW, 28)
						.key(0.46f, Clip.X, 0.18f * reach, Clip.Y, 0.3f, Clip.Z, -0.04f, Clip.SWEEP, -62 * reach, Clip.YAW, -6, Clip.PITCH, -3, Clip.K_YAW, 32)
						.key(1)};
		use = Clip.of("heavy stab", 1000)
				.key(0)
				.key(0.3f, Clip.Y, 0.08f, Clip.Z, 0.1f, Clip.PITCH, 14, Clip.SWEEP, 12, Clip.K_PITCH, 10)
				.key(0.36f, Clip.Y, 0.09f, Clip.Z, 0.11f, Clip.PITCH, 16, Clip.SWEEP, 14, Clip.K_PITCH, 12)
				.key(0.47f, Clip.X, -0.12f, Clip.Y, 0.08f, Clip.Z, -0.3f * reach, Clip.PITCH, -6, Clip.SWEEP, 8, Clip.K_PITCH, -62)
				.key(0.6f, Clip.X, -0.11f, Clip.Y, 0.07f, Clip.Z, -0.27f * reach, Clip.PITCH, -5, Clip.SWEEP, 7, Clip.K_PITCH, -58)
				.key(1);
		draw = KnifeClips.draw(this);
		inspect = KnifeClips.inspect(this);
	}

	/** The id used in saved settings: the name in lower case. */
	public String id() {
		return name().toLowerCase(Locale.ROOT);
	}
}
