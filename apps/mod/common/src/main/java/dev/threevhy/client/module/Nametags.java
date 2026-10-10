package dev.threevhy.client.module;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

/** Your own nametag in third person. */
public final class Nametags {
	private Nametags() {
	}

	public static boolean showOwn(Entity entity) {
		Minecraft mc = Minecraft.getInstance();
		return Modules.NAMETAGS.enabled() && Modules.NAMETAGS_OWN.get() && entity == mc.player
				&& !mc.options.getCameraType().isFirstPerson() && !mc.options.hideGui;
	}
}
