package dev.eventide.client.module;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.eventide.client.Config;
import dev.eventide.client.ui.Tokens;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;

/**
 * Nametags: your own tag in third person, and players' tags as their face and their name (with whatever the
 * server puts in it), floating with a text shadow and no plate behind them; their health after it if you
 * turn Health on. The server's icons in a name (a rank badge like DonutSMP's media camera, a +) can each be
 * left off: every one seen is listed in the settings ({@link #icons()}), and the ones you hide are taken out
 * of the tag, in Eventide's style or Minecraft's. The tag is built where Minecraft would submit its own, in
 * the same place and facing the camera the same way; seen through walls like Minecraft's when the player
 * isn't sneaking, and only in plain sight when they are.
 */
public final class Nametags {
	/** Minecraft's own name tag sizes: a text pixel is 0.025 blocks. */
	private static final float SCALE = 0.025f;
	private static final float HEAD = 8;
	private static final float HEAD_GAP = 2.5f;
	private static final float HEALTH_GAP = 4;
	private static final int FULL_BRIGHT = 0xF000F0;
	private static final String HEART = "❤";
	/** How many icons the settings list remembers from names seen this session. */
	private static final int SEEN_LIMIT = 16;
	/** How many filtered names are kept, so a tag isn't rebuilt every frame. */
	private static final int CACHE_LIMIT = 96;

	/**
	 * One icon a server puts in names: a character drawn from a font of its own (a rank badge from the
	 * server's resource pack) or a + sign. {@code key} names it in the saved settings and {@code glyph} draws
	 * it, in its own font and colour.
	 */
	public record Icon(String key, Component glyph) {
	}

	private static final Map<String, Icon> SEEN = new LinkedHashMap<>();
	private static final Map<Component, Component> SHOWN = new LinkedHashMap<>(128, 0.75f, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<Component, Component> eldest) {
			return size() > CACHE_LIMIT;
		}
	};
	private static Set<String> hidden = Set.of();
	private static int hiddenSize = -1;
	private static int hiddenHash = 0;

	private Nametags() {
	}

	public static boolean showOwn(Entity entity) {
		Minecraft mc = Minecraft.getInstance();
		return Modules.NAMETAGS.enabled() && Modules.NAMETAGS_OWN.get() && entity == mc.player
				&& !mc.options.getCameraType().isFirstPerson() && !mc.options.hideGui;
	}

	/** Whether this player's tag is drawn in Eventide's style instead of Minecraft's. */
	public static boolean styled(AvatarRenderState state) {
		return Modules.NAMETAGS.enabled() && Modules.NAMETAGS_STYLE.is("Eventide") && state.nameTag != null && state.nameTagAttachment != null;
	}

	/**
	 * Submits the tag for {@code state} at Minecraft's name tag spot, {@code offset} text pixels down (as
	 * Minecraft's own call takes it), facing the camera at {@code orientation}.
	 */
	public static void submit(AvatarRenderState state, PoseStack pose, SubmitNodeCollector collector, Quaternionf orientation, int offset) {
		Minecraft mc = Minecraft.getInstance();
		Font font = mc.font;
		Component name = shown(state.nameTag);
		String hearts = Modules.NAMETAGS_HEALTH.get() ? hearts(mc, state.id) : null;
		float nameW = font.width(name);
		float heartsW = hearts == null ? 0 : font.width(hearts) + 1 + font.width(HEART);
		float w = HEAD + HEAD_GAP + nameW + (hearts == null ? 0 : HEALTH_GAP + heartsW);
		float x0 = -w / 2f;
		boolean seeThrough = !state.isDiscrete;
		int light = state.lightCoords;

		pose.pushPose();
		pose.translate(state.nameTagAttachment.x, state.nameTagAttachment.y + 0.5, state.nameTagAttachment.z);
		pose.mulPose(orientation);
		pose.scale(SCALE, -SCALE, SCALE);
		Identifier skin = state.skin.body().texturePath();
		float hy = offset + (font.lineHeight - 1 - HEAD) / 2f;
		collector.submitCustomGeometry(pose, seeThrough ? RenderTypes.textSeeThrough(skin) : RenderTypes.text(skin), (last, out) -> {
			face(last, out, x0, hy, HEAD, 8, 8, light, 0);
			face(last, out, x0 - 0.4f, hy - 0.4f, HEAD + 0.8f, 40, 8, light, 0.01f);
		});
		float tx = x0 + HEAD + HEAD_GAP;
		text(collector, pose, name, tx, offset, 0xFFFFFFFF, seeThrough, light);
		if (hearts != null) {
			float hx = tx + nameW + HEALTH_GAP;
			text(collector, pose, Component.literal(hearts), hx, offset, 0xFFFFFFFF, seeThrough, light);
			text(collector, pose, Component.literal(HEART), hx + font.width(hearts) + 1, offset, Tokens.PINK, seeThrough, light);
		}
		pose.popPose();
	}

	/**
	 * {@code name} as the tag shows it: without the icons you've hidden, and with the space they leave folded
	 * up. Unchanged while Nametags is off or nothing is hidden. Names are filtered once and remembered.
	 */
	public static Component shown(Component name) {
		if (name == null || !Modules.NAMETAGS.enabled()) return name;
		refreshHidden();
		Component cached = SHOWN.get(name);
		if (cached != null) return cached;
		Component out = filter(name, hidden);
		SHOWN.put(name, out);
		return out;
	}

	/** Every icon seen in names this session, then any hidden ones not seen yet, in the order they turned up. */
	public static List<Icon> icons() {
		refreshHidden();
		List<Icon> out = new ArrayList<>(SEEN.values());
		for (String key : Config.get().hiddenNameIcons) {
			if (SEEN.containsKey(key)) continue;
			Component glyph = glyphFor(key);
			if (glyph != null) out.add(new Icon(key, glyph));
		}
		return out;
	}

	public static boolean hidden(Icon icon) {
		refreshHidden();
		return hidden.contains(icon.key());
	}

	/** Hides an icon on everyone's tag, or brings it back. */
	public static void toggle(Icon icon) {
		List<String> list = Config.get().hiddenNameIcons;
		if (!list.remove(icon.key())) list.add(icon.key());
		Config.saveSoon();
		refreshHidden();
	}

	private static void refreshHidden() {
		List<String> list = Config.get().hiddenNameIcons;
		int hash = list.hashCode();
		if (list.size() == hiddenSize && hash == hiddenHash) return;
		hiddenSize = list.size();
		hiddenHash = hash;
		hidden = new HashSet<>(list);
		SHOWN.clear();
	}

	/**
	 * Rebuilds {@code name} without the hidden icons, noting every icon it has on the way. A character is an
	 * icon when it's a + sign, sits in Unicode's private use area (where resource packs put their pictures),
	 * or comes from a font other than Minecraft's own and isn't a word in that font (a short run, or not a
	 * letter or digit), so a whole name written in a fancy font stays a name.
	 */
	static Component filter(Component name, Set<String> hide) {
		List<String> texts = new ArrayList<>();
		List<Style> styles = new ArrayList<>();
		boolean[] lastSpace = {true};
		boolean[] changed = {false};
		name.visit((style, text) -> {
			boolean ownFont = !FontDescription.DEFAULT.equals(style.getFont());
			int letters = 0;
			for (int i = 0; i < text.length(); ) {
				int cp = text.codePointAt(i);
				if (!Character.isWhitespace(cp)) letters++;
				i += Character.charCount(cp);
			}
			boolean shortRun = letters <= 2;
			StringBuilder kept = new StringBuilder(text.length());
			for (int i = 0; i < text.length(); ) {
				int cp = text.codePointAt(i);
				i += Character.charCount(cp);
				if (isIcon(cp, ownFont, shortRun)) {
					String key = key(cp, style.getFont());
					note(key, cp, style);
					if (hide.contains(key)) {
						changed[0] = true;
						continue;
					}
				}
				boolean space = Character.isWhitespace(cp);
				if (space && lastSpace[0]) {
					changed[0] = true;
					continue;
				}
				lastSpace[0] = space;
				kept.appendCodePoint(cp);
			}
			if (!kept.isEmpty()) {
				texts.add(kept.toString());
				styles.add(style);
			}
			return Optional.empty();
		}, Style.EMPTY);
		if (!changed[0]) return name;
		int last = texts.size() - 1;
		while (last >= 0) {
			String trimmed = texts.get(last).stripTrailing();
			if (!trimmed.isEmpty()) {
				texts.set(last, trimmed);
				break;
			}
			last--;
		}
		MutableComponent out = Component.empty();
		for (int i = 0; i <= last; i++) out.append(Component.literal(texts.get(i)).withStyle(styles.get(i)));
		return out;
	}

	private static boolean isIcon(int cp, boolean ownFont, boolean shortRun) {
		if (cp == '+' || cp == 0xFF0B || cp == 0x2795 || cp == 0x271A) return true;
		if (cp >= 0xE000 && cp <= 0xF8FF || cp >= 0xF0000) return true;
		return ownFont && !Character.isWhitespace(cp) && (shortRun || !Character.isLetterOrDigit(cp));
	}

	private static String key(int cp, FontDescription font) {
		String where = font instanceof FontDescription.Resource resource ? resource.id().toString() : font.toString();
		return Integer.toHexString(cp) + "@" + where;
	}

	private static void note(String key, int cp, Style style) {
		if (SEEN.containsKey(key)) return;
		if (SEEN.size() >= SEEN_LIMIT) {
			String drop = null;
			for (String each : SEEN.keySet()) {
				if (!hidden.contains(each)) {
					drop = each;
					break;
				}
			}
			if (drop == null) return;
			SEEN.remove(drop);
		}
		Style look = Style.EMPTY.withColor(style.getColor()).withBold(style.isBold()).withFont(style.getFont());
		SEEN.put(key, new Icon(key, Component.literal(new String(Character.toChars(cp))).withStyle(look)));
	}

	/** Draws a saved icon again from its key, for one hidden before it was seen this session (null if it can't be). */
	private static Component glyphFor(String key) {
		int at = key.indexOf('@');
		if (at <= 0) return null;
		try {
			int cp = Integer.parseInt(key.substring(0, at), 16);
			Identifier font = Identifier.tryParse(key.substring(at + 1));
			if (font == null) return null;
			return Component.literal(new String(Character.toChars(cp))).withStyle(Style.EMPTY.withFont(new FontDescription.Resource(font)));
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	/** For the in-game test: forgets the icons seen so far. */
	public static void forgetIcons() {
		SEEN.clear();
		SHOWN.clear();
	}

	/** The player's health in hearts, to the half heart ("10", "7.5"), or null if they can't be found. */
	private static String hearts(Minecraft mc, int id) {
		if (mc.level == null) return null;
		if (!(mc.level.getEntity(id) instanceof LivingEntity living)) return null;
		float halves = Math.round(living.getHealth() + living.getAbsorptionAmount());
		float value = halves / 2f;
		return value == Math.floor(value) ? String.valueOf((int) value) : String.valueOf(value);
	}

	/**
	 * One piece of the tag's text with a shadow. A tag that shows through walls is drawn twice, as Minecraft's
	 * is: faint where it's hidden, then in full where it's in sight.
	 */
	private static void text(SubmitNodeCollector collector, PoseStack pose, Component text, float x, float y, int color, boolean seeThrough,
			int light) {
		if (seeThrough) {
			collector.submitText(pose, x, y, text.getVisualOrderText(), false, Font.DisplayMode.SEE_THROUGH, light, (color & 0xFFFFFF) | 0x80000000, 0, 0);
			collector.submitText(pose, x, y, text.getVisualOrderText(), true, Font.DisplayMode.NORMAL, FULL_BRIGHT, color, 0, 0);
		} else {
			collector.submitText(pose, x, y, text.getVisualOrderText(), true, Font.DisplayMode.NORMAL, light, color, 0, 0);
		}
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
