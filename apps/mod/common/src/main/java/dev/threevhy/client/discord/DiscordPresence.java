package dev.threevhy.client.discord;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.threevhy.client.Config;
import dev.threevhy.client.ThreevhyClient;
import dev.threevhy.client.module.Modules;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

/**
 * "Playing 3vhy Client" on Discord, with what you're doing in game. Needs a Discord application
 * of your own; its ID comes from the build (THREEVHY_DISCORD_APP_ID) or 3vhy-client.json.
 */
public final class DiscordPresence {
	private static final String DOWNLOAD_URL = "https://github.com/xsaaacc-collab/3vhy-client/releases/latest";

	private static final ExecutorService IO = Executors.newSingleThreadExecutor(r -> {
		Thread thread = new Thread(r, "3vhy Discord");
		thread.setDaemon(true);
		return thread;
	});
	private static final AtomicReference<String> WANTED = new AtomicReference<>(null);
	private static final long STARTED = System.currentTimeMillis() / 1000L;

	private static String builtInAppId = "";
	private static volatile DiscordIpc ipc = null;
	private static volatile String sent = null;
	private static long nextConnectAttempt = 0;
	private static volatile String status = null;
	private static int ticks = 0;

	private DiscordPresence() {
	}

	public static void start() {
		try (InputStream in = DiscordPresence.class.getClassLoader().getResourceAsStream("threevhy-build.properties")) {
			if (in != null) {
				Properties props = new Properties();
				props.load(in);
				builtInAppId = props.getProperty("discordAppId", "").trim();
			}
		} catch (IOException ignored) {
			// no built-in ID
		}
	}

	private static String appId() {
		String fromConfig = Config.get().discordAppId == null ? "" : Config.get().discordAppId.trim();
		String id = fromConfig.isEmpty() ? builtInAppId : fromConfig;
		return id.matches("\\d{5,}") ? id : "";
	}

	/** Whether Discord is showing the presence right now. */
	public static boolean connected() {
		return Modules.DISCORD.enabled() && ipc != null && sent != null;
	}

	/** Shown in the mod menu when presence can't run. */
	public static String statusLine() {
		if (!Modules.DISCORD.enabled()) return null;
		if (appId().isEmpty()) return "Discord presence needs a Discord app ID";
		return status;
	}

	public static void tick(Minecraft mc) {
		if (++ticks % 40 != 0) return; // every 2 seconds
		String appId = appId();
		if (!Modules.DISCORD.enabled() || appId.isEmpty()) {
			WANTED.set(null);
		} else {
			WANTED.set(activity(mc).toString());
		}
		IO.execute(() -> sync(appId));
	}

	public static void stop() {
		WANTED.set(null);
		IO.execute(() -> {
			if (ipc != null) {
				try {
					ipc.setActivity(null);
				} catch (IOException ignored) {
					// closing anyway
				}
				ipc.close();
				ipc = null;
			}
		});
	}

	/** Runs on the Discord thread only. */
	private static void sync(String appId) {
		String wanted = WANTED.get();
		if (wanted == null) {
			if (ipc != null) {
				try {
					ipc.setActivity(null);
				} catch (IOException ignored) {
					// going away
				}
				ipc.close();
				ipc = null;
				sent = null;
			}
			return;
		}
		if (ipc == null) {
			long now = System.currentTimeMillis();
			if (now < nextConnectAttempt) return;
			try {
				ipc = DiscordIpc.connect(appId);
				sent = null;
				status = null;
			} catch (IOException e) {
				nextConnectAttempt = now + 15_000;
				status = "Discord isn't open";
				return;
			}
		}
		if (Objects.equals(wanted, sent)) return;
		try {
			ipc.setActivity(com.google.gson.JsonParser.parseString(wanted).getAsJsonObject());
			sent = wanted;
		} catch (IOException e) {
			ThreevhyClient.LOG.debug("Lost Discord connection", e);
			ipc.close();
			ipc = null;
			sent = null;
		}
	}

	private static JsonObject activity(Minecraft mc) {
		String details;
		if (mc.level == null) {
			details = "In the menus";
		} else if (mc.getSingleplayerServer() != null) {
			details = "Playing singleplayer";
		} else {
			ServerData server = mc.getCurrentServer();
			if (server != null && Modules.DISCORD_SERVER.get()) {
				details = "Playing on " + server.ip;
			} else {
				details = "Playing multiplayer";
			}
		}

		JsonObject activity = new JsonObject();
		activity.addProperty("type", 0);
		activity.addProperty("details", details);
		activity.addProperty("state", "Minecraft " + ThreevhyClient.minecraftVersion());

		JsonObject timestamps = new JsonObject();
		timestamps.addProperty("start", STARTED);
		activity.add("timestamps", timestamps);

		JsonObject assets = new JsonObject();
		assets.addProperty("large_image", "3vhy");
		assets.addProperty("large_text", "3vhy Client " + ThreevhyClient.version());
		activity.add("assets", assets);

		JsonArray buttons = new JsonArray();
		JsonObject download = new JsonObject();
		download.addProperty("label", "Get 3vhy Client");
		download.addProperty("url", DOWNLOAD_URL);
		buttons.add(download);
		activity.add("buttons", buttons);
		return activity;
	}
}
