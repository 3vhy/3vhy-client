package dev.threevhy.client.module;

import dev.threevhy.client.Config;
import dev.threevhy.client.Config.Waypoint;
import dev.threevhy.client.Draw;
import dev.threevhy.client.module.Setting.Color;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/** Waypoints for the current world or server, and their markers on screen. */
public final class Waypoints {
	private Waypoints() {
	}

	/** Which world's waypoints to use: the server address, or the singleplayer world's name. */
	public static String worldKey(Minecraft mc) {
		ServerData server = mc.getCurrentServer();
		if (server != null && server.ip != null) return "server:" + server.ip.toLowerCase(Locale.ROOT);
		if (mc.getSingleplayerServer() != null) {
			return "world:" + mc.getSingleplayerServer().getWorldData().getLevelName();
		}
		return null;
	}

	public static String dimension(Minecraft mc) {
		return mc.level == null ? "" : mc.level.dimension().toString();
	}

	public static List<Waypoint> current(Minecraft mc) {
		String key = worldKey(mc);
		return key == null ? List.of() : Config.get().waypoints(key);
	}

	/** Adds a waypoint where the player stands. Returns it, or null outside a world. */
	public static Waypoint addHere(Minecraft mc) {
		String key = worldKey(mc);
		LocalPlayer player = mc.player;
		if (key == null || player == null) return null;
		List<Waypoint> list = Config.get().waypoints(key);
		Waypoint waypoint = new Waypoint();
		waypoint.name = "Waypoint " + (list.size() + 1);
		waypoint.x = player.blockPosition().getX();
		waypoint.y = player.blockPosition().getY();
		waypoint.z = player.blockPosition().getZ();
		waypoint.dimension = dimension(mc);
		waypoint.color = Color.SWATCHES[(1 + list.size()) % (Color.SWATCHES.length - 1)];
		list.add(waypoint);
		Config.saveSoon();
		return waypoint;
	}

	public static void remove(Minecraft mc, Waypoint waypoint) {
		current(mc).remove(waypoint);
		Config.saveSoon();
	}

	public static double distance(Minecraft mc, Waypoint waypoint) {
		if (mc.player == null) return 0;
		Vec3 pos = mc.player.position();
		double dx = waypoint.x + 0.5 - pos.x;
		double dy = waypoint.y - pos.y;
		double dz = waypoint.z + 0.5 - pos.z;
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	/** Draws each visible waypoint in this dimension where it is on screen, with its distance. */
	public static void drawOnScreen(Draw d, Minecraft mc) {
		LocalPlayer player = mc.player;
		if (!Modules.WAYPOINTS.enabled() || player == null || mc.level == null) return;
		List<Waypoint> list = current(mc);
		if (list.isEmpty()) return;
		float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
		Vec3 eye = player.getEyePosition(partial);
		double yaw = Math.toRadians(Freelook.active() ? Freelook.yaw() : player.getViewYRot(partial));
		double pitch = Math.toRadians(Freelook.active() ? Freelook.pitch() : player.getViewXRot(partial));
		double[] forward = {-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch)};
		double[] right = {-Math.cos(yaw), 0, -Math.sin(yaw)};
		double[] up = {
				right[1] * forward[2] - right[2] * forward[1],
				right[2] * forward[0] - right[0] * forward[2],
				right[0] * forward[1] - right[1] * forward[0]
		};
		int w = d.width();
		int h = d.height();
		double focal = 1 / Math.tan(Math.toRadians(mc.options.fov().get()) / 2);
		double aspect = (double) w / h;
		String dimension = dimension(mc);

		for (Waypoint waypoint : list) {
			if (!waypoint.visible || !dimension.equals(waypoint.dimension)) continue;
			double vx = waypoint.x + 0.5 - eye.x;
			double vy = waypoint.y + 1.0 - eye.y;
			double vz = waypoint.z + 0.5 - eye.z;
			double z = vx * forward[0] + vy * forward[1] + vz * forward[2];
			if (z < 0.1) continue;
			double x = vx * right[0] + vy * right[1] + vz * right[2];
			double y = vx * up[0] + vy * up[1] + vz * up[2];
			int sx = (int) Math.round(w / 2.0 * (1 + x * focal / aspect / z));
			int sy = (int) Math.round(h / 2.0 * (1 - y * focal / z));
			if (sx < -50 || sx > w + 50 || sy < -20 || sy > h + 20) continue;

			int color = waypoint.color | 0xFF000000;
			d.fill(sx - 2, sy - 2, sx + 3, sy + 3, 0xA0000000);
			d.fill(sx - 1, sy - 1, sx + 2, sy + 2, color);
			String label = waypoint.name;
			if (Modules.WAYPOINTS_DISTANCE.get()) label += "  " + Math.round(distance(mc, waypoint)) + "m";
			int tw = mc.font.width(label);
			d.fill(sx - tw / 2 - 3, sy - 15, sx + tw / 2 + 3, sy - 4, 0x900A1640);
			d.fill(sx - tw / 2 - 3, sy - 4, sx + tw / 2 + 3, sy - 3, color);
			d.text(label, sx - tw / 2, sy - 13, 0xFFE3F3FC, false);
		}
	}
}
