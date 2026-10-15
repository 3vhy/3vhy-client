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
 * The 3vhy hub (Right Shift), like Lunar's: the game stays sharp behind falling snow, the 3vhy emblem
 * and name sit just above the middle of the screen over a row of three glass buttons (Profiles, MODS,
 * Waypoints), and the HUD stays on screen to be dragged around. Hovering a widget shows a pencil (its
 * settings) and an X (turn it off). The emblem and buttons are measured off Lunar's at Minecraft's GUI
 * scale, so they grow and shrink with the HUD (the HUD size setting too).
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
	/** Just the HUD editor, without the emblem and buttons in the middle (the Panels style's Edit HUD). */
	private boolean editOnly = false;

	public HubScreen(Screen parent) {
		super(Component.literal("3vhy Client"));
		this.parent = parent;
	}

	/** Shows only the HUD widgets to drag, with no emblem or buttons in the middle. */
	public HubScreen editOnly() {
		editOnly = true;
		return this;
	}

	public boolean isEditOnly() {
		return editOnly;
	}

	@Override
	protected boolean blurBackground() {
		return false;
	}

	@Override
	protected boolean panoramaBackground() {
		return !TitleScreenStyle.drawsBackground();
	}

	@Override
	protected void drawBackdrop(Draw d) {
		drawLunarBackdrop(d, this.width, this.height, snow);
	}

	/**
	 * Behind the Lunar-style menus: October's ghosts if the main menu shows them (with no world), a
	 * light tint like Lunar's, and the snow.
	 */
	static void drawLunarBackdrop(Draw d, int w, int h, Snow snow) {
		MenuTheme t = MenuTheme.current();
		if (Minecraft.getInstance().level == null && TitleScreenStyle.drawsBackground()) October.drawBackdrop(d, w, h);
		d.fill(0, 0, w, h, t.isLight() ? t.tint() : 0x29000000);
		snow.draw(d, w, h);
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
		boolean overCentre = !editOnly && dragging == null && mouseX >= centre[0] && mouseX < centre[2]
				&& mouseY >= centre[1] && mouseY < centre[3];
		HudWidget hovered = null;
		for (HudWidget widget : HudWidgets.ALL) {
			if (!widget.shown()) continue;
			int[] box = drawWidget(ui, mc, widget, false);
			if (dragging == null && !overCentre && mouseX >= box[0] && mouseX < box[2] && mouseY >= box[1]
					&& mouseY < box[3]) {
				hovered = widget;
			}
			hits.add(new Ui.Hit(box[0], box[1], box[2], box[3], () -> startDrag(widget, mouseX, mouseY), null));
		}

		// The logo and buttons step aside while something is dragged
		if (dragging == null && !editOnly) drawCentre(ui, mc);

		// The widget being touched is drawn again on top, so it's never hidden behind the logo block
		HudWidget active = dragging != null ? dragging : hovered;
		if (active != null) {
			drawWidget(ui, mc, active, true);
			if (dragging == null) controls(ui, mc, active);
		}

		String hint = "DRAG HUD MODS TO MOVE THEM  ·  PENCIL FOR SETTINGS  ·  X TO HIDE" + (editOnly ? "  ·  ESC TO GO BACK" : "");
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

	/** Lunar's hub, in GUI pixels at 100% HUD size: its button row and its logo above it. */
	private static final float BUTTON_H = 19.3f;
	private static final float MODS_W = 66.7f;
	private static final float GAP = 2f;
	private static final float EMBLEM = 37f;
	/** From the middle of the screen up to the emblem's top, the name's capital top, and the buttons' top. */
	private static final float EMBLEM_TOP = 66.7f;
	private static final float NAME_TOP = 25f;
	private static final float NAME_CAP = 6.3f;
	private static final float BUTTONS_TOP = 9.7f;

	/** How big the centre is drawn: the HUD size setting, made smaller if the window can't fit it. */
	private float centreScale() {
		float fit = Math.min((this.height - 16) / 80f, (this.width - 16) / 112f);
		return Math.max(0.4f, Math.min(HudRenderer.scale(), fit));
	}

	/** Where the emblem, the name and the buttons go: {x1, y1, x2, y2}. */
	private int[] centreBounds() {
		float s = centreScale();
		float rowW = MODS_W + 2 * (BUTTON_H + GAP);
		float cx = this.width / 2f;
		float cy = this.height / 2f;
		return new int[] {Math.round(cx - rowW * s / 2) - 2, Math.round(cy - EMBLEM_TOP * s) - 2,
				Math.round(cx + rowW * s / 2) + 2, Math.round(cy + BUTTONS_TOP * s + BUTTON_H * s) + 2};
	}

	/** The pencil and X over a hovered widget. */
	private void controls(Ui ui, Minecraft mc, HudWidget widget) {
		int[] box = box(mc, widget);
		int y = box[1] - 12 >= 0 ? box[1] - 12 : box[3] + 2;
		iconBox(ui, box[0], y, Icons.PENCIL, false, () -> mc.setScreen(Modules.THEME_MENU_STYLE.is("Panels")
				? new ClickGuiScreen(this).expand(widget.module) : new ModMenuScreen(this).openModule(widget.module)));
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

	private void drawCentre(Ui ui, Minecraft mc) {
		float s = centreScale();
		float cx = this.width / 2f;
		float cy = this.height / 2f;
		float intro = Math.min(1f, (System.currentTimeMillis() - openedAt) / 300f);
		float drop = (1 - (1 - (1 - intro) * (1 - intro) * (1 - intro))) * -6 * s;
		int alpha = Math.round(255 * intro);
		Draw d = ui.d;

		float guiScale = (float) mc.getWindow().getGuiScale();
		int cell = Math.max(1, Math.round(EMBLEM * s * guiScale / 16));
		int size = 16 * cell;
		d.push();
		d.scale(1f / guiScale);
		drawEmblem(d, Math.round(cx * guiScale - size / 2f), Math.round((cy - EMBLEM_TOP * s + drop) * guiScale), size, cell, alpha,
				0xFFFFFFFF);
		d.pop();

		boolean clean = UiFont.clean();
		Component brand = clean ? UiFont.bold("3VHY") : Ui.bold("3VHY");
		Component client = clean ? UiFont.semibold(" CLIENT") : Component.literal(" CLIENT");
		float nameScale = NAME_CAP * s / (clean ? 6.8f : 7f);
		float brandW = mc.font.width(brand) * nameScale;
		float nameW = brandW + mc.font.width(client) * nameScale;
		float nameY = cy - NAME_TOP * s + drop;
		text(d, brand, cx - nameW / 2, nameY, nameScale, MenuTheme.alpha(0x000000, alpha * 0x60 / 255), s);
		text(d, client, cx - nameW / 2 + brandW, nameY, nameScale, MenuTheme.alpha(0x000000, alpha * 0x60 / 255), s);
		text(d, brand, cx - nameW / 2, nameY, nameScale, MenuTheme.alpha(0xFFFFFF, alpha), 0);
		text(d, client, cx - nameW / 2 + brandW, nameY, nameScale, MenuTheme.alpha(0xF0F3F8, alpha), 0);

		float y = cy - BUTTONS_TOP * s;
		float h = BUTTON_H * s;
		float rowW = (MODS_W + 2 * (BUTTON_H + GAP)) * s;
		float x = cx - rowW / 2;
		lunarButton(ui, x, y, h, h, null, PixelIcons.forModule(Modules.PROFILES),
				() -> mc.setScreen(new ModMenuScreen(this).openTab(ModMenuScreen.Tab.PROFILES)));
		if (ui.hover(Math.round(x), Math.round(y), Math.round(x + h), Math.round(y + h))) ui.tooltip("Profiles", Math.round(x + h / 2), Math.round(y + h + 4));
		x += h + GAP * s;
		lunarButton(ui, x, y, MODS_W * s, h, "MODS", null, () -> mc.setScreen(ModMenuScreen.create(this)));
		x += (MODS_W + GAP) * s;
		lunarButton(ui, x, y, h, h, null, PixelIcons.forModule(Modules.WAYPOINTS),
				() -> mc.setScreen(new ModMenuScreen(this).openTab(ModMenuScreen.Tab.WAYPOINTS)));
		if (ui.hover(Math.round(x), Math.round(y), Math.round(x + h), Math.round(y + h))) ui.tooltip("Waypoints", Math.round(x + h / 2), Math.round(y + h + 4));
	}

	/** Text at a fractional size; {@code shadow} nudges it down and right by that many GUI pixels' worth. */
	private static void text(Draw d, Component text, float x, float y, float scale, int color, float shadow) {
		d.push();
		d.translate(x + shadow * 0.5f, y + shadow * 0.5f);
		d.scale(scale);
		d.text(text, 0, 0, color, false);
		d.pop();
	}

	/**
	 * Lunar's hub button: smoked glass (a fifth black) with a thin light edge and small round corners,
	 * drawn in real pixels, and white text or a pixel icon in the middle.
	 */
	static void lunarButton(Ui ui, float x, float y, float w, float h, String label, String[] icon, Runnable action) {
		Draw d = ui.d;
		boolean hover = ui.hover(Math.round(x), Math.round(y), Math.round(x + w), Math.round(y + h));
		float radius = h * 0.11f;
		Smooth.roundRect(d, x, y, x + w, y + h, radius, hover ? 0x40FFFFFF : 0x38000000);
		Smooth.roundOutline(d, x, y, x + w, y + h, radius, h * 0.035f, hover ? 0xB0FFFFFF : 0x4DFFFFFF);
		Minecraft mc = ui.mc;
		if (icon != null) {
			float guiScale = (float) mc.getWindow().getGuiScale();
			int pixel = Math.max(1, Math.round(h / 27f * guiScale));
			int size = PixelIcons.SIZE * pixel;
			d.push();
			d.scale(1f / guiScale);
			PixelIcons.draw(d, icon, Math.round((x + w / 2) * guiScale - size / 2f), Math.round((y + h / 2) * guiScale - size / 2f), pixel,
					hover ? 0xFFFFFFFF : 0xFFE6E8EC);
			d.pop();
		} else if (label != null) {
			boolean clean = UiFont.clean();
			Component text = clean ? UiFont.semibold(label) : Component.literal(label);
			float scale = h * 0.245f / (clean ? 6.8f : 7f);
			float textW = mc.font.width(text) * scale;
			text(d, text, x + (w - textW) / 2, y + (h - (clean ? 6.8f : 7f) * scale) / 2, scale, hover ? 0xFFFFFFFF : 0xFFEFF2F6, 0);
		}
		ui.hit(Math.round(x), Math.round(y), Math.round(x + w), Math.round(y + h), action);
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
				if (!widget.shown() || !widget.resizable()) continue;
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
