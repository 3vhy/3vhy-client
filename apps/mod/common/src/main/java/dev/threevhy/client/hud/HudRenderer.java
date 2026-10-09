package dev.threevhy.client.hud;

import dev.threevhy.client.Config;
import dev.threevhy.client.Draw;
import dev.threevhy.client.gui.HudEditorScreen;
import net.minecraft.client.Minecraft;

/** Draws the enabled HUD widgets every frame. */
public final class HudRenderer {
	private HudRenderer() {
	}

	public static void render(Draw d) {
		Minecraft mc = Minecraft.getInstance();
		CpsTracker.poll(mc);
		if (mc.options.hideGui || mc.player == null || mc.screen instanceof HudEditorScreen) {
			return;
		}
		drawAll(d, mc, false);
	}

	/** Shared with the HUD editor, which draws the same widgets in preview mode. */
	public static void drawAll(Draw d, Minecraft mc, boolean preview) {
		float scale = scale();
		int w = Math.round(d.width() / scale);
		int h = Math.round(d.height() / scale);
		d.push();
		d.scale(scale);
		for (HudWidget widget : HudWidgets.ALL) {
			if (!widget.module.enabled() || (!preview && !widget.visible(mc))) {
				continue;
			}
			widget.draw(d, widget.x(mc, w), widget.y(mc, h), mc, preview);
		}
		d.pop();
	}

	public static float scale() {
		float scale = Config.get().hudScale;
		return scale <= 0.25f ? 1.0f : scale;
	}
}
