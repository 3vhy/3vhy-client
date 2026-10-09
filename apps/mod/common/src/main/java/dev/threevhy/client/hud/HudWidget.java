package dev.threevhy.client.hud;

import dev.threevhy.client.Config;
import dev.threevhy.client.Draw;
import dev.threevhy.client.gui.HudEditorScreen;
import dev.threevhy.client.module.Module;
import net.minecraft.client.Minecraft;

/**
 * One movable piece of the HUD. Until the player drags it somewhere, it sits at its anchor,
 * stacked under the other widgets that share the anchor. A dragged position is stored as
 * 0..1 across the free space, so it survives window and GUI scale changes.
 */
public abstract class HudWidget {
	/** Where a widget sits before it's moved. */
	public enum Anchor {
		TOP_LEFT,
		TOP_RIGHT,
		MIDDLE_RIGHT,
		BOTTOM_RIGHT,
		BELOW_CROSSHAIR,
		ABOVE_CROSSHAIR
	}

	private static final int MARGIN = 4;
	private static final int GAP = 3;

	public final String id;
	public final Module module;
	public final Anchor anchor;

	protected HudWidget(String id, Module module, Anchor anchor) {
		this.id = id;
		this.module = module;
		this.anchor = anchor;
	}

	public abstract int width(Minecraft mc);

	public abstract int height(Minecraft mc);

	/** Draws at (x, y). In preview mode (HUD editor) widgets show sample values when empty. */
	public abstract void draw(Draw d, int x, int y, Minecraft mc, boolean preview);

	public boolean visible(Minecraft mc) {
		return true;
	}

	/** The position the player dragged this widget to, or null if it's at its anchor. */
	public double[] storedPosition() {
		double[] stored = Config.get().hud.get(id);
		return stored == null || stored.length < 2 ? null : stored;
	}

	public void setPosition(double x, double y) {
		Config.get().hud.put(id, new double[] {clamp(x), clamp(y)});
		Config.saveSoon();
	}

	public void resetPosition() {
		Config.get().hud.remove(id);
		Config.saveSoon();
	}

	/** Top-left corner {x, y} on a screen of the given (already scaled) size. */
	public int[] position(Minecraft mc, int screenWidth, int screenHeight) {
		double[] stored = storedPosition();
		if (stored != null) {
			return new int[] {
					(int) Math.round(stored[0] * Math.max(0, screenWidth - width(mc))),
					(int) Math.round(stored[1] * Math.max(0, screenHeight - height(mc)))
			};
		}
		int w = width(mc);
		int h = height(mc);
		return switch (anchor) {
			case TOP_LEFT, TOP_RIGHT -> {
				int y = MARGIN;
				boolean editing = mc.screen instanceof HudEditorScreen;
				for (HudWidget other : HudWidgets.ALL) {
					if (other == this) break;
					if (other.anchor == anchor && other.module.enabled() && other.storedPosition() == null
							&& (editing || other.visible(mc))) {
						y += other.height(mc) + GAP;
					}
				}
				yield new int[] {anchor == Anchor.TOP_LEFT ? MARGIN : screenWidth - w - MARGIN, y};
			}
			case MIDDLE_RIGHT -> new int[] {screenWidth - w - MARGIN, (screenHeight - h) / 2};
			case BOTTOM_RIGHT -> new int[] {screenWidth - w - MARGIN, screenHeight - h - MARGIN};
			case BELOW_CROSSHAIR -> new int[] {(screenWidth - w) / 2, screenHeight / 2 + 18};
			case ABOVE_CROSSHAIR -> new int[] {(screenWidth - w) / 2, screenHeight / 2 - 22 - h};
		};
	}

	private static double clamp(double value) {
		return Math.max(0.0, Math.min(1.0, value));
	}

	protected static void panel(Draw d, int x, int y, int w, int h) {
		d.fill(x, y, x + w, y + h, Theme.PANEL);
		d.fill(x, y, x + 2, y + h, Theme.CYAN);
	}

	protected static int textWidth(Minecraft mc, String text) {
		return mc.font.width(text);
	}
}
