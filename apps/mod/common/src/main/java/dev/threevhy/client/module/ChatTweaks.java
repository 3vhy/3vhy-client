package dev.threevhy.client.module;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Chat timestamps, stacked repeats and a longer history. Called from the chat mixin. */
public final class ChatTweaks {
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);
	private static final long STACK_WINDOW_MS = 60_000;
	private static String lastText = null;
	private static int repeats = 1;
	private static long lastAt = 0;

	private ChatTweaks() {
	}

	/**
	 * The message as it should appear. When it repeats the previous one, removePrevious drops the
	 * old line and the new one gets "(x2)", "(x3)" and so on.
	 */
	public static Component decorate(Component message, Runnable removePrevious) {
		if (!Modules.CHAT.enabled() || message == null) return message;
		String text = message.getString();
		long now = System.currentTimeMillis();
		Component result = message;
		if (Modules.CHAT_STACK.get() && !text.isBlank() && text.equals(lastText) && now - lastAt < STACK_WINDOW_MS) {
			repeats++;
			removePrevious.run();
			result = message.copy().append(Component.literal(" (x" + repeats + ")").withStyle(ChatFormatting.GRAY));
		} else {
			repeats = 1;
		}
		lastText = text;
		lastAt = now;
		if (Modules.CHAT_TIMESTAMPS.get()) {
			MutableComponent stamp = Component.literal("[" + LocalTime.now().format(TIME) + "] ")
					.withStyle(ChatFormatting.DARK_GRAY);
			result = stamp.append(result);
		}
		return result;
	}

	/** Whether {@code shown} is the previous message, so it's the one a repeat replaces. */
	public static boolean isPrevious(Component shown) {
		return lastText != null && shown != null && shown.getString().contains(lastText);
	}

	/**
	 * Takes the newest message's lines off the chat. Minecraft puts each new message's lines at the
	 * front of the list, last line first, so they run up to the first line that ends an entry.
	 * Only those lines go; re-wrapping the whole chat for every repeat froze the game for a moment
	 * with a long history.
	 */
	public static <L> void removeNewestLines(List<L> lines, Predicate<L> endsEntry) {
		if (lines.isEmpty()) return;
		lines.remove(0);
		while (!lines.isEmpty() && !endsEntry.test(lines.get(0))) lines.remove(0);
	}

	/** How many times the latest message has repeated in a row. */
	public static int repeats() {
		return repeats;
	}

	/** How many chat lines to keep. */
	public static int historyLimit(int vanilla) {
		return Modules.CHAT.enabled() && Modules.CHAT_HISTORY.get() ? Math.max(vanilla, 1000) : vanilla;
	}
}
