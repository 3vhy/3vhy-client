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
 * The 3vhy hub (Right Shift), like Lunar's: the game dims behind falling snow, the 3vhy emblem and
 * name sit in the middle over a row of glass buttons (Profiles, MODS, Waypoints), and the HUD stays
 * on screen to be dragged around. Hovering a widget shows a pencil (its settings) and an X (turn it off).
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

		String hint = "DRAG HUD MODS TO MOVE THEM  ·  PENCIL FOR SETTINGS  ·  X TO HIDE";
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

	private static final int BUTTON_H = 26;
	private static final int MODS_W = 120;
	private static final int GAP = 6;

	/** Pixels per cell of the emblem's 3. */
	private int cell() {
		return this.height >= 360 ? 4 : 3;
	}

	private int emblemSize() {
		return 16 * cell();
	}

	private int buttonsY() {
		return this.height / 2 + 14;
	}

	/** Where the emblem, the name and the buttons go: {x1, y1, x2, y2}. */
	private int[] centreBounds() {
		int rowW = MODS_W + 2 * (BUTTON_H + GAP);
		int half = Math.max(rowW, emblemSize()) / 2 + 8;
		int top = buttonsY() - 10 - 12 - 8 - emblemSize() - 6;
		int bottom = buttonsY() + BUTTON_H + 6;
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

	private void drawCentre(Ui ui, Minecraft mc, int[] bounds) {
		// A soft plate keeps the emblem and buttons readable over HUD widgets placed in the middle
		ui.d.roundRect(bounds[0], bounds[1], bounds[2], bounds[3], 10, MenuTheme.alpha(ui.t.panel(), 0xB0));
		int cx = this.width / 2;
		float intro = Math.min(1f, (System.currentTimeMillis() - openedAt) / 300f);
		int drop = Math.round((1 - (1 - (1 - intro) * (1 - intro) * (1 - intro))) * -8);
		int alpha = Math.round(255 * intro);

		int btnY = buttonsY();
		int nameY = btnY - 10 - 12;
		int size = emblemSize();
		int ex = cx - size / 2;
		int ey = nameY - 8 - size + drop;
		drawEmblem(ui.d, ex, ey, size, cell(), alpha, ui.t.text());

		ui.d.push();
		ui.d.translate(cx, nameY + drop);
		ui.d.scale(1.5f);
		String brand = "3VHY";
		String client = " CLIENT";
		int textW = ui.boldWidth(brand) + ui.width(client);
		ui.d.text(Ui.bold(brand), -textW / 2, 0, MenuTheme.alpha(ui.t.text(), alpha), false);
		ui.d.text(client, -textW / 2 + ui.boldWidth(brand), 0, MenuTheme.alpha(Draw.mix(ui.t.text(), ui.t.muted(), 0.25), alpha), false);
		ui.d.pop();

		int rowW = MODS_W + 2 * (BUTTON_H + GAP);
		int x = cx - rowW / 2;
		ui.glassButton(x, btnY, BUTTON_H, BUTTON_H, null, PixelIcons.forModule(Modules.PROFILES),
				() -> mc.setScreen(new ModMenuScreen(this).openTab(ModMenuScreen.Tab.PROFILES)));
		if (ui.hover(x, btnY, x + BUTTON_H, btnY + BUTTON_H)) ui.tooltip("Profiles", x + BUTTON_H / 2, btnY + BUTTON_H + 4);
		x += BUTTON_H + GAP;
		ui.glassButton(x, btnY, MODS_W, BUTTON_H, "Mods", null, () -> mc.setScreen(ModMenuScreen.create(this)));
		x += MODS_W + GAP;
		ui.glassButton(x, btnY, BUTTON_H, BUTTON_H, null, PixelIcons.forModule(Modules.WAYPOINTS),
				() -> mc.setScreen(new ModMenuScreen(this).openTab(ModMenuScreen.Tab.WAYPOINTS)));
		if (ui.hover(x, btnY, x + BUTTON_H, btnY + BUTTON_H)) ui.tooltip("Waypoints", x + BUTTON_H / 2, btnY + BUTTON_H + 4);
	}

	/** The 3vhy emblem: the app icon's pixel 3 inside a rounded outline, in one colour like Lunar's. */
	static void drawEmblem(Draw d, int x, int y, int size, int cell, int alpha, int color) {
		int white = MenuTheme.alpha(color, alpha);
		int shade = MenuTheme.alpha(0x000000, alpha * 0x50 / 255);
		int radius = size / 4;
		int ring = Math.max(2, cell / 2 + 1);
		d.roundRect(x, y, x + size, y + size, radius, MenuTheme.alpha(0x000000, alpha * 0x40 / 255));
		for (int i = 0; i < ring; i++) d.roundOutline(x + i, y + i, x + size - i, y + size - i, radius - i, white);
		int gx = x + (size - 10 * cell) / 2;
		int gy = y + (size - 12 * cell) / 2;
		Wordmark.drawGlyph(d, gx + Math.max(1, cell / 3), gy + Math.max(1, cell / 3), cell, shade);
		Wordmark.drawGlyph(d, gx, gy, cell, white);
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
