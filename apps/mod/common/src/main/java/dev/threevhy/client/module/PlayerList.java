package dev.threevhy.client.module;

import dev.threevhy.client.Config;
import dev.threevhy.client.Hooks;
import dev.threevhy.client.compat.Compat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket;

/**
 * The Player List: names the player added, and which of them are on the current server right now.
 * Someone counts as online when they're in the server's player list, or, for servers like DonutSMP
 * whose tab list doesn't show everyone, when the server suggests their name for "/msg name" (the
 * same suggestions you get by pressing Tab in chat). Can post a chat line when one joins or leaves.
 */
public final class PlayerList {
	private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_]{1,16}");
	/** Right after joining, the server sends its whole player list; those aren't "joins". */
	private static final long QUIET_AFTER_CONNECT_MS = 5000;

	/** One name is checked with the server this often, taking turns. */
	private static final long ASK_EVERY_MS = 2500;
	/** Request ids for our suggestion requests, far from the ones the chat box uses. */
	private static final AtomicInteger NEXT_REQUEST = new AtomicInteger(0x3B0000);

	private static Set<String> online = new HashSet<>();
	private static ClientPacketListener connection = null;
	private static long connectedAt = 0;
	/** Each name's online state as last announced, so joins and leaves can be told apart. */
	private static final Map<String, Boolean> STATES = new HashMap<>();
	/** Suggestion request id -> name it asked about. Filled here, answered on the network thread. */
	private static final Map<Integer, String> PENDING = new ConcurrentHashMap<>();
	/** Name -> when the server last suggested it. */
	private static final Map<String, Long> SEEN = new ConcurrentHashMap<>();
	/** Names the server has answered about at least once since joining. */
	private static final Set<String> CHECKED = ConcurrentHashMap.newKeySet();
	private static int askIndex = 0;
	private static long nextAskAt = 0;

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
		String key = name.toLowerCase(Locale.ROOT);
		online.remove(key);
		STATES.remove(key);
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
		long now = System.currentTimeMillis();
		if (current == null) {
			connection = null;
			online = new HashSet<>();
			return;
		}
		if (current != connection) {
			connection = current;
			connectedAt = now;
			online = new HashSet<>();
			STATES.clear();
			PENDING.clear();
			SEEN.clear();
			CHECKED.clear();
		}
		boolean ask = Modules.PLAYER_LIST.enabled() && Modules.PLAYER_LIST_ASK.get() && mc.player != null
				&& now - connectedAt > 3000;
		if (ask && now >= nextAskAt) {
			nextAskAt = now + ASK_EVERY_MS;
			askNext(current);
		}
		boolean settled = now - connectedAt > QUIET_AFTER_CONNECT_MS;
		boolean alerts = Modules.PLAYER_LIST.enabled() && Modules.PLAYER_LIST_ALERTS.get();
		Set<String> nowOnline = new HashSet<>();
		for (String name : names()) {
			String key = name.toLowerCase(Locale.ROOT);
			boolean inTab = current.getPlayerInfoIgnoreCase(name) != null;
			Long seen = SEEN.get(key);
			boolean isOnline = inTab || (seen != null && now - seen < staleAfter());
			if (isOnline) nowOnline.add(key);
			// A name's first known state after joining is where it starts, not a join or a leave
			boolean known = inTab || !ask || CHECKED.contains(key);
			if (!settled || !known) continue;
			Boolean before = STATES.put(key, isOnline);
			if (alerts && before != null && before != isOnline) alert(name, isOnline);
		}
		online = nowOnline;
		Hooks.playerListTicked = true;
	}

	/** How long a "seen by the server" answer counts, if no newer answer came. */
	private static long staleAfter() {
		return ASK_EVERY_MS * (names().size() + 2) * 3L;
	}

	/** Asks the server to complete "/msg name" for the next listed name that isn't in the tab list. */
	private static void askNext(ClientPacketListener current) {
		List<String> list = names();
		if (list.isEmpty()) return;
		if (PENDING.size() > 64) PENDING.clear();
		String command = Modules.PLAYER_LIST_COMMAND.getOrDefault().trim();
		if (command.startsWith("/")) command = command.substring(1);
		for (int tries = 0; tries < list.size(); tries++) {
			String name = list.get(Math.floorMod(askIndex++, list.size()));
			if (current.getPlayerInfoIgnoreCase(name) != null) continue;
			askAbout(current, command, name);
			return;
		}
	}

	/** Sends one "/command name" suggestion request. Public for the in-game test. */
	public static void askAbout(ClientPacketListener current, String command, String name) {
		int id = NEXT_REQUEST.getAndIncrement();
		PENDING.put(id, name.toLowerCase(Locale.ROOT));
		current.send(new ServerboundCommandSuggestionPacket(id, "/" + command + " " + name));
	}

	/**
	 * Called with every command suggestion the server sends. Returns true when it answers one of the
	 * Player List's own questions, which then never reaches the chat box. Runs on the network thread.
	 */
	public static boolean onSuggestions(int id, List<String> suggestions) {
		String name = PENDING.remove(id);
		if (name == null) return false;
		boolean found = false;
		for (String suggestion : suggestions) {
			if (suggestion.equalsIgnoreCase(name)) found = true;
		}
		if (found) SEEN.put(name, System.currentTimeMillis());
		else SEEN.remove(name);
		CHECKED.add(name);
		Hooks.playerListAskedServer = true;
		return true;
	}

	/** What the server checks are doing, for the in-game test's log. */
	public static String describeChecks() {
		return "asked " + (NEXT_REQUEST.get() - 0x3B0000) + ", waiting " + PENDING + ", checked " + CHECKED + ", seen " + SEEN.keySet()
				+ ", answers received " + Hooks.suggestionPackets + " (last id " + Hooks.lastSuggestionId + ")";
	}

	/** Whether the server has answered about this name since joining (for the in-game test). */
	public static boolean checkedWithServer(String name) {
		return CHECKED.contains(name.toLowerCase(Locale.ROOT));
	}

	/** Whether the server's last answer said this name is online (for the in-game test). */
	public static boolean seenByServer(String name) {
		return SEEN.containsKey(name.toLowerCase(Locale.ROOT));
	}

	private static void alert(String name, boolean joined) {
		Compat.chat(Component.literal("§b3vhy §8» §f" + name + (joined ? " §ajoined" : " §7left")));
	}
}
