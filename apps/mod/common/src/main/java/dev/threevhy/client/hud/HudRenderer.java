package dev.threevhy.client.hud;

import dev.threevhy.client.Draw;
import dev.threevhy.client.gui.HubScreen;
import dev.threevhy.client.module.Modules;
import dev.threevhy.client.module.Waypoints;
import dev.threevhy.client.module.Zoom;
import net.minecraft.client.Minecraft;

/** Draws the waypoint markers and the enabled HUD widgets every frame. */
public final class HudRenderer {
	private HudRenderer() {
	}

	public static void render(Draw d) {
		Minecraft mc = Minecraft.getInstance();
		CpsTracker.poll(mc);
		Zoom.update(mc);
		if (mc.options.hideGui || mc.player == null) return;
		Waypoints.drawOnScreen(d, mc);
		// The hub draws the widgets itself, sharp above its blur
		if (mc.screen instanceof HubScreen) return;
		drawAll(d, mc);
	}

	private static void drawAll(Draw d, Minecraft mc) {
		for (HudWidget widget : HudWidgets.ALL) {
			if (!widget.module.enabled() || !widget.visible(mc)) continue;
			float scale = scaleFor(widget);
			int w = Math.round(d.width() / scale);
			int h = Math.round(d.height() / scale);
			int[] pos = widget.position(mc, w, h);
			d.push();
			d.scale(scale);
			widget.draw(d, pos[0], pos[1], mc, false);
			d.pop();
		}
	}

	/** How big a widget is drawn: the HUD size setting (unless it opts out) times its own Size setting. */
	public static float scaleFor(HudWidget widget) {
		return (widget.scaled() ? scale() : 1f) * widget.sizeScale();
	}

	/** The HUD size setting, as a multiplier. */
	public static float scale() {
		return (float) (Modules.THEME_HUD_SCALE.get() / 100.0);
	}
}
