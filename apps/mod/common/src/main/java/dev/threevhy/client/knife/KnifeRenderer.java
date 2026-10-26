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
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Draws the knife in your hand, in place of the held item: your arm reaching in from the bottom right (left
 * for left-handed players) with the knife gripped in the fist, its handle across the palm and the blade
 * standing up towards the middle of the screen so it reads at a glance, moved by {@link KnifeAnimator} and
 * the Knives viewmodel settings. The hand is drawn after the world with the depth cleared, so it never sinks
 * into walls; the knife is also kept clear of the camera's near plane, so big flips never cut it off.
 */
public final class KnifeRenderer {
	/** Where the fist is, in blocks from the eye: right, down, forward. */
	private static final float BASE_X = 0.32f;
	private static final float BASE_Y = -0.27f;
	private static final float BASE_Z = -0.84f;
	/** How big a knife is: its pixels in blocks. */
	private static final float KNIFE_SCALE = 1.07f / 16f;
	/** Which way the forearm runs from the fist: to the right and down, a little towards you. */
	private static final Vector3f FOREARM = new Vector3f(0.86f, -0.36f, 0.11f).normalize();
	/** How far the arm is turned about its length, which decides which way the fist (and the blade) faces. */
	private static final float ARM_TWIST = -131;
	private static final float ARM_SCALE = 0.84f;
	/** How far down the forearm the elbow is, where slashes swing from. */
	private static final float ELBOW = 0.42f;
	/** The middle of the fist on the arm model, two pixels in from the end. */
	private static final Vector3f HAND = new Vector3f(-0.4246f, 0.616f, 0);
	private static final Vector3f ARM_AXIS = new Vector3f(-0.0998f, 0.995f, 0).normalize();
	/** The arm model's own lean (Minecraft draws a first-person arm turned 0.1 radians about its depth). */
	private static final float ARM_LEAN = 0.1f;
	/**
	 * How the knife sits in the fist: the blade leaves the front of the fist, tipped this many degrees
	 * towards the fingers and turned this many about the arm, with the edge towards the fingers, so the
	 * handle crosses the palm and the pommel shows below the little finger.
	 */
	private static final float HOLD_TILT = 28;
	private static final float HOLD_TURN = 75;
	/** How far the animations' moves (in blocks) carry at this distance. */
	private static final float MOVE = 0.6f;
	private static final float NEAR = 0.16f;
	/** Points around a knife, in pixels, that must stay in front of the near plane. */
	private static final float[][] REACH = {{0, 10.5f}, {0, -7.5f}, {5.5f, 3}, {-2.5f, 3}};

	private static final Quaternionf ARM_RIGHT = armTurn();
	private static final Quaternionf ARM_LEFT = mirror(ARM_RIGHT);
	private static final Quaternionf HOLD_RIGHT = hold(ARM_RIGHT);
	private static final Quaternionf HOLD_LEFT = mirror(HOLD_RIGHT);
	private static final PoseStack PROBE = new PoseStack();
	private static final Vector3f POINT = new Vector3f();

	private static final KnifeAnimator ANIMATOR = new KnifeAnimator();
	private static final float[] POSE = Clip.rest();
	/** A pose (or a clip at a point in it) to show instead of the animation, for the in-game test's pictures. */
	private static float[] preview = null;
	private static int lastSlot = -1;
	private static KnifeModel lastModel = null;
	private static boolean shown = false;

	private KnifeRenderer() {
	}

	/** The arm's turn for the right hand: its length along the forearm, twisted so the fist faces the right way. */
	private static Quaternionf armTurn() {
		Vector3f wanted = new Vector3f(FOREARM).negate();
		return new Quaternionf().rotationTo(ARM_AXIS, wanted).rotateAxis((float) Math.toRadians(ARM_TWIST), ARM_AXIS.x, ARM_AXIS.y, ARM_AXIS.z);
	}

	/** The knife's turn for the right hand: the arm's, then the knife's place in the fist. */
	private static Quaternionf hold(Quaternionf arm) {
		double tilt = Math.toRadians(HOLD_TILT);
		Vector3f blade = new Vector3f(0, (float) Math.sin(tilt), (float) -Math.cos(tilt));
		Vector3f edge = new Vector3f(0, (float) Math.cos(tilt), (float) Math.sin(tilt));
		Vector3f flat = new Vector3f(edge).cross(blade);
		Matrix3f axes = new Matrix3f(edge.x, edge.y, edge.z, blade.x, blade.y, blade.z, flat.x, flat.y, flat.z);
		Quaternionf inFist = new Quaternionf().rotationY((float) Math.toRadians(HOLD_TURN)).mul(new Quaternionf().setFromNormalized(axes));
		return new Quaternionf(arm).rotateZ(ARM_LEAN).mul(inFist);
	}

	/** The same turn seen in a mirror (across the screen's middle), for the left hand. */
	private static Quaternionf mirror(Quaternionf q) {
		return new Quaternionf(q.x, -q.y, -q.z, q.w);
	}

	private static float speed(dev.threevhy.client.module.Setting.Slider slider) {
		return (float) (slider.get() / 100.0);
	}

	public static void inspect(KnifeModel model) {
		ANIMATOR.play(model.inspect(), speed(Modules.KNIVES_INSPECT_SPEED), System.nanoTime());
		Hooks.knifeInspectRan = true;
	}

	public static void use(KnifeModel model) {
		ANIMATOR.play(model.use(), speed(Modules.KNIVES_SWING_SPEED), System.nanoTime());
		Hooks.knifeSlashRan = true;
	}

	public static void slash(KnifeModel model) {
		var gameMode = Minecraft.getInstance().gameMode;
		boolean mining = gameMode != null && gameMode.isDestroying();
		ANIMATOR.slash(model, speed(Modules.KNIVES_SWING_SPEED), System.nanoTime(), mining);
		Hooks.knifeSlashRan = true;
	}

	/**
	 * For the in-game test: holds the knife still at one point of an animation ("draw", "inspect", "use" or
	 * "slash0".."slash2", {@code t} 0..1 through it), or at rest with one pose channel set ("channel3" with
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
		float push = Math.max(Math.abs(nearPush(model, true)), Math.abs(nearPush(model, false)));
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
			animator.pose(now, 0, 0, 0, current);
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
		ANIMATOR.pose(now, camera.yRot(), camera.xRot(), speed(Modules.KNIVES_IDLE), POSE);
		if (preview != null) System.arraycopy(preview, 0, POSE, 0, Clip.CHANNELS);
		boolean right = arm == HumanoidArm.RIGHT;
		float side = right ? 1 : -1;

		float push = nearPush(model, right);

		poseStack.pushPose();
		viewmodel(poseStack, side, push);
		if (Modules.KNIVES_ARM.get() && !player.isInvisible()) drawArm(player, right, poseStack, collector, light);
		knife(poseStack, model, right);
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
	private static float nearPush(KnifeModel model, boolean right) {
		PROBE.last().pose().identity();
		PROBE.last().normal().identity();
		PROBE.pushPose();
		viewmodel(PROBE, right ? 1 : -1, 0);
		knife(PROBE, model, right);
		float nearest = Float.NEGATIVE_INFINITY;
		for (float[] p : REACH) {
			nearest = Math.max(nearest, PROBE.last().pose().transformPosition(POINT.set(p[0], p[1], 0)).z);
		}
		PROBE.popPose();
		return nearest > -NEAR ? -NEAR - nearest : 0;
	}

	/**
	 * Moves {@code pose} to the fist: the base spot, the viewmodel settings and the animation, mirrored by
	 * {@code side}. Turns and tilts swing the arm about the elbow, as a real slash does; roll twists the
	 * wrist about the forearm, which is how a knife is turned over to look at it.
	 */
	private static void viewmodel(PoseStack pose, float side, float push) {
		float x = Modules.KNIVES_X.getFloat() / 100f * 0.3f;
		float y = Modules.KNIVES_Y.getFloat() / 100f * 0.3f;
		float z = Modules.KNIVES_Z.getFloat() / 100f * 0.3f;
		pose.translate(side * (BASE_X + x + POSE[Clip.X] * MOVE), BASE_Y + y + POSE[Clip.Y] * MOVE, BASE_Z + z + POSE[Clip.Z] * MOVE + push);
		pose.mulPose(Axis.ZP.rotationDegrees(side * POSE[Clip.SWEEP]));
		float ex = side * FOREARM.x * ELBOW;
		float ey = FOREARM.y * ELBOW;
		float ez = FOREARM.z * ELBOW;
		pose.translate(ex, ey, ez);
		pose.mulPose(Axis.YP.rotationDegrees(side * (Modules.KNIVES_YAW.getFloat() + POSE[Clip.YAW])));
		pose.mulPose(Axis.XP.rotationDegrees(Modules.KNIVES_PITCH.getFloat() + POSE[Clip.PITCH]));
		pose.translate(-ex, -ey, -ez);
		float roll = (float) Math.toRadians(-side * (Modules.KNIVES_ROLL.getFloat() + POSE[Clip.ROLL]));
		pose.mulPose(new Quaternionf().rotationAxis(roll, side * FOREARM.x, FOREARM.y, FOREARM.z));
	}

	/** From the fist to the knife's own frame: its place in the hand, its size, then its turn in the fingers. */
	private static void knife(PoseStack pose, KnifeModel model, boolean right) {
		pose.mulPose(right ? HOLD_RIGHT : HOLD_LEFT);
		float s = KNIFE_SCALE * model.scale * (float) (Modules.KNIVES_SCALE.get() / 100.0);
		pose.scale(s, s, s);
		pose.translate(model.spinX, model.spinY - model.grip, 0);
		pose.mulPose(Axis.YP.rotationDegrees(POSE[Clip.K_YAW]));
		pose.mulPose(Axis.XP.rotationDegrees(POSE[Clip.K_PITCH]));
		pose.mulPose(Axis.ZP.rotationDegrees(POSE[Clip.K_ROLL]));
		pose.translate(-model.spinX, -model.spinY + POSE[Clip.SLIDE], 0);
	}

	/** Your arm, the middle of its fist on the knife's grip, the forearm running back to the corner of the screen. */
	private static void drawArm(AbstractClientPlayer player, boolean right, PoseStack pose, SubmitNodeCollector collector, int light) {
		AvatarRenderer<AbstractClientPlayer> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getPlayerRenderer(player);
		Identifier skin = player.getSkin().body().texturePath();
		pose.pushPose();
		pose.mulPose(right ? ARM_RIGHT : ARM_LEFT);
		pose.scale(ARM_SCALE, ARM_SCALE, ARM_SCALE);
		pose.translate(right ? -HAND.x : HAND.x, -HAND.y, -HAND.z);
		if (right) {
			renderer.renderRightHand(pose, collector, light, skin, player.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE));
		} else {
			renderer.renderLeftHand(pose, collector, light, skin, player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE));
		}
		pose.popPose();
	}

	/** The knife's parts, each moved for the pose: a butterfly knife's handles about their pins, an energy blade's length. */
	private static void drawParts(KnifeModel model, PoseStack pose, SubmitNodeCollector collector, int light) {
		String finish = Modules.KNIVES_FINISH.get();
		KnifeMesh.Palette palette = model.palette(finish);
		Identifier atlas = KnifeAtlas.get();
		float open = Math.max(0, POSE[Clip.OPEN]);
		for (KnifeModel.Part part : model.parts()) {
			pose.pushPose();
			float glowAlpha = 1;
			switch (part.kind()) {
				case HANDLE_A, HANDLE_B -> {
					boolean a = part.kind() == KnifeModel.PartKind.HANDLE_A;
					float closed = a ? 180 : -180;
					float angle = closed * (1 - Math.min(1, open)) + (a ? POSE[Clip.HANDLE_A] : POSE[Clip.HANDLE_B]);
					pose.translate(part.pivotX(), part.pivotY(), 0);
					pose.mulPose(Axis.ZP.rotationDegrees(angle));
				}
				case ENERGY -> {
					if (open < 0.01f) {
						pose.popPose();
						continue;
					}
					pose.translate(part.pivotX(), part.pivotY(), 0);
					pose.scale(1, open, 1);
					long ms = System.currentTimeMillis();
					glowAlpha = 0.86f + 0.1f * (float) Math.sin(ms * 0.031) + 0.04f * (float) Math.sin(ms * 0.17);
				}
				default -> {
				}
			}
			KnifeMesh mesh = part.mesh();
			float alpha = glowAlpha;
			collector.submitCustomGeometry(pose, Compat.knifeRenderType(atlas, false),
					(last, out) -> mesh.emit(last, out, light, OverlayTexture.NO_OVERLAY, palette, false, 1));
			if (mesh.hasGlow()) {
				collector.submitCustomGeometry(pose, Compat.knifeRenderType(atlas, true),
						(last, out) -> mesh.emit(last, out, 0xF000F0, OverlayTexture.NO_OVERLAY, palette, true, alpha));
			}
			pose.popPose();
		}
	}
}
