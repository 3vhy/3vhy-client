package dev.threevhy.client.mixin;

import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The 3vhy main menu leaves out Minecraft's yellow splash text. */
@Mixin(TitleScreen.class)
public interface TitleScreenAccessor {
	@Accessor("splash")
	void threevhy$setSplash(SplashRenderer splash);
}
