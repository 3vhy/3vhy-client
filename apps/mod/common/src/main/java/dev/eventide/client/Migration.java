package dev.eventide.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Eventide used to be called 3vhy Client. On the first launch after the rename this carries the old setup
 * across, so nothing changes for the player: config/3vhy-client.json (modules, settings, profiles, the HUD
 * layout, knives, skies, Name Protect rules) becomes config/eventide.json, the old 3vhy folder in the game
 * folder (the lyrics cache, skins, the music script) becomes eventide, and key binds saved under the old
 * names in options.txt keep their keys. The old files stay where they are, with MIGRATED_TO_EVENTIDE.txt
 * saying where everything went. Setting values that named the old client are renamed as the config loads.
 */
public final class Migration {
	/** The old mod's id, its config file and its folder in the game folder. */
	public static final String OLD_ID = "threevhy";
	private static final String OLD_CONFIG = "3vhy-client.json";
	private static final String OLD_FOLDER = "3vhy";
	private static final String NOTE = "MIGRATED_TO_EVENTIDE.txt";
	/** Choice values that spelled out the old name, and what they're called now. */
	private static final Map<String, String> VALUES = Map.of(
			"3vhy Violet", "Eventide Violet",
			"Minecraft - 3vhy Client", "Minecraft - Eventide",
			"3vhy Client", "Eventide");

	private Migration() {
	}

	/** Where Eventide keeps its files in the game folder (lyrics cache, skins, the music script). */
	public static Path dataDir() {
		return FabricLoader.getInstance().getGameDir().resolve(EventideClient.MOD_ID);
	}

	public static Path configFile() {
		return FabricLoader.getInstance().getConfigDir().resolve(EventideClient.MOD_ID + ".json");
	}

	/** Copies the old client's files to Eventide's on the first launch. Runs before the config and key binds load. */
	public static void run() {
		run(FabricLoader.getInstance().getGameDir(), FabricLoader.getInstance().getConfigDir());
	}

	static void run(Path gameDir, Path configDir) {
		List<String> moved = new ArrayList<>();
		Path oldConfig = configDir.resolve(OLD_CONFIG);
		Path config = configDir.resolve(EventideClient.MOD_ID + ".json");
		Path oldData = gameDir.resolve(OLD_FOLDER);
		Path data = gameDir.resolve(EventideClient.MOD_ID);
		try {
			if (Files.isRegularFile(oldConfig) && !Files.exists(config)) {
				Files.createDirectories(configDir);
				Files.copy(oldConfig, config);
				moved.add("config/" + OLD_CONFIG + " -> config/" + config.getFileName());
			}
			if (Files.isDirectory(oldData) && !Files.exists(data)) {
				int files = copyTree(oldData, data);
				moved.add(OLD_FOLDER + "/ -> " + EventideClient.MOD_ID + "/ (" + files + " files)");
			}
			int binds = keyBinds(gameDir.resolve("options.txt"));
			if (binds > 0) moved.add(binds + " key binds in options.txt");
		} catch (IOException | RuntimeException e) {
			EventideClient.LOG.warn("Couldn't carry everything over from 3vhy Client", e);
		}
		if (moved.isEmpty()) return;
		EventideClient.LOG.info("Carried over from 3vhy Client: {}", String.join("; ", moved));
		String note = "3vhy Client is now Eventide. On " + java.time.LocalDate.now() + " Eventide copied your setup over:\n\n"
				+ String.join("\n", moved) + "\n\nEverything here was left as it was. Once you've checked Eventide has your settings, "
				+ "you can delete this folder and config/" + OLD_CONFIG + ".\n";
		try {
			Path at = Files.isDirectory(oldData) ? oldData.resolve(NOTE) : configDir.resolve("3vhy-client." + NOTE);
			Files.writeString(at, note, StandardCharsets.UTF_8);
		} catch (IOException e) {
			EventideClient.LOG.warn("Couldn't write the migration note", e);
		}
	}

	private static int copyTree(Path from, Path to) throws IOException {
		int[] count = {0};
		try (Stream<Path> walk = Files.walk(from)) {
			for (Path source : (Iterable<Path>) walk::iterator) {
				if (source.getFileName().toString().equals(NOTE)) continue;
				Path target = to.resolve(from.relativize(source).toString());
				if (Files.isDirectory(source)) {
					Files.createDirectories(target);
				} else {
					Files.createDirectories(target.getParent());
					Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
					count[0]++;
				}
			}
		}
		return count[0];
	}

	/**
	 * Key binds Minecraft saved under the old names ("key_key.threevhy.zoom") are renamed to the new ones,
	 * unless the new ones are already there. Returns how many were renamed.
	 */
	private static int keyBinds(Path options) throws IOException {
		if (!Files.isRegularFile(options)) return 0;
		List<String> lines = Files.readAllLines(options, StandardCharsets.UTF_8);
		String oldPrefix = "key_key." + OLD_ID + ".";
		String newPrefix = "key_key." + EventideClient.MOD_ID + ".";
		if (lines.stream().anyMatch(line -> line.startsWith(newPrefix))) return 0;
		int renamed = 0;
		for (int i = 0; i < lines.size(); i++) {
			if (lines.get(i).startsWith(oldPrefix)) {
				lines.set(i, newPrefix + lines.get(i).substring(oldPrefix.length()));
				renamed++;
			}
		}
		if (renamed > 0) Files.write(options, lines, StandardCharsets.UTF_8);
		return renamed;
	}

	/** Renames setting values that spelled out the old client's name. */
	public static void renameValues(Map<String, String> settings) {
		settings.replaceAll((key, value) -> value == null ? null : VALUES.getOrDefault(value, value));
	}
}
