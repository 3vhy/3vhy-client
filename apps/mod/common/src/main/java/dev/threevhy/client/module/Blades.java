package dev.threevhy.client.module;

import dev.threevhy.client.Hooks;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Blades: swords (and axes, if asked) drawn as a katana or a scythe in the material's colours, in your
 * hand, other players' hands and the inventory, and an inspect animation (I by default) that turns the
 * blade over to show it off. Only how they look changes; they're still the same swords.
 */
public final class Blades {
	private static final Set<String> MATERIALS = Set.of("wooden", "stone", "copper", "iron", "golden", "diamond", "netherite");
	private static final Map<Identifier, Identifier[]> SWAPS = new HashMap<>();
	private static final long INSPECT_MS = 2400;

	private static long inspectAt = 0;
	private static int inspectSlot = -1;

	private Blades() {
	}

	/** The model to draw for an item: a blade's in place of a sword's when Blades is on. */
	public static Identifier model(Identifier model, ItemStack stack, ItemOwner owner) {
		if (model == null || !Modules.BLADES.enabled() || !"minecraft".equals(model.getNamespace())) return model;
		if (owner instanceof Player player && player != Minecraft.getInstance().player && !Modules.BLADES_OTHERS.get()) return model;
		Identifier[] swap = SWAPS.computeIfAbsent(model, Blades::swapsFor);
		if (swap == null) return model;
		Identifier blade = swap[Modules.BLADES_STYLE.is("Scythe") ? 1 : 0];
		if (blade == null) return model;
		if (model.getPath().endsWith("_axe") && !Modules.BLADES_AXES.get()) return model;
		Hooks.bladeModelSwapped = true;
		return blade;
	}

	private static Identifier[] swapsFor(Identifier model) {
		String path = model.getPath();
		String material;
		if (path.endsWith("_sword")) material = path.substring(0, path.length() - "_sword".length());
		else if (path.endsWith("_axe")) material = path.substring(0, path.length() - "_axe".length());
		else return new Identifier[] {null, null};
		if (!MATERIALS.contains(material)) material = "iron";
		return new Identifier[] {Identifier.fromNamespaceAndPath("threevhy", "katana_" + material),
				Identifier.fromNamespaceAndPath("threevhy", "scythe_" + material)};
	}

	/** Whether this is something Blades draws as a blade (and so can inspect). */
	public static boolean isBlade(ItemStack stack) {
		if (stack.isEmpty()) return false;
		String id = stack.getItem().toString();
		return id.endsWith("_sword") || Modules.BLADES_AXES.get() && id.endsWith("_axe") && !id.endsWith("pickaxe");
	}

	/** The inspect key: turns the blade in your hand over for a look. */
	public static void inspect(Minecraft mc) {
		LocalPlayer player = mc.player;
		if (!Modules.BLADES.enabled() || player == null || !isBlade(player.getMainHandItem())) return;
		inspectAt = System.currentTimeMillis();
		inspectSlot = player.getInventory().getSelectedSlot();
	}

	/** Stops inspecting when you swing, switch items or let go of the blade. */
	public static void tick(Minecraft mc) {
		if (inspectAt == 0) return;
		LocalPlayer player = mc.player;
		if (player == null || player.swinging || player.getInventory().getSelectedSlot() != inspectSlot || !isBlade(player.getMainHandItem())
				|| System.currentTimeMillis() - inspectAt > INSPECT_MS) {
			inspectAt = 0;
		}
	}

	/**
	 * How far through the inspect animation the main hand is (0..1), or -1 when it isn't inspecting.
	 * The off hand never inspects.
	 */
	public static float inspectProgress(InteractionHand hand) {
		if (inspectAt == 0 || hand != InteractionHand.MAIN_HAND) return -1;
		float t = (System.currentTimeMillis() - inspectAt) / (float) INSPECT_MS;
		return t >= 1 ? -1 : t;
	}
}
