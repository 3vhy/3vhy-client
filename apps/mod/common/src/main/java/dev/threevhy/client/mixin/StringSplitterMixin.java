package dev.threevhy.client.mixin;

import dev.threevhy.client.module.ProtectedText;
import java.util.function.BiConsumer;
import net.minecraft.client.StringSplitter;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Name Protect and line wrapping. Wrapped or cut-off text (chat, tooltips, signs, the text floating
 * above players on servers) is protected before it's measured, so lines break around the name
 * that's actually shown and a real name is never split across two lines. Methods that give back
 * positions in a string the caller keeps measure that string as it is instead, so the positions
 * fit it.
 */
@Mixin(StringSplitter.class)
public abstract class StringSplitterMixin {
	@Inject(
			method = "splitLines(Lnet/minecraft/network/chat/FormattedText;ILnet/minecraft/network/chat/Style;Ljava/util/function/BiConsumer;)V",
			at = @At("HEAD"),
			cancellable = true,
			require = 1)
	private void threevhy$wrapProtected(FormattedText text, int maxWidth, Style style, BiConsumer<FormattedText, Boolean> output,
			CallbackInfo ci) {
		if (!ProtectedText.shouldPrepare()) return;
		FormattedText shown = ProtectedText.protect(text, style);
		if (shown == null) return;
		dev.threevhy.client.Hooks.wrapProtectRan = true;
		StringSplitter splitter = (StringSplitter) (Object) this;
		ProtectedText.paused(() -> {
			splitter.splitLines(shown, maxWidth, style, (line, newLine) -> ProtectedText.resumed(() -> output.accept(line, newLine)));
			return null;
		});
		ci.cancel();
	}

	@Inject(
			method = "headByWidth(Lnet/minecraft/network/chat/FormattedText;ILnet/minecraft/network/chat/Style;)Lnet/minecraft/network/chat/FormattedText;",
			at = @At("HEAD"),
			cancellable = true,
			require = 1)
	private void threevhy$cutProtected(FormattedText text, int maxWidth, Style style, CallbackInfoReturnable<FormattedText> cir) {
		if (!ProtectedText.shouldPrepare()) return;
		FormattedText shown = ProtectedText.protect(text, style);
		if (shown == null) return;
		StringSplitter splitter = (StringSplitter) (Object) this;
		cir.setReturnValue(ProtectedText.paused(() -> splitter.headByWidth(shown, maxWidth, style)));
	}

	@Inject(
			method = "findLineBreak(Ljava/lang/String;ILnet/minecraft/network/chat/Style;)I",
			at = @At("HEAD"),
			cancellable = true,
			require = 1)
	private void threevhy$lineBreakAsWritten(String text, int maxWidth, Style style, CallbackInfoReturnable<Integer> cir) {
		if (!ProtectedText.shouldPrepare()) return;
		StringSplitter splitter = (StringSplitter) (Object) this;
		cir.setReturnValue(ProtectedText.paused(() -> splitter.findLineBreak(text, maxWidth, style)));
	}

	@Inject(
			method = "splitLines(Ljava/lang/String;ILnet/minecraft/network/chat/Style;ZLnet/minecraft/client/StringSplitter$LinePosConsumer;)V",
			at = @At("HEAD"),
			cancellable = true,
			require = 1)
	private void threevhy$linesAsWritten(String text, int maxWidth, Style style, boolean withNewLines, StringSplitter.LinePosConsumer output,
			CallbackInfo ci) {
		if (!ProtectedText.shouldPrepare()) return;
		StringSplitter splitter = (StringSplitter) (Object) this;
		ProtectedText.paused(() -> {
			splitter.splitLines(text, maxWidth, style, withNewLines, output);
			return null;
		});
		ci.cancel();
	}
}
