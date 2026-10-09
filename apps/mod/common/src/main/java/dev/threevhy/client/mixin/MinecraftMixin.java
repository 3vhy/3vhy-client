package dev.threevhy.client.mixin;

import dev.threevhy.client.WindowBranding;
import dev.threevhy.client.gui.MainMenuScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
	/** Whenever Minecraft updates the window title, put "3vhy Client" back. */
	@Inject(method = "updateTitle", at = @At("RETURN"))
	private void threevhy$keepTitle(CallbackInfo ci) {
		dev.threevhy.client.Hooks.titleHookRan = true;
		WindowBranding.apply();
	}

	/** Opens the 3vhy main menu wherever Minecraft would open its own title screen. */
	@ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
	private Screen threevhy$mainMenu(Screen screen) {
		return MainMenuScreen.replace(screen);
	}
}
