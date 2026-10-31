package dev.eventide.client.knife;

/**
 * Each knife's draw and inspect. Signature ones for the knives known for them: the karambit spins up
 * round its ring, the butterfly knife flips its handles open, folders flick their blade out, bayonets
 * flip end over end into the hand. Inspects open on a pose that turns the blade flat towards you, then
 * show the knife's own tricks. Values are in the units {@link Clip} describes.
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
			case FLIP, FALCHION, NAVAJA, STILETTO, URSUS -> Clip.of("draw", 820)
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
}
