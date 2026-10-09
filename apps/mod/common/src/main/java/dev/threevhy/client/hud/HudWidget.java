package dev.threevhy.client.hud;

import dev.threevhy.client.Config;
import dev.threevhy.client.Draw;
import dev.threevhy.client.module.Module;
import net.minecraft.client.Minecraft;

/** One movable piece of the HUD. Its position is stored as 0..1 across the free space. */
public abstract class HudWidget {
	public final String id;
	public final Module module;
	private final double defaultX;
	private final double defaultY;

	protected HudWidget(String id, Module module, double defaultX, double defaultY) {
		this.id = id;
		this.module = module;
		this.defaultX = defaultX;
		this.defaultY = defaultY;
	}

	public abstract int width(Minecraft mc);

	public abstract int height(Minecraft mc);

	/** Draws at (x, y). In preview mode (HUD editor) widgets show sample values when empty. */
	public abstract void draw(Draw d, int x, int y, Minecraft mc, boolean preview);

	public boolean visible(Minecraft mc) {
		return true;
	}

	public double[] position() {
		double[] stored = Config.get().hud.get(id);
		if (stored == null || stored.length < 2) {
			return new double[] {defaultX, defaultY};
		}
		return stored;
	}

	public void setPosition(double x, double y) {
		Config.get().hud.put(id, new double[] {clamp(x), clamp(y)});
		Config.saveSoon();
	}

	public void resetPosition() {
		Config.get().hud.remove(id);
		Config.saveSoon();
	}

	/** Top-left corner on a screen of the given (already scaled) size. */
	public int x(Minecraft mc, int screenWidth) {
		return (int) Math.round(position()[0] * Math.max(0, screenWidth - width(mc)));
	}

	public int y(Minecraft mc, int screenHeight) {
		return (int) Math.round(position()[1] * Math.max(0, screenHeight - height(mc)));
	}

	private static double clamp(double value) {
		return Math.max(0.0, Math.min(1.0, value));
	}

	// ---- Shared drawing helpers ----

	protected static void panel(Draw d, int x, int y, int w, int h) {
		d.fill(x, y, x + w, y + h, Theme.PANEL);
		d.fill(x, y, x + 2, y + h, Theme.CYAN);
	}

	protected static int textWidth(Minecraft mc, String text) {
		return mc.font.width(text);
	}
}
