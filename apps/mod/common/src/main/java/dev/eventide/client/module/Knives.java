package dev.eventide.client.module;

import dev.eventide.client.knife.KnifeModel;
import dev.eventide.client.knife.KnifeRenderer;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Knives: the hotbar slots you pick, and (with Auto knives) every item of a kind, held as a knife with
 * its own draw, slash and inspect animations. Picked slots and their knives are saved in the profile as
 * one line ("0:karambit,3:butterfly"). Only how the item looks in your hand changes.
 */
public final class Knives {
	private static final KnifeModel[] SLOTS = new KnifeModel[9];
	private static String parsedFrom = null;
	private static boolean useWasDown = false;

	private Knives() {
	}

	private static void parse() {
		String raw = Modules.KNIVES_SLOTS.get();
		if (raw.equals(parsedFrom)) return;
		parsedFrom = raw;
		java.util.Arrays.fill(SLOTS, null);
		for (String part : raw.split(",")) {
			int colon = part.indexOf(':');
			if (colon <= 0) continue;
			try {
				int slot = Integer.parseInt(part.substring(0, colon).trim());
				KnifeModel model = KnifeModel.byLabel(part.substring(colon + 1).trim());
				if (slot >= 0 && slot < 9 && model != null) SLOTS[slot] = model;
			} catch (NumberFormatException ignored) {
				// A hand-edited config with a bad slot number: skip that entry
			}
		}
	}

	private static void save() {
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < 9; i++) {
			if (SLOTS[i] == null) continue;
			if (!out.isEmpty()) out.append(',');
			out.append(i).append(':').append(SLOTS[i].id());
		}
		Modules.KNIVES_SLOTS.set(out.toString());
		parsedFrom = out.toString();
	}

	/** The knife picked for hotbar slot {@code slot} (0..8), or null. */
	public static KnifeModel slot(int slot) {
		parse();
		return slot >= 0 && slot < 9 ? SLOTS[slot] : null;
	}

	/** Marks or unmarks a hotbar slot as a knife; a new one gets the knife picked in the settings. */
	public static void toggleSlot(int slot) {
		parse();
		if (slot < 0 || slot >= 9) return;
		SLOTS[slot] = SLOTS[slot] == null ? defaultModel() : null;
		save();
	}

	/** Picks the next (or previous, for {@code step} -1) knife for a marked slot. */
	public static void cycleSlot(int slot, int step) {
		parse();
		if (slot < 0 || slot >= 9 || SLOTS[slot] == null) return;
		KnifeModel[] all = KnifeModel.values();
		SLOTS[slot] = all[Math.floorMod(SLOTS[slot].ordinal() + step, all.length)];
		save();
	}

	public static void setSlot(int slot, KnifeModel model) {
		parse();
		if (slot < 0 || slot >= 9) return;
		SLOTS[slot] = model;
		save();
	}

	/** The knife picked in the settings, for auto knives and newly marked slots. */
	public static KnifeModel defaultModel() {
		KnifeModel model = KnifeModel.byLabel(Modules.KNIVES_MODEL.get());
		return model == null ? KnifeModel.KARAMBIT : model;
	}

	/** Whether Auto knives makes this item a knife. */
	public static boolean autoMatches(ItemStack stack) {
		if (!Modules.KNIVES_AUTO.get() || stack.isEmpty()) return false;
		String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
		return switch (Modules.KNIVES_AUTO_TYPE.get()) {
			case "Swords and axes" -> id.endsWith("_sword") || id.endsWith("_axe") && !id.endsWith("pickaxe");
			case "Item ID" -> {
				String wanted = Modules.KNIVES_AUTO_ITEM.get().trim().toLowerCase(Locale.ROOT);
				yield !wanted.isEmpty() && (id.equals(wanted) || !wanted.contains(":") && id.equals("minecraft:" + wanted));
			}
			default -> id.endsWith("_sword");
		};
	}

	/**
	 * The knife {@code player} holds as {@code stack} in hotbar slot {@code slot}, or null when it isn't one
	 * (or Knives is off). Only your own hands get knives.
	 */
	public static KnifeModel modelFor(Player player, ItemStack stack, int slot) {
		if (!Modules.KNIVES.enabled() || player != Minecraft.getInstance().player) return null;
		KnifeModel picked = slot(slot);
		if (picked != null) return picked;
		return autoMatches(stack) ? defaultModel() : null;
	}

	/** The knife in your main hand right now, or null. */
	public static KnifeModel held(Minecraft mc) {
		LocalPlayer player = mc.player;
		if (player == null) return null;
		return modelFor(player, player.getMainHandItem(), player.getInventory().getSelectedSlot());
	}

	/** Whether knives are drawn with their own animations (Custom animations on); off, the item is drawn as usual. */
	public static boolean animated() {
		return Modules.KNIVES_ANIMATIONS.get();
	}

	/** The inspect key: a long, showy look at the knife. */
	public static void inspect(Minecraft mc) {
		KnifeModel model = held(mc);
		if (model != null && animated() && mc.screen == null) KnifeRenderer.inspect(model);
	}

	/** Watches the use key, for the knife's heavy stab on right click. */
	public static void tick(Minecraft mc) {
		boolean down = mc.options.keyUse.isDown() && mc.screen == null;
		if (down && !useWasDown) {
			KnifeModel model = held(mc);
			if (model != null && animated()) KnifeRenderer.use(model);
		}
		useWasDown = down;
	}

	/** Your arm swung (a hit, a click on a block): the knife slashes. */
	public static void onSwing(InteractionHand hand) {
		if (hand != InteractionHand.MAIN_HAND) return;
		KnifeModel model = held(Minecraft.getInstance());
		if (model != null && animated()) KnifeRenderer.slash(model);
	}

	/** Puts the viewmodel's position, turn, size and speeds back to their defaults. */
	public static void resetViewmodel() {
		for (Setting setting : Modules.KNIVES.settings()) {
			if (setting instanceof Setting.Slider) setting.reset();
		}
	}
}
