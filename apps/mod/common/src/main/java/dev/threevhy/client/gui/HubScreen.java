package dev.threevhy.client.gui;

import dev.threevhy.client.Config;
import dev.threevhy.client.Draw;
import dev.threevhy.client.ThreevhyClient;
import dev.threevhy.client.compat.CompatScreen;
import dev.threevhy.client.hud.HudRenderer;
import dev.threevhy.client.hud.HudWidget;
import dev.threevhy.client.hud.HudWidgets;
import dev.threevhy.client.module.Modules;
import dev.threevhy.client.module.Setting;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/**
 * The 3vhy hub (Right Shift), like NoRisk's: the game blurs behind falling snow, the 3vhy logo and
 * a MOD MENU button sit in the middle, and the HUD stays on screen to be dragged around. Hovering
 * a widget shows a pencil (its settings) and an X (turn it off).
 */
public final class HubScreen extends CompatScreen {
	private static final int SNAP = 4;
	private static final int MARGIN = 4;

	private final Screen parent;
	private final List<Ui.Hit> hits = Ui.newHits();
	private final Snow snow = new Snow();
	private final long openedAt = System.currentTimeMillis();

	private HudWidget dragging = null;
	private double grabX;
	private double grabY;
	private int guideX = -1;
	private int guideY = -1;

	public HubScreen(Screen parent) {
		super(Component.literal("3vhy Client"));
		this.parent = parent;
	}

	@Override
	protected void drawBackdrop(Draw d) {
		MenuTheme t = MenuTheme.current();
		if (Minecraft.getInstance().level == null) MainMenuScreen.drawBackdrop(d, this.width, this.height);
		d.fill(0, 0, this.width, this.height, t.tint());
		snow.draw(d, this.width, this.height);
	}

	@Override
	protected void draw(Draw d, int mouseX, int mouseY, float delta) {
		hits.clear();
		Minecraft mc = Minecraft.getInstance();
		Ui ui = new Ui(d, mouseX, mouseY, hits);
		MenuTheme t = ui.t;

		// Centre lines while dragging
		if (dragging != null) {
			if (guideX >= 0) d.fill(guideX, 0, guideX + 1, this.height, MenuTheme.alpha(t.accent(), 0xA0));
			if (guideY >= 0) d.fill(0, guideY, this.width, guideY + 1, MenuTheme.alpha(t.accent(), 0xA0));
		}

		int[] centre = centreBounds();
		boolean overCentre = dragging == null && mouseX >= centre[0] && mouseX < centre[2]
				&& mouseY >= centre[1] && mouseY < centre[3];
		HudWidget hovered = null;
		for (HudWidget widget : HudWidgets.ALL) {
			if (!widget.module.enabled()) continue;
			int[] box = drawWidget(ui, mc, widget, false);
			if (dragging == null && !overCentre && mouseX >= box[0] && mouseX < box[2] && mouseY >= box[1]
					&& mouseY < box[3]) {
				hovered = widget;
			}
			hits.add(new Ui.Hit(box[0], box[1], box[2], box[3], () -> startDrag(widget, mouseX, mouseY), null));
		}

		// The logo block sits on its own glass plate, and steps aside while something is dragged
		if (dragging == null) drawCentre(ui, mc, centre);

		// The widget being touched is drawn again on top, so it's never hidden behind the logo block
		HudWidget active = dragging != null ? dragging : hovered;
		if (active != null) {
			drawWidget(ui, mc, active, true);
			if (dragging == null) controls(ui, mc, active);
		}

		String hint = "DRAG TO MOVE  ·  PENCIL FOR SETTINGS  ·  X TO HIDE";
		if (active != null && active.resizable()) {
			hint += "  ·  SCROLL TO RESIZE (" + Math.round(active.sizeScale() * 100) + "%)";
		}
		float scale = 0.6f;
		ui.small(hint, this.width / 2f - ui.width(hint) * scale / 2, 4, scale, MenuTheme.alpha(t.text(), 0xB0));
	}

	/** Draws one widget in its spot with its outline; returns its box. */
	private int[] drawWidget(Ui ui, Minecraft mc, HudWidget widget, boolean highlighted) {
		float scale = widgetScale(widget);
		int[] box = box(mc, widget);
		if (highlighted) ui.d.roundRect(box[0] - 2, box[1] - 2, box[2] + 2, box[3] + 2, 4, MenuTheme.alpha(ui.t.panel(), 0x60));
		ui.d.push();
		ui.d.scale(scale);
		int[] pos = widget.position(mc, Math.round(this.width / scale), Math.round(this.height / scale));
		widget.draw(ui.d, pos[0], pos[1], mc, true);
		ui.d.pop();
		ui.d.roundOutline(box[0] - 1, box[1] - 1, box[2] + 1, box[3] + 1, 3,
				highlighted ? ui.t.accent() : MenuTheme.alpha(ui.t.edge(), 0x90));
		return box;
	}

	/** Where the logo, MOD MENU button and quick buttons go: {x1, y1, x2, y2}. */
	private int[] centreBounds() {
		int cell = this.height >= 420 ? 3 : 2;
		int btnY = this.height / 2 - 10;
		int top = btnY - 14 - 4 - 15 * cell - 8;
		int bottom = btnY + 20 + 6 + 24 + 8;
		int half = 88;
		return new int[] {this.width / 2 - half, top, this.width / 2 + half, bottom};
	}

	/** The pencil and X over a hovered widget. */
	private void controls(Ui ui, Minecraft mc, HudWidget widget) {
		int[] box = box(mc, widget);
		int y = box[1] - 12 >= 0 ? box[1] - 12 : box[3] + 2;
		iconBox(ui, box[0], y, Icons.PENCIL, false, () -> mc.setScreen(new ModMenuScreen(this).openModule(widget.module)));
		iconBox(ui, box[2] - 10, y, Icons.CLOSE, true, () -> widget.module.set(false));
	}

	private void iconBox(Ui ui, int x, int y, String[] icon, boolean danger, Runnable action) {
		MenuTheme t = ui.t;
		boolean hover = ui.hover(x, y, x + 10, y + 10);
		int fill = danger ? (hover ? t.bad() : MenuTheme.alpha(t.bad(), 0xB0)) : (hover ? t.accent() : MenuTheme.alpha(t.panel(), 0xF0));
		ui.d.roundRect(x, y, x + 10, y + 10, 5, fill);
		ui.d.roundOutline(x, y, x + 10, y + 10, 5, danger ? t.bad() : t.edgeStrong());
		Icons.draw(ui.d, icon, x + 2, y + 2, 0xFFFFFFFF);
		ui.hit(x, y, x + 10, y + 10, action);
	}

	private void drawCentre(Ui ui, Minecraft mc, int[] plate) {
		MenuTheme t = ui.t;
		ui.panel(plate[0], plate[1], plate[2], plate[3], MenuTheme.alpha(t.panel(), 0xD8));
		int cx = this.width / 2;
		int btnW = 150;
		int btnH = 20;
		int btnY = this.height / 2 - btnH / 2;
		int cell = this.height >= 420 ? 3 : 2;
		float intro = Math.min(1f, (System.currentTimeMillis() - openedAt) / 300f);
		int drop = Math.round((1 - (1 - (1 - intro) * (1 - intro) * (1 - intro))) * -8);

		int clientY = btnY - 14;
		int wordY = clientY - 4 - 15 * cell + drop;
		Wordmark.draw(ui.d, cx - 43 * cell / 2, wordY, cell, 0xFFE3F3FC, 0xFF061D8C);
		String client = "C L I E N T";
		ui.text(client, cx - ui.width(client) / 2, clientY, t.accent());

		ui.button(cx - btnW / 2, btnY, btnW, btnH, "Mod Menu", false, () -> mc.setScreen(new ModMenuScreen(this)));

		record Quick(String label, String[] icon, Runnable action) {
		}
		List<Quick> quick = List.of(
				new Quick("Profiles", PixelIcons.forModule(Modules.PROFILES),
						() -> mc.setScreen(new ModMenuScreen(this).openTab(ModMenuScreen.Tab.PROFILES))),
				new Quick("Waypoints", PixelIcons.forModule(Modules.WAYPOINTS),
						() -> mc.setScreen(new ModMenuScreen(this).openTab(ModMenuScreen.Tab.WAYPOINTS))),
				new Quick("Crosshair", PixelIcons.forModule(Modules.CROSSHAIR),
						() -> mc.setScreen(new ModMenuScreen(this).openModule(Modules.CROSSHAIR))),
				new Quick("Theme", PixelIcons.forModule(Modules.THEME),
						() -> mc.setScreen(new ModMenuScreen(this).openModule(Modules.THEME))),
				new Quick("Discord", PixelIcons.forModule(Modules.DISCORD),
						() -> mc.setScreen(new ModMenuScreen(this).openModule(Modules.DISCORD))));
		int size = 24;
		int gap = 5;
		int rowW = quick.size() * size + (quick.size() - 1) * gap;
		int x = cx - rowW / 2;
		int y = btnY + btnH + 6;
		for (Quick each : quick) {
			ui.iconButton(x, y, size, each.icon(), each.label(), false, each.action());
			x += size + gap;
		}
	}

	private float widgetScale(HudWidget widget) {
		return HudRenderer.scaleFor(widget);
	}

	/** A widget's box on screen (GUI pixels): {x1, y1, x2, y2}. */
	private int[] box(Minecraft mc, HudWidget widget) {
		float scale = widgetScale(widget);
		int[] pos = widget.position(mc, Math.round(this.width / scale), Math.round(this.height / scale));
		return new int[] {
				Math.round(pos[0] * scale), Math.round(pos[1] * scale),
				Math.round((pos[0] + widget.width(mc)) * scale), Math.round((pos[1] + widget.height(mc)) * scale)
		};
	}

	private void startDrag(HudWidget widget, double mouseX, double mouseY) {
		Minecraft mc = Minecraft.getInstance();
		float scale = widgetScale(widget);
		int[] pos = widget.position(mc, Math.round(this.width / scale), Math.round(this.height / scale));
		dragging = widget;
		grabX = mouseX / scale - pos[0];
		grabY = mouseY / scale - pos[1];
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == 0) {
			for (int i = hits.size() - 1; i >= 0; i--) {
				Ui.Hit hit = hits.get(i);
				if (!hit.contains(event.x(), event.y()) || hit.action() == null) continue;
				hit.action().run();
				// Grabbing a widget is silent; buttons click
				if (dragging == null) {
					Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
				}
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (dragging == null) return super.mouseDragged(event, dragX, dragY);
		Minecraft mc = Minecraft.getInstance();
		float scale = widgetScale(dragging);
		int sw = Math.round(this.width / scale);
		int sh = Math.round(this.height / scale);
		int w = dragging.width(mc);
		int h = dragging.height(mc);
		double left = event.x() / scale - grabX;
		double top = event.y() / scale - grabY;
		guideX = -1;
		guideY = -1;
		if (Math.abs(left + w / 2.0 - sw / 2.0) < SNAP) {
			left = sw / 2.0 - w / 2.0;
			guideX = this.width / 2;
		} else if (Math.abs(left - MARGIN) < SNAP) {
			left = MARGIN;
		} else if (Math.abs(left + w - (sw - MARGIN)) < SNAP) {
			left = sw - MARGIN - w;
		}
		if (Math.abs(top + h / 2.0 - sh / 2.0) < SNAP) {
			top = sh / 2.0 - h / 2.0;
			guideY = this.height / 2;
		} else if (Math.abs(top - MARGIN) < SNAP) {
			top = MARGIN;
		} else if (Math.abs(top + h - (sh - MARGIN)) < SNAP) {
			top = sh - MARGIN - h;
		}
		double freeW = Math.max(1, sw - w);
		double freeH = Math.max(1, sh - h);
		dragging.setPosition(left / freeW, top / freeH);
		return true;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (dragging != null) {
			dragging = null;
			guideX = -1;
			guideY = -1;
			Config.saveSoon();
			return true;
		}
		return super.mouseReleased(event);
	}

	/** Scrolling over a widget that has its own Size setting makes it bigger or smaller. */
	@Override
	public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
		if (dragging == null && vertical != 0) {
			Minecraft mc = Minecraft.getInstance();
			for (int i = HudWidgets.ALL.size() - 1; i >= 0; i--) {
				HudWidget widget = HudWidgets.ALL.get(i);
				if (!widget.module.enabled() || !widget.resizable()) continue;
				int[] box = box(mc, widget);
				if (x < box[0] || x >= box[2] || y < box[1] || y >= box[3]) continue;
				Setting.Slider size = widget.module.sliderSetting("size");
				size.set(size.get() + Math.signum(vertical) * size.step);
				return true;
			}
		}
		return super.mouseScrolled(x, y, horizontal, vertical);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (ThreevhyClient.openSettingsKey != null && ThreevhyClient.openSettingsKey.matches(event)) {
			Config.save();
			Minecraft.getInstance().setScreen(null);
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void onClose() {
		Config.save();
		Minecraft.getInstance().setScreen(parent);
	}
}
