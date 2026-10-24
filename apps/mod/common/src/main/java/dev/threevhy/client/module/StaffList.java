package dev.threevhy.client.module;

import dev.threevhy.client.compat.Compat;
import dev.threevhy.client.ui.Tokens;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.PlayerTeam;

/**
 * Staff List: the staff on your server's tab list, found by the rank in front of their name (Owner,
 * Admin, Mod, Helper and so on; the words are a setting). Only what the tab list shows is read: staff
 * the server hides from it aren't looked for. Staff in spectator mode are marked, like the tab list
 * does.
 */
public final class StaffList {
	/** One staff member: their name, rank label, rank colour, tab entry (null for test entries) and whether they're spectating. */
	public record Staff(String name, String rank, int color, PlayerInfo info, boolean spectating) {
	}

	private static final long SCAN_MS = 250;
	private static final List<Staff> SIMULATED = new ArrayList<>();
	private static boolean simulating = false;
	private static List<Staff> current = List.of();
	private static long scannedAt = 0;
	private static String rankWords = null;
	private static List<String> ranks = List.of();

	private StaffList() {
	}

	/** The staff online now, highest rank first. Read from the tab list at most four times a second. */
	public static List<Staff> current(Minecraft mc) {
		long now = System.currentTimeMillis();
		if (now - scannedAt < SCAN_MS) return current;
		scannedAt = now;
		List<Staff> found = new ArrayList<>(SIMULATED);
		ClientPacketListener connection = mc.getConnection();
		if (connection != null) {
			for (PlayerInfo info : connection.getListedOnlinePlayers()) {
				if (mc.player != null && info.getProfile() != null && mc.player.getUUID().equals(Compat.profileId(info.getProfile()))) continue;
				String name = Compat.profileName(info.getProfile());
				String rank = rankOf(rankText(info, name));
				if (rank == null) continue;
				found.add(new Staff(name == null ? "?" : name, rank.toUpperCase(Locale.ROOT), colorOf(rank), info,
						info.getGameMode() == GameType.SPECTATOR));
			}
		}
		List<String> order = ranks();
		found.sort(Comparator.comparingInt((Staff staff) -> order.indexOf(staff.rank().toLowerCase(Locale.ROOT)))
				.thenComparing(staff -> staff.name().toLowerCase(Locale.ROOT)));
		current = List.copyOf(found);
		return current;
	}

	/**
	 * The text around a player's name in the tab list (their team prefix and display name) without the
	 * name itself, so a player called Devin isn't taken for a developer.
	 */
	private static String rankText(PlayerInfo info, String name) {
		StringBuilder text = new StringBuilder();
		PlayerTeam team = info.getTeam();
		if (team != null) text.append(team.getPlayerPrefix().getString()).append(' ');
		if (info.getTabListDisplayName() != null) text.append(info.getTabListDisplayName().getString());
		String all = text.toString();
		if (name == null || name.isEmpty()) return all;
		return all.replaceAll("(?i)(?<![A-Za-z0-9_])" + Pattern.quote(name) + "(?![A-Za-z0-9_])", " ");
	}

	/** The rank words to look for, in order from highest: the Ranks setting, split at commas. */
	private static List<String> ranks() {
		String words = Modules.STAFF_LIST_RANKS.getOrDefault();
		if (!words.equals(rankWords)) {
			rankWords = words;
			List<String> list = new ArrayList<>();
			for (String word : words.toLowerCase(Locale.ROOT).split(",")) {
				String trimmed = word.trim();
				if (!trimmed.isEmpty()) list.add(trimmed);
			}
			ranks = List.copyOf(list);
		}
		return ranks;
	}

	/** The first rank word found in {@code text} ("[Sr.Mod] Kai" finds mod, "Moderator" too), or null when they aren't staff. */
	public static String rankOf(String text) {
		String clean = text.replaceAll("§.", "").toLowerCase(Locale.ROOT);
		String[] tokens = clean.split("[^\\p{L}0-9]+");
		for (String rank : ranks()) {
			for (String token : tokens) {
				if (token.isEmpty()) continue;
				if (token.equals(rank) || token.startsWith(rank) && token.length() <= rank.length() + 6
						|| token.endsWith(rank) && token.length() <= rank.length() + 3) return rank;
			}
		}
		return null;
	}

	/** A rank's colour: gold owners, pink admins, ember managers, lilac developers, teal mods, blue helpers. */
	public static int colorOf(String rank) {
		return switch (rank.toLowerCase(Locale.ROOT)) {
			case "owner", "founder" -> Tokens.GOLD;
			case "admin" -> 0xFFFF6E8C;
			case "manager" -> Tokens.EMBER;
			case "dev", "developer" -> Tokens.LILAC;
			case "mod", "moderator" -> 0xFF6FDCCB;
			case "helper", "support" -> 0xFF86B4FF;
			default -> Tokens.LILAC_2;
		};
	}

	/** For the in-game test: a staff member who isn't really there. Until {@link #stopSimulating}, the list acts as if on a server. */
	public static void simulate(String name, String rank, boolean spectating) {
		simulating = true;
		SIMULATED.removeIf(staff -> staff.name().equals(name));
		SIMULATED.add(new Staff(name, rank.toUpperCase(Locale.ROOT), colorOf(rank), null, spectating));
		scannedAt = 0;
	}

	/** For the in-game test: sends a test entry away. */
	public static void leave(String name) {
		SIMULATED.removeIf(staff -> staff.name().equals(name));
		scannedAt = 0;
	}

	/** For the in-game test: back to only the real tab list. */
	public static void stopSimulating() {
		SIMULATED.clear();
		simulating = false;
		scannedAt = 0;
	}

	/** True while the in-game test is pretending to be on a server with staff. */
	public static boolean simulating() {
		return simulating;
	}
}
