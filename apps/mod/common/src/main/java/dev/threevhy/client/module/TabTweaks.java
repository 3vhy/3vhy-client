package dev.threevhy.client.module;

import dev.threevhy.client.Draw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;

/** Tab list changes: ping as a number, and hiding the server's header and footer. */
public final class TabTweaks {
	private TabTweaks() {
	}

	/** Draws the ping as a number where the bars would go. Returns false to keep the bars. */
	public static boolean drawPing(Draw d, int width, int x, int y, PlayerInfo info) {
		if (!Modules.TAB_EDITOR.enabled() || !Modules.TAB_PING_NUMBERS.get()) return false;
		int latency = info.getLatency();
		String text = latency + "ms";
		int color = latency < 0 ? 0xFF93AEDA
				: latency < 50 ? 0xFF4ADE80 : latency < 100 ? 0xFFFFD166 : latency < 150 ? 0xFFFF9F43 : 0xFFFF6B81;
		float scale = 0.75f;
		float textWidth = Minecraft.getInstance().font.width(text) * scale;
		d.push();
		d.translate(x + width - 1 - textWidth, y + 1.5f);
		d.scale(scale);
		d.text(text, 0, 0, color, true);
		d.pop();
		return true;
	}

	public static boolean hideHeaderAndFooter() {
		return Modules.TAB_EDITOR.enabled() && Modules.TAB_HIDE_HEADER.get();
	}
}
