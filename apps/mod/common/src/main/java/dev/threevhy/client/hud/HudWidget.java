package dev.threevhy.client.hud;

import dev.threevhy.client.Config;
import dev.threevhy.client.Draw;
import dev.threevhy.client.gui.Accent;
import dev.threevhy.client.gui.HubScreen;
import dev.threevhy.client.gui.Smooth;
import dev.threevhy.client.gui.UiFont;
import dev.threevhy.client.mixin.BossHealthOverlayAccessor;
import dev.threevhy.client.module.Module;
import dev.threevhy.client.module.Modules;
import dev.threevhy.client.module.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

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
		TOP_CENTER,
		MIDDLE_RIGHT,
		BOTTOM_RIGHT,
		BELOW_CROSSHAIR,
		ABOVE_CROSSHAIR,
		ABOVE_HOTBAR
	}

	private static final int MARGIN = 4;
	private static final int GAP = 3;
	private static final float PILL_TEXT = 0.85f;
	/** The pills' capital height; every other pill size is a share of it, measured off 67's. */
	protected static final float PILL_CAP = 6.8f * PILL_TEXT;
	protected static final int PILL_H = Math.round(2.6f * PILL_CAP);
	private static final float PILL_LABEL_GAP = 0.8f * PILL_CAP;
	private static final int PILL_FILL = 0x1F2231;

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

	/** Whether the widget is switched on: its mod is on (and, for some, one of the mod's settings). */
	public boolean shown() {
		return module.enabled();
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
			case TOP_CENTER -> new int[] {(screenWidth - w) / 2, bossBarsBottom(mc) + MARGIN + stackedBefore(mc)};
			case BOTTOM_RIGHT -> new int[] {screenWidth - w - MARGIN, screenHeight - h - MARGIN - stackedBefore(mc)};
			case ABOVE_HOTBAR -> new int[] {(screenWidth - w) / 2, screenHeight - 58 - h - stackedBefore(mc)};
			case MIDDLE_RIGHT -> new int[] {screenWidth - w - MARGIN, (screenHeight - h) / 2};
			case BELOW_CROSSHAIR -> new int[] {(screenWidth - w) / 2, screenHeight / 2 + 20};
			case ABOVE_CROSSHAIR -> new int[] {(screenWidth - w) / 2, screenHeight / 2 - 22 - h};
		};
	}

	/** Where the server's boss bars end at the top of the screen (0 when there are none). */
	private static int bossBarsBottom(Minecraft mc) {
		try {
			int bars = ((BossHealthOverlayAccessor) mc.gui.getBossOverlay()).threevhy$events().size();
			return bars == 0 ? 0 : 12 + (bars - 1) * 19 + 5;
		} catch (RuntimeException | LinkageError e) {
			return 0;
		}
	}

	/** Height taken by the earlier widgets stacked at the same anchor. */
	private int stackedBefore(Minecraft mc) {
		int total = 0;
		boolean editing = mc.screen instanceof HubScreen;
		for (HudWidget other : HudWidgets.ALL) {
			if (other == this) break;
			if (other.anchor == anchor && other.shown() && other.storedPosition() == null
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

	/** Bold pill text: the clean font, or Minecraft's when Theme's Menu font is set to it. */
	protected static Component boldText(String text) {
		return UiFont.clean() ? UiFont.bold(text) : Component.literal(text).withStyle(Style.EMPTY.withBold(true));
	}

	/** How wide {@code text} is in a pill. */
	protected static float pillTextWidth(Minecraft mc, String text) {
		return mc.font.width(boldText(text)) * PILL_TEXT;
	}

	/** A pill's width with a label column {@code labelW} wide (0 for none) and the given value. */
	protected static int pillWidth(Minecraft mc, float labelW, String value) {
		return Math.round(2 * PILL_CAP + (labelW > 0 ? labelW + PILL_LABEL_GAP : 0) + pillTextWidth(mc, value));
	}

	/** The dark box behind pills: fully round ends (or square with Rounded HUD off). */
	protected void pillBox(Draw d, int x, int y, int w, int h) {
		int alpha = backgroundAlpha();
		if (alpha <= 0) return;
		int fill = (alpha << 24) | PILL_FILL;
		if (rounded()) Smooth.roundRect(d, x, y, x + w, y + h, Math.min(h, PILL_H) / 2f, fill);
		else d.fill(x, y, x + w, y + h, fill);
	}

	/**
	 * One line of a stat like 67's: the label in the theme colour, then the value, both bold, with the
	 * label column {@code labelW} wide. {@code y} is the line's top in the pill.
	 */
	protected void pillLine(Draw d, Minecraft mc, int x, float y, float labelW, String label, String value, int valueColor) {
		boolean shadow = textShadow();
		float tx = x + PILL_CAP;
		float ty = y + (PILL_H - PILL_CAP) / 2f;
		if (labelW > 0) {
			pillText(d, label, tx, ty, Accent.get(), shadow);
			tx += labelW + PILL_LABEL_GAP;
		}
		pillText(d, value, tx, ty, valueColor, shadow);
	}

	private static void pillText(Draw d, String text, float x, float y, int color, boolean shadow) {
		d.push();
		d.translate(x, y);
		d.scale(PILL_TEXT);
		d.text(boldText(text), 0, 0, color, shadow);
		d.pop();
	}
}
