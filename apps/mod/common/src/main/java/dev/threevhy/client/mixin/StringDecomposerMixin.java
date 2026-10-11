package dev.threevhy.client.mixin;

import dev.threevhy.client.module.ProtectedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.util.StringDecomposer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Name Protect: everything Minecraft draws or measures is read character by character here, so this
 * is where names get swapped. The protected text is read instead of the original, and every
 * character is reported with its position in the original string: callers use those positions to
 * cut the original, and a longer or shorter name must not push them past its end.
 */
@Mixin(StringDecomposer.class)
public abstract class StringDecomposerMixin {
	@Inject(
			method = "iterateFormatted(Ljava/lang/String;ILnet/minecraft/network/chat/Style;Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z",
			at = @At("HEAD"),
			cancellable = true,
			require = 1)
	private static void threevhy$protectFormatted(String text, int skip, Style style, Style resetStyle, FormattedCharSink sink,
			CallbackInfoReturnable<Boolean> cir) {
		ProtectedText swap = ProtectedText.forReading(text, skip);
		if (swap == null) return;
		cir.setReturnValue(StringDecomposer.iterateFormatted(swap.text, 0, style, resetStyle, swap.wrap(sink)));
	}

	@Inject(
			method = "iterate(Ljava/lang/String;Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z",
			at = @At("HEAD"),
			cancellable = true,
			require = 1)
	private static void threevhy$protectPlain(String text, Style style, FormattedCharSink sink, CallbackInfoReturnable<Boolean> cir) {
		ProtectedText swap = ProtectedText.forReading(text, 0);
		if (swap == null) return;
		cir.setReturnValue(StringDecomposer.iterate(swap.text, style, swap.wrap(sink)));
	}
}
