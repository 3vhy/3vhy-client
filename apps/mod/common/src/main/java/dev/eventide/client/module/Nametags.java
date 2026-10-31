package dev.eventide.client.module;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.eventide.client.ui.Tokens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;

/**
 * Nametags: your own tag in third person, and players' tags drawn as a dark rounded pill with their face,
 * their name and their health in hearts. The tag is built where Minecraft would submit its own, in the same
 * place and facing the camera the same way; seen through walls like Minecraft's when the player isn't
 * sneaking, and only in plain sight when they are.
 */
public final class Nametags {
	/** Minecraft's own name tag sizes: a text pixel is 0.025 blocks. */
	private static final float SCALE = 0.025f;
	private static final float PAD = 3.5f;
	private static final float HEAD = 9;
	private static final float GAP = 3;
	private static final float HEIGHT = 13;
	private static final float RADIUS = 4.5f;
	private static final int PILL = 0xB8120E1E;
	private static final int FULL_BRIGHT = 0xF000F0;
	private static final String HEART = "❤";

	private Nametags() {
	}

	public static boolean showOwn(Entity entity) {
		Minecraft mc = Minecraft.getInstance();
		return Modules.NAMETAGS.enabled() && Modules.NAMETAGS_OWN.get() && entity == mc.player
				&& !mc.options.getCameraType().isFirstPerson() && !mc.options.hideGui;
	}

	/** Whether this player's tag is drawn as Eventide's pill instead of Minecraft's. */
	public static boolean styled(AvatarRenderState state) {
		return Modules.NAMETAGS.enabled() && Modules.NAMETAGS_STYLE.is("Eventide") && state.nameTag != null && state.nameTagAttachment != null;
	}

	/**
	 * Submits the pill for {@code state} at Minecraft's name tag spot, {@code offset} text pixels down (as
	 * Minecraft's own call takes it), facing the camera at {@code orientation}.
	 */
	public static void submit(AvatarRenderState state, PoseStack pose, SubmitNodeCollector collector, Quaternionf orientation, int offset) {
		Minecraft mc = Minecraft.getInstance();
		Font font = mc.font;
		Component name = state.nameTag;
		String hearts = hearts(mc, state.id);
		float nameW = font.width(name);
		float heartsW = hearts == null ? 0 : font.width(hearts) + 2 + font.width(HEART);
		float w = PAD + HEAD + GAP + nameW + (hearts == null ? 0 : GAP + 2 + heartsW) + PAD;
		float x0 = -w / 2f;
		float y0 = offset - 2;
		boolean seeThrough = !state.isDiscrete;
		int light = state.lightCoords;

		pose.pushPose();
		pose.translate(state.nameTagAttachment.x, state.nameTagAttachment.y + 0.5, state.nameTagAttachment.z);
		pose.mulPose(orientation);
		pose.scale(SCALE, -SCALE, SCALE);
		collector.submitCustomGeometry(pose, seeThrough ? RenderTypes.textBackgroundSeeThrough() : RenderTypes.textBackground(),
				(last, out) -> pill(last, out, x0, y0, x0 + w, y0 + HEIGHT, light));
		Identifier skin = state.skin.body().texturePath();
		float hx = x0 + PAD;
		float hy = y0 + (HEIGHT - HEAD) / 2f;
		collector.submitCustomGeometry(pose, seeThrough ? RenderTypes.textSeeThrough(skin) : RenderTypes.text(skin), (last, out) -> {
			face(last, out, hx, hy, HEAD, 8, 8, light, 0);
			face(last, out, hx - 0.4f, hy - 0.4f, HEAD + 0.8f, 40, 8, light, 0.01f);
		});
		float tx = hx + HEAD + GAP;
		text(collector, pose, name, tx, offset, 0xFFFFFFFF, seeThrough, light);
		if (hearts != null) {
			float hx2 = tx + nameW + GAP + 2;
			text(collector, pose, Component.literal(hearts), hx2, offset, 0xFFFFFFFF, seeThrough, light);
			text(collector, pose, Component.literal(HEART), hx2 + font.width(hearts) + 2, offset, Tokens.PINK, seeThrough, light);
		}
		pose.popPose();
	}

	/** The player's health in hearts, to the half heart ("10", "7.5"), or null if they can't be found. */
	private static String hearts(Minecraft mc, int id) {
		if (mc.level == null) return null;
		if (!(mc.level.getEntity(id) instanceof LivingEntity living)) return null;
		float halves = Math.round(living.getHealth() + living.getAbsorptionAmount());
		float value = halves / 2f;
		return value == Math.floor(value) ? String.valueOf((int) value) : String.valueOf(value);
	}

	private static void text(SubmitNodeCollector collector, PoseStack pose, Component text, float x, float y, int color, boolean seeThrough,
			int light) {
		if (seeThrough) {
			collector.submitText(pose, x, y, text.getVisualOrderText(), false, Font.DisplayMode.SEE_THROUGH, light, (color & 0xFFFFFF) | 0x80000000, 0, 0);
			collector.submitText(pose, x, y, text.getVisualOrderText(), false, Font.DisplayMode.NORMAL, FULL_BRIGHT, color, 0, 0);
		} else {
			collector.submitText(pose, x, y, text.getVisualOrderText(), false, Font.DisplayMode.NORMAL, light, (color & 0xFFFFFF) | 0x80000000, 0, 0);
		}
	}

	/** A rounded rectangle as quads: the middle band, then each side's rounded ends as thin slices. */
	private static void pill(PoseStack.Pose pose, VertexConsumer out, float x1, float y1, float x2, float y2, int light) {
		int steps = 6;
		float z = -0.02f;
		quad(pose, out, x1 + RADIUS, y1, x2 - RADIUS, y2, z, light);
		for (int i = 0; i < steps; i++) {
			double a0 = Math.PI / 2 * i / steps;
			double a1 = Math.PI / 2 * (i + 1) / steps;
			float dx0 = RADIUS * (float) Math.cos(a0);
			float dx1 = RADIUS * (float) Math.cos(a1);
			float dy0 = RADIUS * (float) Math.sin(a0);
			float dy1 = RADIUS * (float) Math.sin(a1);
			float left0 = x1 + RADIUS - dx0;
			float left1 = x1 + RADIUS - dx1;
			float right0 = x2 - RADIUS + dx0;
			float right1 = x2 - RADIUS + dx1;
			slice(pose, out, left0, left1, y1 + RADIUS - dy0, y1 + RADIUS - dy1, y2 - RADIUS + dy0, y2 - RADIUS + dy1, z, light);
			slice(pose, out, right1, right0, y1 + RADIUS - dy1, y1 + RADIUS - dy0, y2 - RADIUS + dy1, y2 - RADIUS + dy0, z, light);
		}
	}

	private static void quad(PoseStack.Pose pose, VertexConsumer out, float x1, float y1, float x2, float y2, float z, int light) {
		out.addVertex(pose, x1, y1, z).setColor(PILL).setLight(light);
		out.addVertex(pose, x1, y2, z).setColor(PILL).setLight(light);
		out.addVertex(pose, x2, y2, z).setColor(PILL).setLight(light);
		out.addVertex(pose, x2, y1, z).setColor(PILL).setLight(light);
	}

	/** A vertical slice of a rounded end, from x {@code xa} (top {@code ta}, bottom {@code ba}) to {@code xb}. */
	private static void slice(PoseStack.Pose pose, VertexConsumer out, float xa, float xb, float ta, float tb, float ba, float bb, float z, int light) {
		out.addVertex(pose, xa, ta, z).setColor(PILL).setLight(light);
		out.addVertex(pose, xa, ba, z).setColor(PILL).setLight(light);
		out.addVertex(pose, xb, bb, z).setColor(PILL).setLight(light);
		out.addVertex(pose, xb, tb, z).setColor(PILL).setLight(light);
	}

	/** One 8 x 8 square of the 64 x 64 skin (the face at 8,8 or the hat at 40,8) drawn {@code size} square. */
	private static void face(PoseStack.Pose pose, VertexConsumer out, float x, float y, float size, int u, int v, int light, float lift) {
		float u1 = u / 64f;
		float v1 = v / 64f;
		float u2 = (u + 8) / 64f;
		float v2 = (v + 8) / 64f;
		float z = lift;
		out.addVertex(pose, x, y, z).setColor(0xFFFFFFFF).setUv(u1, v1).setLight(light);
		out.addVertex(pose, x, y + size, z).setColor(0xFFFFFFFF).setUv(u1, v2).setLight(light);
		out.addVertex(pose, x + size, y + size, z).setColor(0xFFFFFFFF).setUv(u2, v2).setLight(light);
		out.addVertex(pose, x + size, y, z).setColor(0xFFFFFFFF).setUv(u2, v1).setLight(light);
	}
}
