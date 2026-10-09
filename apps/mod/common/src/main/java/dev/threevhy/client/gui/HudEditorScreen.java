package dev.threevhy.client.gui;

import dev.threevhy.client.Config;
import dev.threevhy.client.Draw;
import dev.threevhy.client.compat.CompatScreen;
import dev.threevhy.client.hud.HudRenderer;
import dev.threevhy.client.hud.HudWidget;
import dev.threevhy.client.hud.HudWidgets;
import dev.threevhy.client.hud.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Drag HUD widgets around. Positions save when you let go. */
public final class HudEditorScreen extends CompatScreen {
	private final Screen parent;
	private HudWidget dragging = null;
	private double grabX;
	private double grabY;

	private int resetX1, resetY1, resetX2, resetY2;
	private int doneX1, doneY1, doneX2, doneY2;

	public HudEditorScreen(Screen parent) {
		super(Component.literal("Edit HUD layout"));
		this.parent = parent;
	}

	@Override
	protected boolean dimBackground() {
		return false;
	}

	@Override
	protected void draw(Draw d, int mouseX, int mouseY, float delta) {
		Minecraft mc = Minecraft.getInstance();
		d.fill(0, 0, this.width, this.height, 0x40060E2B);

		// Faint centre lines help line things up
		d.fill(this.width / 2, 0, this.width / 2 + 1, this.height, 0x205BC8E6);
		d.fill(0, this.height / 2, this.width, this.height / 2 + 1, 0x205BC8E6);

		HudRenderer.drawAll(d, mc, true);

		float scale = HudRenderer.scale();
		int sw = Math.round(this.width / scale);
		int sh = Math.round(this.height / scale);
		double mx = mouseX / scale;
		double my = mouseY / scale;
		d.push();
		d.scale(scale);
		for (HudWidget widget : HudWidgets.ALL) {
			if (!widget.module.enabled()) continue;
			int x = widget.x(mc, sw);
			int y = widget.y(mc, sh);
			int w = widget.width(mc);
			int h = widget.height(mc);
			boolean hot = widget == dragging || (dragging == null && mx >= x && mx < x + w && my >= y && my < y + h);
			d.outline(x - 1, y - 1, x + w + 1, y + h + 1, hot ? Theme.CYAN : 0x808CC4EA);
		}
		d.pop();

		String hint = "Drag widgets to move them. Turn widgets on or off in 3vhy Settings.";
		int hw = mc.font.width(hint);
		d.fill(this.width / 2 - hw / 2 - 8, 8, this.width / 2 + hw / 2 + 8, 24, Theme.PANEL_SOLID);
		d.text(hint, this.width / 2 - hw / 2, 12, Theme.ICE, false);

		int by = this.height - 30;
		resetX1 = this.width / 2 - 96;
		resetY1 = by;
		resetX2 = resetX1 + 90;
		resetY2 = by + 20;
		doneX1 = this.width / 2 + 6;
		doneY1 = by;
		doneX2 = doneX1 + 90;
		doneY2 = by + 20;
		button(d, mc, resetX1, resetY1, resetX2, resetY2, "Reset layout", false, mouseX, mouseY);
		button(d, mc, doneX1, doneY1, doneX2, doneY2, "Done", true, mouseX, mouseY);
	}

	private void button(Draw d, Minecraft mc, int x1, int y1, int x2, int y2, String label, boolean primary, int mx,
			int my) {
		boolean hover = mx >= x1 && mx < x2 && my >= y1 && my < y2;
		int bg = primary ? (hover ? 0xFF7FD6EF : Theme.CYAN) : (hover ? 0xFF2A4C9E : 0xFF22428F);
		d.fill(x1 + 1, y1, x2 - 1, y2, bg);
		d.fill(x1, y1 + 1, x2, y2 - 1, bg);
		d.fill(x1 + 1, y2 - 2, x2 - 1, y2, 0x50000000);
		int tw = mc.font.width(label);
		d.text(label, x1 + (x2 - x1 - tw) / 2, y1 + 6, primary ? Theme.NIGHT : Theme.ICE, false);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double x = event.x();
		double y = event.y();
		if (event.button() != 0) return super.mouseClicked(event, doubleClick);
		if (x >= doneX1 && x < doneX2 && y >= doneY1 && y < doneY2) {
			onClose();
			return true;
		}
		if (x >= resetX1 && x < resetX2 && y >= resetY1 && y < resetY2) {
			for (HudWidget widget : HudWidgets.ALL) widget.resetPosition();
			return true;
		}
		Minecraft mc = Minecraft.getInstance();
		float scale = HudRenderer.scale();
		int sw = Math.round(this.width / scale);
		int sh = Math.round(this.height / scale);
		double mx = x / scale;
		double my = y / scale;
		// Topmost first
		for (int i = HudWidgets.ALL.size() - 1; i >= 0; i--) {
			HudWidget widget = HudWidgets.ALL.get(i);
			if (!widget.module.enabled()) continue;
			int wx = widget.x(mc, sw);
			int wy = widget.y(mc, sh);
			if (mx >= wx && mx < wx + widget.width(mc) && my >= wy && my < wy + widget.height(mc)) {
				dragging = widget;
				grabX = mx - wx;
				grabY = my - wy;
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (dragging == null) return super.mouseDragged(event, dragX, dragY);
		Minecraft mc = Minecraft.getInstance();
		float scale = HudRenderer.scale();
		int sw = Math.round(this.width / scale);
		int sh = Math.round(this.height / scale);
		double left = event.x() / scale - grabX;
		double top = event.y() / scale - grabY;
		double freeW = Math.max(1, sw - dragging.width(mc));
		double freeH = Math.max(1, sh - dragging.height(mc));
		dragging.setPosition(left / freeW, top / freeH);
		return true;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (dragging != null) {
			dragging = null;
			Config.saveSoon();
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public void onClose() {
		Config.save();
		Minecraft.getInstance().setScreen(parent);
	}
}
