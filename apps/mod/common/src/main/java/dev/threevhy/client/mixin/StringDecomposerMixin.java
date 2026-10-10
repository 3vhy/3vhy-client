package dev.threevhy.client.mixin;

import dev.threevhy.client.module.NameProtect;
import net.minecraft.util.StringDecomposer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Name Protect. Every string Minecraft measures or draws is turned into characters here, so swapping
 * names at this point covers chat, the tab list, nametags, scoreboards and other mods' HUDs, and
 * text widths always match what's drawn.
 */
@Mixin(StringDecomposer.class)
public abstract class StringDecomposerMixin {
	@ModifyVariable(
			method = {
					"iterateFormatted(Ljava/lang/String;ILnet/minecraft/network/chat/Style;Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z",
					"iterate(Ljava/lang/String;Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z"
			},
			at = @At("HEAD"),
			argsOnly = true)
	private static String threevhy$protectNames(String text) {
		return NameProtect.apply(text);
	}
}
