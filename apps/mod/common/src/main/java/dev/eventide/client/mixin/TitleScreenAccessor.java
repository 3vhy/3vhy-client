package dev.eventide.client.mixin;

import com.mojang.realmsclient.gui.screens.RealmsNotificationsScreen;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The Eventide main menu leaves out Minecraft's yellow splash text, and Realms' icons (they'd sit beside the old spot of the Realms button). */
@Mixin(TitleScreen.class)
public interface TitleScreenAccessor {
	@Accessor("splash")
	void eventide$setSplash(SplashRenderer splash);

	@Accessor("realmsNotificationsScreen")
	RealmsNotificationsScreen eventide$realmsNotifications();

	@Accessor("realmsNotificationsScreen")
	void eventide$setRealmsNotifications(RealmsNotificationsScreen screen);
}
