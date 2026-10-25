package dev.threevhy.client.render;

import dev.threevhy.client.ThreevhyClient;
import java.lang.reflect.Method;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Whether an Iris shader pack is drawing the world. A pack replaces Minecraft's sky pass, so 3vhy's
 * sky drawing steps aside while one is on. Asked through Iris's public API by reflection, so 3vhy
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
				ThreevhyClient.LOG.warn("Couldn't ask Iris whether a shader pack is on; 3vhy's sky features stay on", e);
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
