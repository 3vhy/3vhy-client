package dev.threevhy.client.module;

import dev.threevhy.client.Config;
import dev.threevhy.client.Hooks;
import dev.threevhy.client.compat.Compat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

/**
 * The Player List: names the player added, and which of them are on the current server right now
 * (anyone in the server's player list counts as online). Can post a chat line when one joins or leaves.
 */
public final class PlayerList {
	private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_]{1,16}");
	/** Right after joining, the server sends its whole player list; those aren't "joins". */
	private static final long QUIET_AFTER_CONNECT_MS = 5000;

	private static Set<String> online = new HashSet<>();
	private static ClientPacketListener connection = null;
	private static long connectedAt = 0;

	private PlayerList() {
	}

	public static List<String> names() {
		return Config.get().players;
	}

	public static boolean isValidName(String name) {
		return NAME.matcher(name.trim()).matches();
	}

	/** Adds a name; false if it isn't a valid Minecraft name or is already on the list. */
	public static boolean add(String name) {
		String clean = name.trim();
		if (!isValidName(clean) || contains(clean)) return false;
		names().add(clean);
		Config.saveSoon();
		return true;
	}

	public static void remove(String name) {
		names().removeIf(each -> each.equalsIgnoreCase(name));
		online.remove(name.toLowerCase(Locale.ROOT));
		Config.saveSoon();
	}

	public static boolean contains(String name) {
		for (String each : names()) {
			if (each.equalsIgnoreCase(name)) return true;
		}
		return false;
	}

	public static boolean isOnline(String name) {
		return online.contains(name.toLowerCase(Locale.ROOT));
	}

	/** The server's entry for a listed player (their exact name, skin and ping), or null when they're offline. */
	public static PlayerInfo info(Minecraft mc, String name) {
		ClientPacketListener current = mc.getConnection();
		return current == null ? null : current.getPlayerInfoIgnoreCase(name);
	}

	/** Listed players who are online, in list order. */
	public static List<String> onlineNames() {
		List<String> result = new ArrayList<>();
		for (String name : names()) {
			if (isOnline(name)) result.add(name);
		}
		return result;
	}

	/** Players on the server who aren't on the list yet and whose name starts with {@code prefix}. */
	public static List<String> suggestions(Minecraft mc, String prefix, int limit) {
		List<String> result = new ArrayList<>();
		ClientPacketListener current = mc.getConnection();
		if (current == null) return result;
		String start = prefix.trim().toLowerCase(Locale.ROOT);
		for (PlayerInfo info : current.getOnlinePlayers()) {
			String name = Compat.profileName(info.getProfile());
			if (name == null || contains(name) || !name.toLowerCase(Locale.ROOT).startsWith(start)) continue;
			result.add(name);
		}
		result.sort(String.CASE_INSENSITIVE_ORDER);
		return result.size() > limit ? new ArrayList<>(result.subList(0, limit)) : result;
	}

	static void tick(Minecraft mc) {
		ClientPacketListener current = mc.getConnection();
		if (current == null) {
			connection = null;
			online = new HashSet<>();
			return;
		}
		if (current != connection) {
			connection = current;
			connectedAt = System.currentTimeMillis();
			online = new HashSet<>();
		}
		Set<String> now = new HashSet<>();
		for (String name : names()) {
			if (current.getPlayerInfoIgnoreCase(name) != null) now.add(name.toLowerCase(Locale.ROOT));
		}
		boolean alerts = Modules.PLAYER_LIST.enabled() && Modules.PLAYER_LIST_ALERTS.get()
				&& System.currentTimeMillis() - connectedAt > QUIET_AFTER_CONNECT_MS;
		if (alerts) {
			for (String name : names()) {
				String key = name.toLowerCase(Locale.ROOT);
				if (now.contains(key) && !online.contains(key)) alert(name, true);
				else if (!now.contains(key) && online.contains(key)) alert(name, false);
			}
		}
		online = now;
		Hooks.playerListTicked = true;
	}

	private static void alert(String name, boolean joined) {
		Compat.chat(Component.literal("§b3vhy §8» §f" + name + (joined ? " §ajoined" : " §7left")));
	}
}
