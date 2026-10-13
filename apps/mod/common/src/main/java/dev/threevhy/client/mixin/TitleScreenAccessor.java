package dev.threevhy.client.mixin;

import com.mojang.realmsclient.gui.screens.RealmsNotificationsScreen;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The 3vhy main menu leaves out Minecraft's yellow splash text, and Realms' icons once 3vhy Mods takes Realms' place. */
@Mixin(TitleScreen.class)
public interface TitleScreenAccessor {
	@Accessor("splash")
	void threevhy$setSplash(SplashRenderer splash);

	@Accessor("realmsNotificationsScreen")
	RealmsNotificationsScreen threevhy$realmsNotifications();

	@Accessor("realmsNotificationsScreen")
	void threevhy$setRealmsNotifications(RealmsNotificationsScreen screen);
}
