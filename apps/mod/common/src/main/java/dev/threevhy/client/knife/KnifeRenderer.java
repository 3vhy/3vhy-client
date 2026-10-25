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
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Draws the knife in your hand, in place of the held item: your arm holding it, low on the right (left
 * for left-handed players) with the blade angled up towards the middle of the screen so it reads at a
 * glance, moved by {@link KnifeAnimator} and the Knives viewmodel settings. The hand is drawn after the
 * world with the depth cleared, so it never sinks into walls; the knife is also kept clear of the camera's
 * near plane, so big flips never cut it off.
 */
public final class KnifeRenderer {
	/** Where the wrist sits, in blocks from the eye: right, down, forward. */
	private static final float BASE_X = 0.38f;
	private static final float BASE_Y = -0.38f;
	private static final float BASE_Z = -0.66f;
	/** The blade's angle on screen (degrees from straight up, towards the middle) and how far it leans away. */
	private static final float POINT = 22;
	private static final float LEAN = -16;
	/** How big a knife is: its pixels in blocks. */
	private static final float KNIFE_SCALE = 1.0f / 16f;
	/** Which way the forearm runs from the wrist (towards the bottom right, mostly towards you). */
	private static final Vector3f FOREARM = new Vector3f(0.78f, -0.3f, 0.55f).normalize();
	private static final float ARM_TWIST = 70;
	private static final float ARM_SCALE = 0.85f;
	/** How far down the forearm the elbow is, where slashes swing from. */
	private static final float ELBOW = 0.42f;
	/** The middle of the fist on the arm model, two pixels in from the end, where the knife's grip goes. */
	private static final Vector3f HAND = new Vector3f(-0.4246f, 0.616f, 0);
	private static final Vector3f ARM_AXIS = new Vector3f(-0.0998f, 0.995f, 0).normalize();
	private static final float NEAR = 0.16f;

	private static final KnifeAnimator ANIMATOR = new KnifeAnimator();
	private static final float[] POSE = Clip.rest();
	/** A pose (or a clip at a point in it) to show instead of the animation, for the in-game test's pictures. */
	private static float[] preview = null;
	private static int lastSlot = -1;
	private static KnifeModel lastModel = null;
	private static boolean shown = false;

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
		ANIMATOR.play(model.use(), speed(Modules.KNIVES_SWING_SPEED), System.nanoTime());
		Hooks.knifeSlashRan = true;
	}

	public static void slash(KnifeModel model) {
		ANIMATOR.slash(model, speed(Modules.KNIVES_SWING_SPEED), System.nanoTime());
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

		poseStack.pushPose();
		transform(poseStack, model, side, 0);
		float z = poseStack.last().pose().transformPosition(new Vector3f(0, 3, 0)).z;
		poseStack.popPose();
		float push = z > -NEAR ? -NEAR - z : 0;

		poseStack.pushPose();
		viewmodel(poseStack, side, push);
		if (Modules.KNIVES_ARM.get() && !player.isInvisible()) drawArm(player, right, side, poseStack, collector, light);
		knife(poseStack, model, side);
		drawParts(model, poseStack, collector, light);
		poseStack.popPose();
		Hooks.knifeRendered = true;
		return true;
	}

	/**
	 * Moves {@code pose} to the wrist: the base spot, the viewmodel settings and the animation, mirrored by
	 * {@code side}. Turns and tilts swing the arm about the elbow, as a real slash does; roll twists the
	 * wrist about the forearm, which is how a knife is turned over to look at it.
	 */
	private static void viewmodel(PoseStack pose, float side, float push) {
		float x = Modules.KNIVES_X.getFloat() / 100f * 0.3f;
		float y = Modules.KNIVES_Y.getFloat() / 100f * 0.3f;
		float z = Modules.KNIVES_Z.getFloat() / 100f * 0.3f;
		pose.translate(side * (BASE_X + x + POSE[Clip.X]), BASE_Y + y + POSE[Clip.Y], BASE_Z + z + POSE[Clip.Z] + push);
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

	/** From the wrist to the knife's own frame: its hold, size and the turn in the hand. */
	private static void knife(PoseStack pose, KnifeModel model, float side) {
		pose.mulPose(Axis.XP.rotationDegrees(LEAN));
		pose.mulPose(Axis.ZP.rotationDegrees(side * POINT));
		if (side < 0) pose.mulPose(Axis.YP.rotationDegrees(180));
		float s = KNIFE_SCALE * model.scale * (float) (Modules.KNIVES_SCALE.get() / 100.0);
		pose.scale(s, s, s);
		pose.translate(model.spinX, model.spinY, 0);
		pose.mulPose(Axis.YP.rotationDegrees(POSE[Clip.K_YAW]));
		pose.mulPose(Axis.XP.rotationDegrees(POSE[Clip.K_PITCH]));
		pose.mulPose(Axis.ZP.rotationDegrees(POSE[Clip.K_ROLL]));
		pose.translate(-model.spinX, -model.spinY + POSE[Clip.SLIDE], 0);
	}

	private static void transform(PoseStack pose, KnifeModel model, float side, float push) {
		viewmodel(pose, side, push);
		knife(pose, model, side);
	}

	/** Your arm, its hand on the wrist point, the forearm running back towards the corner of the screen. */
	private static void drawArm(AbstractClientPlayer player, boolean right, float side, PoseStack pose, SubmitNodeCollector collector, int light) {
		AvatarRenderer<AbstractClientPlayer> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getPlayerRenderer(player);
		Identifier skin = player.getSkin().body().texturePath();
		pose.pushPose();
		Vector3f wanted = new Vector3f(-side * FOREARM.x, -FOREARM.y, -FOREARM.z);
		Vector3f axis = new Vector3f(right ? ARM_AXIS.x : -ARM_AXIS.x, ARM_AXIS.y, ARM_AXIS.z);
		Quaternionf turn = new Quaternionf().rotationTo(axis, wanted);
		turn.rotateAxis((float) Math.toRadians(side * ARM_TWIST), axis.x, axis.y, axis.z);
		pose.mulPose(turn);
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
