package dev.eventide.client.render;

import dev.eventide.client.EventideClient;
import java.lang.reflect.Method;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Whether an Iris shader pack is drawing the world. A pack replaces Minecraft's sky pass, so Eventide's
 * sky drawing steps aside while one is on. Asked through Iris's public API by reflection, so Eventide
 * doesn't need Iris to load; any problem asking counts as "no pack".
 */
public final class ShaderPacks {
	private static Object api;
	private static Method inUse;
	private static boolean looked = false;
	private static boolean broken = false;

	private ShaderPacks() {
	}

	public static boolean inUse() {
		if (broken) return false;
		if (!looked) {
			looked = true;
			if (!FabricLoader.getInstance().isModLoaded("iris")) {
				broken = true;
				return false;
			}
			try {
				Class<?> type = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
				api = type.getMethod("getInstance").invoke(null);
				inUse = type.getMethod("isShaderPackInUse");
			} catch (ReflectiveOperationException | LinkageError e) {
				EventideClient.LOG.warn("Couldn't ask Iris whether a shader pack is on; Eventide's sky features stay on", e);
				broken = true;
				return false;
			}
		}
		try {
			return (Boolean) inUse.invoke(api);
		} catch (ReflectiveOperationException | RuntimeException e) {
			broken = true;
			return false;
		}
	}
}
