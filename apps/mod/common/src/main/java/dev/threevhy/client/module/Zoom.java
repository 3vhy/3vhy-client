package dev.threevhy.client.module;

import dev.threevhy.client.Config;
import dev.threevhy.client.ThreevhyClient;
import net.minecraft.client.Minecraft;

/** Hold the zoom key (C) to zoom in smoothly. The FOV change happens in GameRendererMixin. */
public final class Zoom {
	private static boolean zooming = false;
	private static boolean savedSmoothCamera = false;
	private static double current = 1.0;
	private static long lastFrame = 0;

	private Zoom() {
	}

	static void tick(Minecraft mc) {
		boolean want = Modules.ZOOM.enabled() && mc.screen == null && ThreevhyClient.zoomKey != null
				&& ThreevhyClient.zoomKey.isDown();
		if (want && !zooming) {
			zooming = true;
			savedSmoothCamera = mc.options.smoothCamera;
			if (Config.get().zoomCinematic) {
				mc.options.smoothCamera = true;
			}
		} else if (!want && zooming) {
			zooming = false;
			mc.options.smoothCamera = savedSmoothCamera;
		}
	}

	/** How much to divide the field of view by this frame (1 = no zoom). Eases in and out. */
	public static double divisor() {
		long now = System.nanoTime();
		double seconds = lastFrame == 0 ? 0 : Math.min(0.1, (now - lastFrame) / 1_000_000_000.0);
		lastFrame = now;
		double target = zooming ? Math.max(1.0, Config.get().zoomFactor) : 1.0;
		double step = Math.min(1.0, seconds * 14.0);
		current += (target - current) * step;
		if (Math.abs(target - current) < 0.001) {
			current = target;
		}
		return current;
	}

	public static boolean isZooming() {
		return zooming;
	}
}
