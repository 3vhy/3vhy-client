package dev.threevhy.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;

/** Everything the player changes in 3vhy Settings, saved to config/3vhy-client.json. */
public final class Config {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static Config instance = new Config();
	private static long saveAt = 0;

	/** Module id -> on/off. Missing entries use the module's default. */
	public Map<String, Boolean> modules = new LinkedHashMap<>();
	/** HUD widget id -> [x, y], each 0..1 across the free space on screen. */
	public Map<String, double[]> hud = new LinkedHashMap<>();
	public float hudScale = 1.0f;
	public float zoomFactor = 4.0f;
	public boolean zoomCinematic = true;
	public boolean discordShowServer = true;
	/** Overrides the Discord application ID the mod was built with. */
	public String discordAppId = "";
	/** The player's own damage tilt, restored when "No hurt cam" is turned off. */
	public double savedDamageTilt = 1.0;

	public static Config get() {
		return instance;
	}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("3vhy-client.json");
	}

	public static void load() {
		Path path = file();
		if (!Files.isRegularFile(path)) {
			return;
		}
		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			Config loaded = GSON.fromJson(reader, Config.class);
			if (loaded != null) {
				if (loaded.modules == null) loaded.modules = new LinkedHashMap<>();
				if (loaded.hud == null) loaded.hud = new LinkedHashMap<>();
				if (loaded.discordAppId == null) loaded.discordAppId = "";
				instance = loaded;
			}
		} catch (Exception e) {
			ThreevhyClient.LOG.warn("Couldn't read 3vhy-client.json, using defaults", e);
		}
	}

	public static void save() {
		saveAt = 0;
		Path path = file();
		try {
			Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
				GSON.toJson(instance, writer);
			}
		} catch (IOException e) {
			ThreevhyClient.LOG.warn("Couldn't save 3vhy-client.json", e);
		}
	}

	/** Saves about a second later, so dragging or clicking quickly doesn't hit the disk every frame. */
	public static void saveSoon() {
		saveAt = System.currentTimeMillis() + 1000;
	}

	static void tick() {
		if (saveAt != 0 && System.currentTimeMillis() >= saveAt) {
			save();
		}
	}
}
