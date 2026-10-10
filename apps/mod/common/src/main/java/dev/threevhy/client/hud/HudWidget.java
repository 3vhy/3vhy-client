package dev.threevhy.client.hud;

import dev.threevhy.client.Config;
import dev.threevhy.client.Draw;
import dev.threevhy.client.gui.HubScreen;
import dev.threevhy.client.module.Module;
import dev.threevhy.client.module.Modules;
import dev.threevhy.client.module.Setting;
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
		ABOVE_CROSSHAIR,
		ABOVE_HOTBAR
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

	/** Draws at (x, y). In preview mode (the hub) widgets show sample values when empty. */
	public abstract void draw(Draw d, int x, int y, Minecraft mc, boolean preview);

	public boolean visible(Minecraft mc) {
		return true;
	}

	/** Whether the HUD size setting applies. The scoreboard keeps Minecraft's own size. */
	public boolean scaled() {
		return true;
	}

	/** This widget's own Size setting as a multiplier, or 1 when it has none. */
	public float sizeScale() {
		Setting.Slider size = module.sliderSetting("size");
		return size == null ? 1f : (float) (size.get() / 100.0);
	}

	/** Whether this widget has its own Size setting (scroll over it in the hub to change it). */
	public boolean resizable() {
		return module.sliderSetting("size") != null;
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
		return defaultPosition(mc, screenWidth, screenHeight);
	}

	protected int[] defaultPosition(Minecraft mc, int screenWidth, int screenHeight) {
		int w = width(mc);
		int h = height(mc);
		return switch (anchor) {
			case TOP_LEFT, TOP_RIGHT -> {
				int y = MARGIN + stackedBefore(mc);
				yield new int[] {anchor == Anchor.TOP_LEFT ? MARGIN : screenWidth - w - MARGIN, y};
			}
			case BOTTOM_RIGHT -> new int[] {screenWidth - w - MARGIN, screenHeight - h - MARGIN - stackedBefore(mc)};
			case ABOVE_HOTBAR -> new int[] {(screenWidth - w) / 2, screenHeight - 58 - h - stackedBefore(mc)};
			case MIDDLE_RIGHT -> new int[] {screenWidth - w - MARGIN, (screenHeight - h) / 2};
			case BELOW_CROSSHAIR -> new int[] {(screenWidth - w) / 2, screenHeight / 2 + 20};
			case ABOVE_CROSSHAIR -> new int[] {(screenWidth - w) / 2, screenHeight / 2 - 22 - h};
		};
	}

	/** Height taken by the earlier widgets stacked at the same anchor. */
	private int stackedBefore(Minecraft mc) {
		int total = 0;
		boolean editing = mc.screen instanceof HubScreen;
		for (HudWidget other : HudWidgets.ALL) {
			if (other == this) break;
			if (other.anchor == anchor && other.module.enabled() && other.storedPosition() == null
					&& other.usesAnchor() && (editing || other.visible(mc))) {
				total += other.height(mc) + GAP;
			}
		}
		return total;
	}

	/** False when the widget's current layout ignores its anchor (e.g. armor drawn as a row). */
	protected boolean usesAnchor() {
		return true;
	}

	private static double clamp(double value) {
		return Math.max(0.0, Math.min(1.0, value));
	}

	/** The box's opacity, 0..255, from the module's Background setting. */
	protected int backgroundAlpha() {
		Setting.Slider slider = module.sliderSetting("background");
		double percent = slider == null ? 60 : slider.get();
		return (int) Math.round(percent / 100.0 * 255);
	}

	/** Whether the box is there at all. */
	protected boolean background() {
		return backgroundAlpha() > 0;
	}

	/** Text gets a shadow once the box is too faint to read it against the world. */
	protected boolean textShadow() {
		return backgroundAlpha() < 90;
	}

	/** A key or slot fill at the widget's opacity. */
	protected int backgroundColor() {
		return (backgroundAlpha() << 24) | 0x060E2B;
	}

	protected boolean showLabel() {
		Setting.Toggle toggle = module.toggleSetting("label");
		return toggle == null || toggle.get();
	}

	protected int valueColor() {
		Setting.Color color = module.colorSetting("color");
		return color == null ? Theme.ICE : color.get();
	}

	/** Whether HUD boxes get round corners (Theme > Rounded HUD). */
	protected static boolean rounded() {
		return Modules.THEME_ROUNDED_HUD.get();
	}

	/**
	 * The box behind a widget: dark navy glass with a thin ice edge on the left. With Rounded HUD on,
	 * the corners are soft and the edge is a short line inside the box.
	 */
	protected void panel(Draw d, int x, int y, int w, int h) {
		int alpha = backgroundAlpha();
		if (alpha <= 0) return;
		int edge = (Math.min(255, alpha + 60) << 24) | 0x5BC8E6;
		if (rounded()) {
			d.roundRect(x, y, x + w, y + h, 4, backgroundColor());
			if (h > 8) d.fill(x + 1, y + 4, x + 2, y + h - 4, edge);
			return;
		}
		d.fill(x, y, x + w, y + h, backgroundColor());
		d.fill(x, y, x + 1, y + h, edge);
	}

	/** A key or slot: rounded with Rounded HUD on, square otherwise. */
	protected static void box(Draw d, int x1, int y1, int x2, int y2, int color) {
		if (rounded()) d.roundRect(x1, y1, x2, y2, 3, color);
		else d.fill(x1, y1, x2, y2, color);
	}

	protected static int textWidth(Minecraft mc, String text) {
		return mc.font.width(text);
	}
}
