package dev.threevhy.client.module;

import dev.threevhy.client.compat.Compat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** Counts totem pops per player for the current life. Fed by ClientPacketListenerMixin. */
public final class PopTracker {
	/** Entity event Minecraft sends when a totem of undying is used. */
	private static final byte TOTEM_POP = 35;
	/** Entity event for a living entity dying. */
	private static final byte DEATH = 3;

	public record Entry(UUID id, String name, int pops) {
	}

	private static final Map<UUID, Entry> POPS = new HashMap<>();

	private PopTracker() {
	}

	public static void onEntityEvent(Entity entity, byte event) {
		if (!(entity instanceof Player player)) {
			return;
		}
		UUID id = player.getUUID();
		String name = player.getName().getString();
		if (event == TOTEM_POP) {
			Entry previous = POPS.get(id);
			int pops = previous == null ? 1 : previous.pops() + 1;
			POPS.put(id, new Entry(id, name, pops));
			if (Modules.POP_NOTIFIER.enabled()) {
				boolean self = player == Minecraft.getInstance().player;
				String who = self ? "You" : name;
				Compat.chat(Component.literal("§b3vhy §8» §f" + who + " §7popped §b" + pops + " §7"
						+ (pops == 1 ? "totem" : "totems")));
			}
		} else if (event == DEATH) {
			Entry previous = POPS.remove(id);
			if (previous != null && Modules.POP_NOTIFIER.enabled() && player != Minecraft.getInstance().player) {
				Compat.chat(Component.literal("§b3vhy §8» §f" + name + " §7died after popping §b" + previous.pops()
						+ " §7" + (previous.pops() == 1 ? "totem" : "totems")));
			}
		}
	}

	/** Players with pops, most first, for the HUD. */
	public static List<Entry> top(int limit) {
		List<Entry> list = new ArrayList<>(POPS.values());
		list.sort((a, b) -> Integer.compare(b.pops(), a.pops()));
		return list.size() > limit ? list.subList(0, limit) : list;
	}

	public static void reset() {
		POPS.clear();
	}
}
