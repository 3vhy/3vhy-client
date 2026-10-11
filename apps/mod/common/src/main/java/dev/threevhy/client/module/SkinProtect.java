package dev.threevhy.client.module;

import com.mojang.authlib.GameProfile;
import dev.threevhy.client.Hooks;
import dev.threevhy.client.compat.Compat;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.world.entity.player.PlayerSkin;

/**
 * Skin Protect: your skin, and optionally everyone's, shows as something else: Minecraft's default
 * Steve, Alex and friends, another player's skin, or a skin file (see CustomSkins).
 */
public final class SkinProtect {
	private SkinProtect() {
	}

	/** The default skin to show for this player, or null to keep their real one. */
	public static PlayerSkin replacement(GameProfile profile) {
		if (profile == null || !Modules.SKIN_PROTECT.enabled()) return null;
		UUID id = Compat.profileId(profile);
		if (id == null) return null;
		boolean own = id.equals(ownId());
		if (own ? !Modules.SKIN_PROTECT_OWN.get() : !Modules.SKIN_PROTECT_OTHERS.get()) return null;
		Hooks.skinProtectRan = true;
		PlayerSkin custom = CustomSkins.current();
		return custom != null ? custom : DefaultPlayerSkin.get(id);
	}

	private static UUID ownId() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) return mc.player.getUUID();
		return Compat.profileId(mc.getGameProfile());
	}
}
