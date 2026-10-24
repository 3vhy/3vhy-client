package dev.threevhy.client.gui;

import dev.threevhy.client.Draw;
import dev.threevhy.client.Hooks;
import dev.threevhy.client.mixin.AbstractSliderButtonAccessor;
import dev.threevhy.client.module.Modules;
import dev.threevhy.client.ui.Anim;
import dev.threevhy.client.ui.Ease;
import dev.threevhy.client.ui.Grad;
import dev.threevhy.client.ui.Paint;
import dev.threevhy.client.ui.Tokens;
import dev.threevhy.client.ui.Type;
import dev.threevhy.client.ui.Type.Weight;
import dev.threevhy.client.ui.Widgets;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.LockIconButton;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.network.chat.contents.TranslatableContents;

/**
 * Minecraft's buttons and sliders as 3vhy's violet glass pills (Theme > Glass buttons): a translucent
 * lavender pill with a light top edge, white Inter text, and a soft accent glow on hover. Only
 * Minecraft's own plain buttons, cycle buttons, sliders and its language and accessibility icon
 * buttons change; anything with its own artwork keeps it.
 */
public final class ButtonStyle {
	private static final Map<AbstractWidget, Anim> HOVER = new WeakHashMap<>();
	private static final Grad FILL = Grad.down(0xB8615790, 0xB8433C6C);
	private static final Grad FILL_HOVER = Grad.down(0xD8796CB0, 0xD8564C8C);
	private static final Grad EDGE = Grad.down(0x8CFFFFFF, 0x14FFFFFF);
	private static final Grad EDGE_HOVER = Grad.down(0xB3FFFFFF, 0x26FFFFFF);

	private ButtonStyle() {
	}

	/** Whether this widget is drawn as a glass pill. */
	public static boolean styles(AbstractWidget widget) {
		if (!Modules.THEME_BUTTONS.get() || !widget.visible) return false;
		if (!widget.getClass().getName().startsWith("net.minecraft.")) return false;
		if (widget instanceof AbstractSliderButton) return true;
		if (widget instanceof PlainTextButton || widget instanceof ImageButton || widget instanceof LockIconButton) return false;
		if (widget instanceof SpriteIconButton) return iconFor(widget) != null;
		return widget instanceof Button || widget instanceof CycleButton<?>;
	}

	/** The line icon for Minecraft's icon buttons 3vhy knows: language and accessibility. */
	private static String iconFor(AbstractWidget widget) {
		if (!(widget.getMessage().getContents() instanceof TranslatableContents contents)) return null;
		return switch (contents.getKey()) {
			case "options.language" -> "languages";
			case "options.accessibility" -> "accessibility";
			default -> null;
		};
	}

	public static void draw(Draw d, AbstractWidget widget) {
		Hooks.buttonsStyled = true;
		float x1 = widget.getX();
		float y1 = widget.getY();
		float x2 = x1 + widget.getWidth();
		float y2 = y1 + widget.getHeight();
		Anim anim = HOVER.computeIfAbsent(widget, w -> new Anim(0, Tokens.T_COLOR, Ease.OUT));
		boolean lit = widget.active && widget.isHoveredOrFocused() && !TitleScreenStyle.hidesHover(widget);
		anim.set(lit);
		float hover = anim.get();
		float old = Paint.opacity(Paint.opacity() * widget.getAlpha() * (widget.active ? 1 : 0.5f));
		if (widget instanceof AbstractSliderButton slider) {
			drawSlider(d, slider, x1, y1, x2, y2, hover);
		} else {
			String icon = widget instanceof SpriteIconButton ? iconFor(widget) : null;
			pill(d, x1, y1, x2, y2, hover, widget.isFocused());
			if (icon != null) {
				float size = Math.min(x2 - x1, y2 - y1) * 0.5f;
				Paint.icon(d, icon, (x1 + x2 - size) / 2f, (y1 + y2 - size) / 2f, size, 0xFFF4F0FF);
			} else {
				label(d, widget.getMessage().getString(), x1, y1, x2, y2);
			}
		}
		Paint.opacity(old);
	}

	/** The glass pill itself, also used by 3vhy's own title screen buttons. */
	public static void pill(Draw d, float x1, float y1, float x2, float y2, float hover, boolean focused) {
		float r = (y2 - y1) / 2f;
		float hair = 1f / Math.max(1, Paint.density(d));
		Paint.shadow(d, x1, y1, x2, y2, r, 0, (y2 - y1) * 0.12f, (y2 - y1) * 0.45f, -(y2 - y1) * 0.1f, 0x59000000);
		if (hover > 0.01f) Paint.shadow(d, x1, y1, x2, y2, r, 0, 0, (y2 - y1) * 0.7f, -(y2 - y1) * 0.18f, Paint.fade(Accent.glow(0.75f), hover));
		Paint.rect(d, x1, y1, x2, y2, r, FILL);
		if (hover > 0.01f) {
			float old = Paint.opacity(Paint.opacity() * hover);
			Paint.rect(d, x1, y1, x2, y2, r, FILL_HOVER);
			Paint.opacity(old);
		}
		Paint.stroke(d, x1, y1, x2, y2, r, Math.max(hair, 0.5f), hover > 0.5f ? EDGE_HOVER : EDGE);
		if (focused) Paint.stroke(d, x1 - 1, y1 - 1, x2 + 1, y2 + 1, r + 1, Math.max(hair, 0.5f), Paint.fade(Accent.lighten(Accent.get(), 0.3f), 0.9f));
	}

	private static void label(Draw d, String text, float x1, float y1, float x2, float y2) {
		float h = y2 - y1;
		float cap = Math.min(6.4f, h * 0.31f);
		String shown = Type.fit(text, x2 - x1 - h * 0.6f, cap, Weight.SEMIBOLD);
		Type.center(d, shown, (x1 + x2) / 2f, y1 + (h - cap) / 2f, cap, Weight.SEMIBOLD, 0xFFF4F0FF);
	}

	private static void drawSlider(Draw d, AbstractSliderButton slider, float x1, float y1, float x2, float y2, float hover) {
		float h = y2 - y1;
		float r = h / 2f;
		double value = ((AbstractSliderButtonAccessor) slider).threevhy$value();
		float hair = 1f / Math.max(1, Paint.density(d));
		Paint.shadow(d, x1, y1, x2, y2, r, 0, h * 0.12f, h * 0.45f, -h * 0.1f, 0x59000000);
		Paint.rect(d, x1, y1, x2, y2, r, Grad.down(0xA0332C55, 0xA0241F40));
		float inset = h * 0.16f;
		float fx = x1 + inset + (float) Math.max(0, Math.min(1, value)) * (x2 - x1 - 2 * inset - (h - 2 * inset)) + (h - 2 * inset);
		float old = Paint.opacity(Paint.opacity() * (0.55f + 0.45f * hover));
		Paint.rect(d, x1 + inset, y1 + inset, fx, y2 - inset, r - inset, Accent.bar());
		Paint.opacity(old);
		Paint.stroke(d, x1, y1, x2, y2, r, Math.max(hair, 0.5f), hover > 0.5f ? EDGE_HOVER : EDGE);
		float handleW = Math.max(1.5f, h * 0.12f);
		float hx = Math.max(x1 + inset + handleW / 2f, fx - handleW / 2f - inset * 0.5f);
		Paint.shadow(d, hx - handleW / 2f, y1 + inset, hx + handleW / 2f, y2 - inset, handleW / 2f, 0, 0, h * 0.25f, 0, Accent.glow(0.6f));
		Paint.rect(d, hx - handleW / 2f, y1 + inset * 0.6f, hx + handleW / 2f, y2 - inset * 0.6f, handleW / 2f, 0xF2FFFFFF);
		label(d, slider.getMessage().getString(), x1, y1, x2, y2);
	}
}
