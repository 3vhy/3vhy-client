package dev.eventide.client.knife;

/**
 * Each knife's draw, inspect and trick, the way a shooter's knives move. Signature draws for the knives
 * known for them: the karambit spins up round its ring, the butterfly knife flips its handles open,
 * folders flick their blade out, bayonets flip end over end into the hand, and the flip knife and the
 * plain fixed blades are tossed and caught. Inspects open on a pose that turns the blade flat towards
 * you, then show the knife's own moves; the trick is the rarer inspect (a quarter of the time, or press
 * inspect again while one plays): a triple ring spin, the butterfly thrown shut and caught, a folder
 * snapped shut and open twice, a bayonet twirled round the thumb. Values are in the units {@link Clip}
 * describes.
 */
final class KnifeClips {
	/** The inspect's opening pose: the knife laid across the screen with its flat towards you, brought in and up. */
	private static final float[] PRESENT = {Clip.SWEEP, 68, Clip.X, -0.24f, Clip.Y, 0.18f, Clip.Z, 0.05f, Clip.PITCH, 6, Clip.YAW, 3};

	private KnifeClips() {
	}

	static Clip draw(KnifeModel model) {
		return switch (model) {
			case KARAMBIT, TALON -> Clip.of("draw", 950)
					.key(0, Clip.Y, -0.9f, Clip.ROLL, 50, Clip.K_ROLL, -540)
					.key(0.62f, Clip.Y, 0.02f, Clip.ROLL, -6, Clip.K_ROLL, -12)
					.key(1);
			case BUTTERFLY -> Clip.of("draw", 1150)
					.key(0, Clip.Y, -0.9f, Clip.OPEN, 0)
					.key(0.3f, Clip.Y, 0.0f, Clip.OPEN, 0)
					.key(0.52f, Clip.OPEN, 0.45f, Clip.HANDLE_B, 140, Clip.K_ROLL, -180)
					.key(0.78f, Clip.OPEN, 1, Clip.HANDLE_B, 0, Clip.K_ROLL, -360)
					.key(1, Clip.K_ROLL, -360);
			case FLIP -> Clip.of("draw", 1050)
					.key(0, Clip.Y, -0.9f, Clip.OPEN, 0, Clip.ROLL, 20)
					.key(0.28f, Clip.Y, -0.04f, Clip.OPEN, 0, Clip.ROLL, 8)
					.key(0.36f, Clip.Y, 0.02f, Clip.OPEN, 0, Clip.PITCH, 10)
					.key(0.52f, Clip.TOSS_Y, 3.5f, Clip.K_ROLL, -200, Clip.OPEN, 0.6f, Clip.PITCH, 4)
					.key(0.66f, Clip.Y, -0.025f, Clip.PITCH, -5, Clip.K_ROLL, -360, Clip.OPEN, 1.06f)
					.key(0.78f, Clip.K_ROLL, -360, Clip.OPEN, 0.98f)
					.key(1, Clip.K_ROLL, -360);
			case CLASSIC, PARACORD, SURVIVAL, NOMAD, SKELETON -> Clip.of("draw", 1100)
					.key(0, Clip.Y, -0.9f, Clip.ROLL, -20)
					.key(0.26f, Clip.Y, -0.05f, Clip.ROLL, -4)
					.key(0.34f, Clip.Y, 0.02f, Clip.PITCH, 9)
					.key(0.52f, Clip.TOSS_Y, 4, Clip.K_ROLL, 210, Clip.PITCH, 3)
					.key(0.68f, Clip.Y, -0.03f, Clip.PITCH, -5, Clip.K_ROLL, 360)
					.key(0.8f, Clip.Y, 0.01f, Clip.K_ROLL, 360)
					.key(1, Clip.K_ROLL, 360);
			case FALCHION, NAVAJA, STILETTO, URSUS -> Clip.of("draw", 820)
					.key(0, Clip.Y, -0.9f, Clip.OPEN, 0, Clip.ROLL, 20)
					.key(0.34f, Clip.Y, -0.02f, Clip.OPEN, 0, Clip.ROLL, 12, Clip.PITCH, 10)
					.key(0.5f, Clip.Y, 0.02f, Clip.OPEN, 1.08f, Clip.ROLL, -6, Clip.PITCH, -6)
					.key(0.66f, Clip.OPEN, 0.97f, Clip.ROLL, 2)
					.key(1);
			case M9_BAYONET, BAYONET -> Clip.of("draw", 900)
					.key(0, Clip.Y, -0.9f, Clip.X, 0.2f, Clip.K_ROLL, 360, Clip.ROLL, -20)
					.key(0.6f, Clip.Y, 0.03f, Clip.X, -0.01f, Clip.K_ROLL, 0, Clip.ROLL, 4)
					.key(1);
			case SHADOW_DAGGERS -> Clip.of("draw", 700)
					.key(0, Clip.Y, -0.9f, Clip.PITCH, -40)
					.key(0.6f, Clip.Y, 0.02f, Clip.PITCH, 6)
					.key(1);
			case DAGGER -> Clip.of("draw", 900)
					.key(0, Clip.Y, -0.9f)
					.key(0.28f, Clip.Y, -0.04f)
					.key(0.55f, Clip.Y, 0.02f, Clip.SLIDE, 6, Clip.K_PITCH, -200)
					.key(0.82f, Clip.SLIDE, 0, Clip.K_PITCH, -360)
					.key(1, Clip.K_PITCH, -360);
			case PHANTOM -> Clip.of("draw", 900)
					.key(0, Clip.Y, -0.9f, Clip.OPEN, 0)
					.key(0.38f, Clip.Y, 0.0f, Clip.OPEN, 0)
					.key(0.62f, Clip.OPEN, 1.08f)
					.key(1);
			case KATANA -> Clip.of("draw", 950)
					.key(0, Clip.X, -0.3f, Clip.Y, -0.8f, Clip.YAW, -40, Clip.ROLL, 30)
					.key(0.6f, Clip.X, 0.03f, Clip.YAW, 6, Clip.ROLL, -4)
					.key(1);
			case SCYTHE -> Clip.of("draw", 1000)
					.key(0, Clip.X, 0.35f, Clip.Y, -0.9f, Clip.K_ROLL, 360)
					.key(0.68f, Clip.Y, 0.03f, Clip.K_ROLL, 0, Clip.X, 0)
					.key(1);
			default -> Clip.of("draw", 820)
					.key(0, Clip.X, 0.35f, Clip.Y, -0.9f, Clip.K_YAW, 360, Clip.ROLL, -30)
					.key(0.68f, Clip.X, -0.01f, Clip.Y, 0.03f, Clip.K_YAW, 0, Clip.ROLL, 4)
					.key(1);
		};
	}

	static Clip inspect(KnifeModel model) {
		return switch (model) {
			case KARAMBIT, TALON -> Clip.of("inspect", 3300)
					.key(0)
					.key(0.14f, PRESENT)
					.then(0.3f, Clip.K_YAW, 160)
					.then(0.42f, Clip.K_YAW, 0)
					.then(0.66f, Clip.K_ROLL, -720, Clip.ROLL, -102)
					.then(0.8f, Clip.K_ROLL, -720, Clip.ROLL, -114, Clip.YAW, 28)
					.key(1, Clip.K_ROLL, -720);
			case BUTTERFLY -> Clip.of("inspect", 3700)
					.key(0)
					.key(0.1f, PRESENT)
					.then(0.25f, Clip.OPEN, 0.5f, Clip.HANDLE_A, -150, Clip.K_ROLL, 180)
					.then(0.38f, Clip.OPEN, 0, Clip.HANDLE_A, 0, Clip.K_ROLL, 360)
					.then(0.52f, Clip.OPEN, 0.5f, Clip.HANDLE_B, 150, Clip.K_ROLL, 540)
					.then(0.66f, Clip.OPEN, 1, Clip.HANDLE_B, 0, Clip.K_ROLL, 720)
					.then(0.82f, Clip.K_YAW, 180)
					.key(1, Clip.K_ROLL, 720, Clip.K_YAW, 360);
			case FLIP, FALCHION, NAVAJA, STILETTO, URSUS -> Clip.of("inspect", 3200)
					.key(0)
					.key(0.13f, PRESENT)
					.then(0.32f, Clip.K_YAW, 180)
					.then(0.46f, Clip.OPEN, 0.15f)
					.then(0.58f, Clip.OPEN, 1.06f)
					.then(0.64f, Clip.OPEN, 1)
					.then(0.84f, Clip.K_YAW, 360)
					.key(1, Clip.K_YAW, 360);
			case M9_BAYONET, BAYONET -> Clip.of("inspect", 3200)
					.key(0)
					.key(0.14f, PRESENT)
					.then(0.34f, Clip.K_YAW, 180)
					.then(0.5f, Clip.SLIDE, 4, Clip.K_PITCH, -180)
					.then(0.62f, Clip.SLIDE, 0, Clip.K_PITCH, -360)
					.then(0.85f, Clip.K_YAW, 360)
					.key(1, Clip.K_YAW, 360, Clip.K_PITCH, -360);
			case DAGGER, SHADOW_DAGGERS -> Clip.of("inspect", 3000)
					.key(0)
					.key(0.12f, PRESENT)
					.then(0.32f, Clip.K_ROLL, 360, Clip.SLIDE, 2)
					.then(0.5f, Clip.K_ROLL, 720, Clip.SLIDE, 0)
					.then(0.7f, Clip.K_YAW, 180)
					.key(1, Clip.K_ROLL, 720, Clip.K_YAW, 360);
			case PHANTOM -> Clip.of("inspect", 3200)
					.key(0)
					.key(0.12f, PRESENT)
					.then(0.4f, Clip.K_ROLL, -360, Clip.OPEN, 0.85f)
					.then(0.62f, Clip.K_ROLL, -720, Clip.OPEN, 1.08f)
					.then(0.8f, Clip.K_YAW, 180, Clip.OPEN, 1)
					.key(1, Clip.K_ROLL, -720, Clip.K_YAW, 360);
			case KATANA -> Clip.of("inspect", 3700)
					.key(0)
					.key(0.16f, PRESENT)
					.then(0.42f, Clip.X, -0.17f)
					.then(0.58f, Clip.K_YAW, 180)
					.then(0.74f, Clip.X, -0.03f)
					.key(1, Clip.K_YAW, 360);
			case SCYTHE -> Clip.of("inspect", 3000)
					.key(0)
					.key(0.14f, PRESENT)
					.then(0.45f, Clip.K_ROLL, 360)
					.then(0.72f, Clip.K_ROLL, 720, Clip.YAW, 28)
					.key(1, Clip.K_ROLL, 720);
			default -> Clip.of("inspect", 3000)
					.key(0)
					.key(0.15f, PRESENT)
					.then(0.35f, Clip.ROLL, -35)
					.then(0.55f, Clip.K_YAW, 180)
					.then(0.72f, Clip.ROLL, -110, Clip.K_YAW, 360)
					.key(1, Clip.K_YAW, 360);
		};
	}

	static Clip trick(KnifeModel model) {
		return switch (model) {
			case KARAMBIT, TALON -> Clip.of("trick", 3000)
					.key(0)
					.key(0.1f, PRESENT)
					.then(0.2f, Clip.K_ROLL, -360)
					.then(0.3f, Clip.K_ROLL, -720)
					.then(0.4f, Clip.K_ROLL, -1080)
					.then(0.5f, Clip.K_ROLL, -1080, Clip.YAW, 10)
					.then(0.62f, Clip.TOSS_Y, 2.5f, Clip.K_PITCH, 180)
					.then(0.74f, Clip.TOSS_Y, 0, Clip.K_PITCH, 360)
					.then(0.86f, Clip.YAW, 0)
					.key(1, Clip.K_ROLL, -1080, Clip.K_PITCH, 360);
			case BUTTERFLY -> Clip.of("trick", 3600)
					.key(0)
					.key(0.1f, PRESENT)
					.then(0.2f, Clip.OPEN, 0.5f, Clip.HANDLE_A, -150, Clip.K_ROLL, 180)
					.then(0.3f, Clip.OPEN, 0, Clip.HANDLE_A, 0, Clip.K_ROLL, 360)
					.then(0.42f, Clip.TOSS_Y, 3, Clip.TOSS_X, 1.2f, Clip.K_ROLL, 720, Clip.K_YAW, 180)
					.then(0.54f, Clip.TOSS_Y, 0, Clip.TOSS_X, 0, Clip.K_ROLL, 1080, Clip.K_YAW, 360)
					.then(0.64f, Clip.OPEN, 0.5f, Clip.HANDLE_B, 150, Clip.K_ROLL, 1260)
					.then(0.74f, Clip.OPEN, 1, Clip.HANDLE_B, 0, Clip.K_ROLL, 1440)
					.then(0.88f, Clip.K_YAW, 540)
					.key(1, Clip.K_ROLL, 1440, Clip.K_YAW, 720);
			case FLIP, FALCHION, NAVAJA, STILETTO, URSUS -> Clip.of("trick", 3200)
					.key(0)
					.key(0.12f, PRESENT)
					.then(0.22f, Clip.OPEN, 0.08f)
					.then(0.3f, Clip.OPEN, 1.06f)
					.then(0.34f, Clip.OPEN, 1)
					.then(0.42f, Clip.OPEN, 0.08f)
					.then(0.5f, Clip.OPEN, 1.06f)
					.then(0.54f, Clip.OPEN, 1)
					.then(0.66f, Clip.TOSS_Y, 3, Clip.TOSS_X, 1, Clip.K_ROLL, 200)
					.then(0.78f, Clip.TOSS_Y, 0, Clip.TOSS_X, 0, Clip.K_ROLL, 360)
					.key(1, Clip.K_ROLL, 360);
			case M9_BAYONET, BAYONET -> Clip.of("trick", 3200)
					.key(0)
					.key(0.12f, PRESENT)
					.then(0.24f, Clip.K_YAW, 360)
					.then(0.36f, Clip.K_YAW, 720)
					.then(0.5f, Clip.TOSS_Y, 3, Clip.TOSS_X, 1, Clip.K_ROLL, 180)
					.then(0.64f, Clip.TOSS_Y, 0, Clip.TOSS_X, 0, Clip.K_ROLL, 360)
					.then(0.78f, Clip.SLIDE, 3, Clip.K_PITCH, -180)
					.then(0.88f, Clip.SLIDE, 0, Clip.K_PITCH, -360)
					.key(1, Clip.K_YAW, 720, Clip.K_ROLL, 360, Clip.K_PITCH, -360);
			case DAGGER, SHADOW_DAGGERS -> Clip.of("trick", 2800)
					.key(0)
					.key(0.12f, PRESENT)
					.then(0.3f, Clip.K_ROLL, 540)
					.then(0.48f, Clip.K_ROLL, 1080)
					.then(0.64f, Clip.K_YAW, 180)
					.then(0.82f, Clip.K_YAW, 360)
					.key(1, Clip.K_ROLL, 1080, Clip.K_YAW, 360);
			case PHANTOM -> Clip.of("trick", 3200)
					.key(0)
					.key(0.12f, PRESENT)
					.then(0.24f, Clip.OPEN, 0.04f)
					.then(0.4f, Clip.K_ROLL, -360)
					.then(0.5f, Clip.OPEN, 1.1f)
					.then(0.56f, Clip.OPEN, 1)
					.then(0.72f, Clip.TOSS_Y, 2.5f, Clip.TOSS_X, 1, Clip.K_ROLL, -540)
					.then(0.86f, Clip.TOSS_Y, 0, Clip.TOSS_X, 0, Clip.K_ROLL, -720)
					.key(1, Clip.K_ROLL, -720);
			case KATANA -> Clip.of("trick", 3600)
					.key(0)
					.key(0.16f, PRESENT)
					.then(0.36f, Clip.K_ROLL, 360, Clip.Y, 0.24f)
					.then(0.56f, Clip.K_ROLL, 720, Clip.Y, 0.18f)
					.then(0.76f, Clip.K_YAW, 180)
					.key(1, Clip.K_ROLL, 720, Clip.K_YAW, 360);
			case SCYTHE -> Clip.of("trick", 3000)
					.key(0)
					.key(0.14f, PRESENT)
					.then(0.34f, Clip.K_ROLL, 360)
					.then(0.5f, Clip.TOSS_Y, 2, Clip.TOSS_X, 1, Clip.K_ROLL, 720)
					.then(0.64f, Clip.TOSS_Y, 0, Clip.TOSS_X, 0, Clip.K_ROLL, 1080)
					.key(1, Clip.K_ROLL, 1080);
			default -> Clip.of("trick", 3000)
					.key(0)
					.key(0.13f, PRESENT)
					.then(0.26f, Clip.Y, 0.22f, Clip.PITCH, 10)
					.then(0.4f, Clip.TOSS_Y, 3.5f, Clip.TOSS_X, 1.2f, Clip.K_ROLL, 200, Clip.K_YAW, 90)
					.then(0.54f, Clip.TOSS_Y, 0, Clip.TOSS_X, 0, Clip.K_ROLL, 360, Clip.K_YAW, 180, Clip.Y, 0.15f)
					.then(0.62f, Clip.Y, 0.18f, Clip.PITCH, 6)
					.then(0.8f, Clip.K_YAW, 360)
					.key(1, Clip.K_ROLL, 360, Clip.K_YAW, 360);
		};
	}

	/**
	 * The heavy stab: drawn back, then driven forward. Curved knives hook instead, the hand rolling over so
	 * the claw rips up and across, as a karambit's heavy attack does.
	 */
	static Clip heavy(KnifeModel model, float reach) {
		if (model.family == KnifeModel.Family.CURVED) {
			return Clip.of("heavy stab", 1000)
					.key(0)
					.key(0.28f, Clip.X, 0.06f, Clip.Y, -0.05f, Clip.Z, 0.12f, Clip.PITCH, 18, Clip.ROLL, 20, Clip.SWEEP, 16, Clip.K_ROLL, 10)
					.key(0.36f, Clip.X, 0.07f, Clip.Y, -0.06f, Clip.Z, 0.13f, Clip.PITCH, 20, Clip.ROLL, 24, Clip.SWEEP, 18, Clip.K_ROLL, 12)
					.key(0.48f, Clip.X, -0.18f, Clip.Y, 0.16f, Clip.Z, -0.26f * reach, Clip.PITCH, -24, Clip.ROLL, -18, Clip.SWEEP, -6, Clip.K_ROLL, -22)
					.key(0.6f, Clip.X, -0.16f, Clip.Y, 0.14f, Clip.Z, -0.24f * reach, Clip.PITCH, -20, Clip.ROLL, -16, Clip.SWEEP, -5, Clip.K_ROLL, -20)
					.key(1);
		}
		return Clip.of("heavy stab", 1000)
				.key(0)
				.key(0.3f, Clip.Y, 0.08f, Clip.Z, 0.1f, Clip.PITCH, 14, Clip.SWEEP, 12, Clip.K_PITCH, 10)
				.key(0.36f, Clip.Y, 0.09f, Clip.Z, 0.11f, Clip.PITCH, 16, Clip.SWEEP, 14, Clip.K_PITCH, 12)
				.key(0.47f, Clip.X, -0.12f, Clip.Y, 0.08f, Clip.Z, -0.3f * reach, Clip.PITCH, -6, Clip.SWEEP, 8, Clip.K_PITCH, -62)
				.key(0.6f, Clip.X, -0.11f, Clip.Y, 0.07f, Clip.Z, -0.27f * reach, Clip.PITCH, -5, Clip.SWEEP, 7, Clip.K_PITCH, -58)
				.key(1);
	}
}
