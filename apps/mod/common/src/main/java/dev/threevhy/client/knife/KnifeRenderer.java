package dev.threevhy.client.knife;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.threevhy.client.Hooks;
import dev.threevhy.client.compat.Compat;
import dev.threevhy.client.module.Knives;
import dev.threevhy.client.module.Modules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Draws the knife in your hand, in place of the held item, framed like a classic shooter's viewmodel: a
 * gloved fist low on the right (left for left-handed players, a mirror image) with the blade standing up and
 * in towards the middle of the screen, moved by {@link KnifeAnimator} and the Knives viewmodel settings. The
 * hand is drawn after the world with the depth cleared, so it never sinks into walls; the knife is also kept
 * clear of the camera's near plane, so big flips never cut it off.
 */
public final class KnifeRenderer {
	/** Where the middle of the grip sits, in blocks from the eye: right, down, forward. */
	private static final float BASE_X = 0.345f;
	private static final float BASE_Y = -0.401f;
	private static final float BASE_Z = -0.960f;
	/** How big the hand and knife are: one of their pixels, in blocks. */
	private static final float UNIT = 0.0912f;
	/** The knife's frame (handle +Y, edge +X, near flat +Z) as the camera sees it: blade up and leaning in, flat towards you. */
	private static final Quaternionf HOLD = new Quaternionf().setFromNormalized(new Matrix3f(0.9151f, 0.4021f, -0.03f, -0.4008f, 0.9152f, 0.0419f,
			0.0443f, -0.0263f, 0.9987f));
	/** How far the elbow is up the forearm from the wrist, in the hand's pixels: slashes swing from it. */
	private static final float ELBOW = 9;
	/** The viewmodel field of view the framing above is made for. */
	private static final float CLASSIC_FOV = 68;
	/** How far the animations' moves (in blocks) carry at this distance. */
	private static final float MOVE = 0.6f;
	private static final float NEAR = 0.16f;
	/** Points around a knife, in pixels, that must stay in front of the near plane. */
	private static final float[][] REACH = {{0, 10.5f}, {0, -7.5f}, {5.5f, 3}, {-2.5f, 3}};

	private static final PoseStack PROBE = new PoseStack();
	private static final Vector3f POINT = new Vector3f();

	private static final KnifeAnimator ANIMATOR = new KnifeAnimator();
	private static final float[] POSE = Clip.rest();
	/** A pose (or a clip at a point in it) to show instead of the animation, for the in-game test's pictures. */
	private static float[] preview = null;
	private static int lastSlot = -1;
	private static KnifeModel lastModel = null;
	private static boolean shown = false;
	/** When the glint after the last swing started running up the blade, or 0. */
	private static long glintStarted = 0;
	/** When the last swing's camera kick peaks, and how far it turns the view (degrees). */
	private static long kickAt = 0;
	private static float kickYaw = 0;
	private static float kickPitch = 0;
	private static boolean mirrored = false;

	private KnifeRenderer() {
	}

	private static float speed(dev.threevhy.client.module.Setting.Slider slider) {
		return (float) (slider.get() / 100.0);
	}

	public static void inspect(KnifeModel model) {
		ANIMATOR.play(model.inspect(), speed(Modules.KNIVES_INSPECT_SPEED), System.nanoTime());
		Hooks.knifeInspectRan = true;
	}

	public static void use(KnifeModel model) {
		float speed = speed(Modules.KNIVES_SWING_SPEED);
		ANIMATOR.play(model.use(), speed, System.nanoTime());
		glintStarted = System.nanoTime() + (long) (420_000_000L / speed);
		kick(470 / speed, 0.4f, 1.1f);
		Hooks.knifeSlashRan = true;
	}

	public static void slash(KnifeModel model) {
		var gameMode = Minecraft.getInstance().gameMode;
		boolean mining = gameMode != null && gameMode.isDestroying();
		float speed = speed(Modules.KNIVES_SWING_SPEED);
		int which = ANIMATOR.slash(model, speed, System.nanoTime(), mining);
		if (which < 0) return;
		glintStarted = System.nanoTime() + (long) (110_000_000L / speed);
		kick(150 / speed, which == 0 ? -0.6f : 0.6f, 0.25f);
		Hooks.knifeSlashRan = true;
	}

	/** A small turn of the view peaking {@code millis} from now, the way a hit punches a shooter's camera. */
	private static void kick(float millis, float yaw, float pitch) {
		kickAt = System.nanoTime() + (long) (millis * 1_000_000L);
		kickYaw = mirrored ? -yaw : yaw;
		kickPitch = pitch;
	}

	/** How far the camera kick turns the view right now (degrees, positive turns right). */
	public static float kickYaw() {
		return kickYaw * kickShape();
	}

	/** How far the camera kick tips the view right now (degrees, positive looks down). */
	public static float kickPitch() {
		return kickPitch * kickShape();
	}

	private static float kickShape() {
		if (kickAt == 0 || !shown || !Modules.KNIVES_KICK.get()) return 0;
		float t = (System.nanoTime() - kickAt) / 1e9f + 0.06f;
		if (t <= 0) return 0;
		if (t > 0.6f) {
			kickAt = 0;
			return 0;
		}
		float u = t / 0.06f;
		return u * (float) Math.exp(1 - u);
	}

	/**
	 * For the in-game test: holds the knife still at one point of an animation ("draw", "inspect", "use",
	 * "slash0" or "slash1", {@code t} 0..1 through it), or at rest with one pose channel set ("channel3" with
	 * {@code t} as the value). Null lets the animation play again.
	 */
	public static void preview(KnifeModel model, String which, float t) {
		if (which == null) {
			preview = null;
			return;
		}
		float[] pose = Clip.rest();
		if (which.startsWith("channel")) {
			pose[Integer.parseInt(which.substring(7))] = t;
		} else {
			Clip clip = switch (which) {
				case "draw" -> model.draw();
				case "inspect" -> model.inspect();
				case "use" -> model.use();
				default -> model.slash(which.startsWith("slash") ? Integer.parseInt(which.substring(5)) : 0);
			};
			clip.sample(t, pose);
		}
		preview = pose;
	}

	/**
	 * For the in-game test: how far the near-plane check moves this knife when it's at rest. Nothing at
	 * rest is near the camera, so anything but 0 means the check is measuring in the wrong space.
	 */
	public static float restPush(KnifeModel model) {
		float[] saved = POSE.clone();
		System.arraycopy(Clip.rest(), 0, POSE, 0, Clip.CHANNELS);
		float push = Math.abs(nearPush(model));
		System.arraycopy(saved, 0, POSE, 0, Clip.CHANNELS);
		return push;
	}

	/**
	 * For the in-game test: plays a draw, quick slashes, an inspect cut short by a slash and a stab on a
	 * made-up clock at 240 frames a second, and returns the biggest change in turning speed between two
	 * frames (degrees a second) over every turning channel, taking each turn the short way round (a whole
	 * turn looks the same as none). Smooth motion keeps it small; a knife that stops dead or starts at full
	 * speed shows up as a spike.
	 */
	public static float motionCheck(KnifeModel model) {
		KnifeAnimator animator = new KnifeAnimator();
		int[] angles = {Clip.PITCH, Clip.YAW, Clip.ROLL, Clip.K_PITCH, Clip.K_YAW, Clip.K_ROLL, Clip.HANDLE_A, Clip.HANDLE_B, Clip.SWEEP};
		long frame = 1_000_000_000L / 240;
		long start = 1_000_000_000L;
		float[] previous = Clip.rest();
		float[] current = Clip.rest();
		float[] lastSpeed = new float[Clip.CHANNELS];
		float worst = 0;
		for (int i = 0; i <= 240 * 4; i++) {
			long now = start + i * frame;
			int ms = Math.round(i * 1000f / 240);
			if (ms == 0) animator.play(model.draw(), 1, now);
			if (ms == 1000 || ms == 1150 || ms == 1300 || ms == 2300) animator.slash(model, 1, now, false);
			if (ms == 1600) animator.play(model.inspect(), 1, now);
			if (ms == 2700) animator.play(model.use(), 1, now);
			animator.pose(now, 0, 0, 0, 0, current);
			if (i > 0) {
				for (int c : angles) {
					float step = (current[c] - previous[c]) % 360f;
					if (step > 180) step -= 360;
					if (step < -180) step += 360;
					float speed = step * 240;
					if (i > 1) worst = Math.max(worst, Math.abs(speed - lastSpeed[c]));
					lastSpeed[c] = speed;
				}
			}
			System.arraycopy(current, 0, previous, 0, Clip.CHANNELS);
		}
		return worst;
	}

	/** Whether an inspect is playing, for the in-game test. */
	public static boolean inspecting() {
		KnifeModel model = lastModel;
		return model != null && ANIMATOR.playing(model.inspect());
	}

	/**
	 * Draws your main hand as a knife if it holds one; returns false to let Minecraft draw it as usual.
	 * Called where Minecraft draws the arm and item in first person.
	 */
	public static boolean render(AbstractClientPlayer player, HumanoidArm arm, ItemStack stack, PoseStack poseStack, SubmitNodeCollector collector,
			int light) {
		Minecraft mc = Minecraft.getInstance();
		int slot = player.getInventory().getSelectedSlot();
		KnifeModel model = Knives.animated() ? Knives.modelFor(player, stack, slot) : null;
		if (model == null) {
			shown = false;
			return false;
		}
		long now = System.nanoTime();
		if (!shown || slot != lastSlot || model != lastModel) {
			ANIMATOR.play(model.draw(), speed(Modules.KNIVES_DRAW_SPEED), now);
			Hooks.knifeDrawRan = true;
		}
		shown = true;
		lastSlot = slot;
		lastModel = model;
		if (player.isScoping()) return true;
		var camera = mc.gameRenderer.getMainCamera();
		var motion = player.getDeltaMovement();
		double yaw = Math.toRadians(player.getYRot());
		double forward = -Math.sin(yaw) * motion.x + Math.cos(yaw) * motion.z;
		double strafe = -Math.cos(yaw) * motion.x - Math.sin(yaw) * motion.z;
		ANIMATOR.moving(forward, strafe, motion.y, player.onGround(), player.isSprinting());
		ANIMATOR.pose(now, camera.yRot(), camera.xRot(), speed(Modules.KNIVES_IDLE), preview != null ? 0 : speed(Modules.KNIVES_BOB), POSE);
		if (preview != null) System.arraycopy(preview, 0, POSE, 0, Clip.CHANNELS);
		int preset = Modules.KNIVES_PRESET.index();
		boolean right = preset == 2 ? false : arm == HumanoidArm.RIGHT;
		mirrored = !right;
		float push = nearPush(model);

		poseStack.pushPose();
		float fov = fovScale();
		poseStack.scale(right ? fov : -fov, fov, 1);
		viewmodel(poseStack, push);
		if (Modules.KNIVES_ARM.get() && !player.isInvisible()) drawHand(model, poseStack, collector, light);
		knife(poseStack, model);
		drawParts(model, poseStack, collector, light);
		poseStack.popPose();
		Hooks.knifeRendered = true;
		return true;
	}

	/**
	 * How far to move the knife away so no part of it comes nearer than the near plane, worked out in the
	 * camera's own space (the hand's pose stack arrives already turned to the world, so its depth can't be
	 * read off it).
	 */
	private static float nearPush(KnifeModel model) {
		PROBE.last().pose().identity();
		PROBE.last().normal().identity();
		PROBE.pushPose();
		viewmodel(PROBE, 0);
		knife(PROBE, model);
		float nearest = Float.NEGATIVE_INFINITY;
		for (float[] p : REACH) {
			nearest = Math.max(nearest, PROBE.last().pose().transformPosition(POINT.set(p[0], p[1], 0)).z);
		}
		PROBE.popPose();
		return nearest > -NEAR ? -NEAR - nearest : 0;
	}

	/**
	 * How much bigger (or smaller) the viewmodel field of view setting makes everything, against the
	 * classic 68 the framing is made for. Like a shooter's setting, it's measured across a 4:3 screen.
	 */
	private static float fovScale() {
		double base = Math.atan(Math.tan(Math.toRadians(CLASSIC_FOV / 2)) * 0.75);
		double now = Math.atan(Math.tan(Math.toRadians(Modules.KNIVES_FOV.get() / 2)) * 0.75);
		return (float) (Math.tan(base) / Math.tan(now));
	}

	/**
	 * Moves {@code pose} into the hand's frame: the base spot, the viewmodel settings and the animation.
	 * Turns and tilts swing the arm about the elbow, as a real slash does; roll twists the wrist about the
	 * forearm, which is how a knife is turned over to look at it.
	 */
	private static void viewmodel(PoseStack pose, float push) {
		boolean center = Modules.KNIVES_PRESET.index() == 1;
		float x = Modules.KNIVES_X.getFloat() / 100f * 0.3f - (center ? 0.17f : 0);
		float y = Modules.KNIVES_Y.getFloat() / 100f * 0.3f - (center ? 0.02f : 0);
		float z = Modules.KNIVES_Z.getFloat() / 100f * 0.3f + (center ? 0.05f : 0);
		float unit = UNIT * (float) (Modules.KNIVES_SCALE.get() / 100.0);
		pose.translate(BASE_X + x + POSE[Clip.X] * MOVE, BASE_Y + y + POSE[Clip.Y] * MOVE, BASE_Z + z + POSE[Clip.Z] * MOVE + push);
		pose.mulPose(Axis.ZP.rotationDegrees(POSE[Clip.SWEEP]));
		Vector3f fore = HOLD.transform(new Vector3f(KnifeHand.FOREARM[0], KnifeHand.FOREARM[1], KnifeHand.FOREARM[2]));
		Vector3f wrist = HOLD.transform(new Vector3f(KnifeHand.WRIST[0], KnifeHand.WRIST[1], KnifeHand.WRIST[2])).mul(unit);
		Vector3f elbow = new Vector3f(fore).mul(ELBOW * unit).add(wrist);
		pose.translate(elbow.x, elbow.y, elbow.z);
		pose.mulPose(Axis.YP.rotationDegrees(Modules.KNIVES_YAW.getFloat() + POSE[Clip.YAW]));
		pose.mulPose(Axis.XP.rotationDegrees(Modules.KNIVES_PITCH.getFloat() + POSE[Clip.PITCH]));
		pose.translate(wrist.x - elbow.x, wrist.y - elbow.y, wrist.z - elbow.z);
		float roll = (float) Math.toRadians(-(Modules.KNIVES_ROLL.getFloat() + POSE[Clip.ROLL]));
		pose.mulPose(new Quaternionf().rotationAxis(roll, fore.x, fore.y, fore.z));
		pose.translate(-wrist.x, -wrist.y, -wrist.z);
		pose.mulPose(HOLD);
		pose.scale(unit, unit, unit);
	}

	/**
	 * From the hand to the knife's own frame: how it sits in the fist (a karambit or talon upside down with
	 * its ring on the index finger, a push dagger across the palm), its size, then its turn in the fingers.
	 */
	private static void knife(PoseStack pose, KnifeModel model) {
		if (model.family == KnifeModel.Family.CURVED) pose.mulPose(Axis.ZP.rotationDegrees(180));
		if (model.family == KnifeModel.Family.PUSH) pose.mulPose(Axis.ZP.rotationDegrees(-90));
		pose.scale(model.scale, model.scale, model.scale);
		pose.translate(model.spinX, model.spinY - model.grip, 0);
		pose.mulPose(Axis.YP.rotationDegrees(POSE[Clip.K_YAW]));
		pose.mulPose(Axis.XP.rotationDegrees(POSE[Clip.K_PITCH]));
		pose.mulPose(Axis.ZP.rotationDegrees(POSE[Clip.K_ROLL]));
		pose.translate(-model.spinX, -model.spinY + POSE[Clip.SLIDE], 0);
	}

	/** The gloved hand round the knife's grip, in the glove and colourway picked in the settings. */
	private static void drawHand(KnifeModel model, PoseStack pose, SubmitNodeCollector collector, int light) {
		int design = Math.max(0, java.util.Arrays.asList(Gloves.DESIGNS).indexOf(Modules.KNIVES_GLOVE.get()));
		int variant = (int) Modules.KNIVES_GLOVE_COLOR.get() - 1;
		Shape hand = KnifeHand.get(model.handleA * model.scale, model.handleB * model.scale, design);
		boolean custom = KnifeShading.custom();
		int frame = KnifeShading.frame(model.ordinal(), 0, 0, (float) (Modules.KNIVES_LIGHT.get() / 100.0));
		if (custom) {
			collector.submitCustomGeometry(pose, Compat.knifeShaded(), (last, out) -> hand.emit(last, out, light, design, variant, frame));
		} else {
			collector.submitCustomGeometry(pose, Compat.knifeRenderType(KnifeAtlas.get(), false),
					(last, out) -> hand.emitPlain(last, out, light, OverlayTexture.NO_OVERLAY, 0, design, variant));
		}
	}

	/**
	 * The knife's parts, each moved for the pose: a butterfly knife's handles about their pins, a folding
	 * blade about its pivot, an energy blade's length. All of them go through 3vhy's knife shader (or plain
	 * entity shading under a shader pack) in one batch.
	 */
	private static void drawParts(KnifeModel model, PoseStack pose, SubmitNodeCollector collector, int light) {
		Finish finish = finish();
		int seed = (int) Modules.KNIVES_SEED.get();
		boolean custom = KnifeShading.custom();
		var type = custom ? Compat.knifeShaded() : Compat.knifeRenderType(KnifeAtlas.get(), false);
		int frame = KnifeShading.frame(model.ordinal(), glint(), (float) (Modules.KNIVES_WEAR.get() / 100.0), (float) (Modules.KNIVES_LIGHT.get() / 100.0));
		float open = Math.max(0, POSE[Clip.OPEN]);
		for (KnifeModel.Part part : model.parts()) {
			pose.pushPose();
			switch (part.kind()) {
				case HANDLE_A, HANDLE_B -> {
					boolean a = part.kind() == KnifeModel.PartKind.HANDLE_A;
					float closed = a ? 180 : -180;
					float angle = closed * (1 - Math.min(1, open)) + (a ? POSE[Clip.HANDLE_A] : POSE[Clip.HANDLE_B]);
					pose.translate(part.pivotX(), part.pivotY(), 0);
					pose.mulPose(Axis.ZP.rotationDegrees(angle));
				}
				case FOLD -> {
					pose.translate(part.pivotX(), part.pivotY(), 0);
					pose.mulPose(Axis.ZP.rotationDegrees(-180 * (1 - open)));
					pose.translate(-part.pivotX(), -part.pivotY(), 0);
				}
				case ENERGY -> {
					if (open < 0.01f) {
						pose.popPose();
						continue;
					}
					pose.translate(part.pivotX(), part.pivotY(), 0);
					pose.scale(1, open, 1);
				}
				default -> {
				}
			}
			Shape mesh = part.mesh();
			int finishId = finish.id;
			if (custom) {
				collector.submitCustomGeometry(pose, type, (last, out) -> mesh.emit(last, out, light, finishId, seed, frame));
			} else {
				collector.submitCustomGeometry(pose, type, (last, out) -> mesh.emitPlain(last, out, light, OverlayTexture.NO_OVERLAY, finishId, 0, 0));
			}
			pose.popPose();
		}
	}

	/** The finish picked in the settings. */
	static Finish finish() {
		Finish finish = Finish.byLabel(Modules.KNIVES_FINISH.get());
		return finish == null ? Finish.FADE : finish;
	}

	/** Where the glint after a swing is along the blade (0..1), or 0 when there isn't one. */
	private static float glint() {
		if (glintStarted == 0) return 0;
		float t = (System.nanoTime() - glintStarted) / 220_000_000f;
		if (t < 0) return 0;
		if (t >= 1) {
			glintStarted = 0;
			return 0;
		}
		return Math.max(0.01f, t);
	}
}
