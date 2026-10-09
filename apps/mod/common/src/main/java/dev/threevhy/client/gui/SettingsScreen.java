package dev.threevhy.client.gui;

import dev.threevhy.client.Config;
import dev.threevhy.client.Draw;
import dev.threevhy.client.compat.CompatScreen;
import dev.threevhy.client.discord.DiscordPresence;
import dev.threevhy.client.hud.Theme;
import dev.threevhy.client.module.Module;
import dev.threevhy.client.module.Module.Category;
import dev.threevhy.client.module.Modules;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** 3vhy Settings: every feature as a card you click to switch on or off. Opens with Right Shift. */
public final class SettingsScreen extends CompatScreen {
	private static final float[] ZOOM_STEPS = {2f, 3f, 4f, 6f, 8f};
	private static final float[] HUD_SCALES = {0.75f, 1.0f, 1.25f, 1.5f};

	private record Hit(int x1, int y1, int x2, int y2, Runnable action) {
		boolean contains(double x, double y) {
			return x >= x1 && x < x2 && y >= y1 && y < y2;
		}
	}

	/** A card: a module toggle, or a setting you click to cycle. */
	private record Entry(Category category, String name, String description, Supplier<Boolean> on,
			Supplier<String> value, Runnable click) {
	}

	private final Screen parent;
	private final List<Entry> entries = new ArrayList<>();
	private final List<Hit> hits = new ArrayList<>();
	private Category selected = null; // null = all
	private double scroll = 0;
	private int maxScroll = 0;

	public SettingsScreen(Screen parent) {
		super(Component.literal("3vhy Settings"));
		this.parent = parent;
		for (Module module : Modules.ALL) {
			entries.add(new Entry(module.category, module.name, module.description, module::enabled, null,
					module::toggle));
		}
		entries.add(new Entry(Category.HUD, "HUD size", "Make every HUD widget bigger or smaller", null,
				() -> Math.round(Config.get().hudScale * 100) + "%", () -> {
					Config.get().hudScale = next(HUD_SCALES, Config.get().hudScale);
					Config.saveSoon();
				}));
		entries.add(new Entry(Category.UTILITY, "Zoom strength", "How far zoom goes in", null,
				() -> trimZero(Config.get().zoomFactor) + "x", () -> {
					Config.get().zoomFactor = next(ZOOM_STEPS, Config.get().zoomFactor);
					Config.saveSoon();
				}));
		entries.add(new Entry(Category.UTILITY, "Smooth zoom camera", "Cinematic camera while zooming",
				() -> Config.get().zoomCinematic, null, () -> {
					Config.get().zoomCinematic = !Config.get().zoomCinematic;
					Config.saveSoon();
				}));
		entries.add(new Entry(Category.DISCORD, "Show server address", "Your server's address on Discord",
				() -> Config.get().discordShowServer, null, () -> {
					Config.get().discordShowServer = !Config.get().discordShowServer;
					Config.saveSoon();
				}));
	}

	private static float next(float[] steps, float current) {
		for (float step : steps) {
			if (step > current + 0.01f) return step;
		}
		return steps[0];
	}

	private static String trimZero(float value) {
		return value == Math.floor(value) ? Integer.toString((int) value) : Float.toString(value);
	}

	// ---------- Layout ----------

	private int panelW() {
		return Math.min(this.width - 24, 470);
	}

	private int panelH() {
		return Math.min(this.height - 24, 300);
	}

	private int panelX() {
		return (this.width - panelW()) / 2;
	}

	private int panelY() {
		return (this.height - panelH()) / 2;
	}

	@Override
	protected void draw(Draw d, int mouseX, int mouseY, float delta) {
		Minecraft mc = Minecraft.getInstance();
		hits.clear();
		int px = panelX();
		int py = panelY();
		int pw = panelW();
		int ph = panelH();

		// Panel with the cyan rim along the top, like the launcher
		d.fill(px, py, px + pw, py + ph, Theme.PANEL_SOLID);
		d.fill(px, py, px + pw, py + 2, Theme.CYAN);
		d.outline(px, py, px + pw, py + ph, 0x405BC8E6);

		Wordmark.draw(d, px + 12, py + 9, 1, Theme.ICE, 0xFF061D8C);
		d.text("CLIENT", px + 12 + Wordmark.COLUMNS + 6, py + 13, Theme.CYAN, false);
		String hint = "Right Shift opens this menu";
		d.text(hint, px + pw - 12 - mc.font.width(hint), py + 14, Theme.MUTED, false);

		// Categories
		int sx = px + 8;
		int sy = py + 36;
		int sw = 84;
		sidebarItem(d, mc, sx, sy, sw, "All", selected == null, mouseX, mouseY, () -> select(null));
		int row = 1;
		for (Category category : Category.values()) {
			Category c = category;
			sidebarItem(d, mc, sx, sy + row * 20, sw, category.label, selected == category, mouseX, mouseY,
					() -> select(c));
			row++;
		}

		// Cards
		int cx = sx + sw + 8;
		int cy = sy;
		int cw = px + pw - 8 - cx;
		int footerH = 30;
		int ch = py + ph - footerH - cy;
		int columns = cw >= 300 ? 2 : 1;
		int gap = 6;
		int cardW = (cw - gap * (columns - 1)) / columns;
		int cardH = 34;
		List<Entry> visible = new ArrayList<>();
		for (Entry entry : entries) {
			if (selected == null || entry.category() == selected) visible.add(entry);
		}
		int rows = (visible.size() + columns - 1) / columns;
		int contentH = rows * (cardH + gap) - gap;
		maxScroll = Math.max(0, contentH - ch);
		scroll = Math.max(0, Math.min(scroll, maxScroll));

		d.enableScissor(cx, cy, cx + cw, cy + ch);
		for (int i = 0; i < visible.size(); i++) {
			int col = i % columns;
			int r = i / columns;
			int x = cx + col * (cardW + gap);
			int y = cy + r * (cardH + gap) - (int) scroll;
			if (y + cardH < cy || y > cy + ch) continue;
			card(d, mc, visible.get(i), x, y, cardW, cardH, mouseX, mouseY, cy, cy + ch);
		}
		d.disableScissor();

		// Footer buttons
		int by = py + ph - footerH + 5;
		int doneW = 70;
		button(d, mc, px + pw - 8 - doneW, by, doneW, 20, "Done", true, mouseX, mouseY, this::onClose);
		int editW = 110;
		button(d, mc, px + pw - 8 - doneW - 6 - editW, by, editW, 20, "Edit HUD layout", false, mouseX, mouseY,
				() -> Minecraft.getInstance().setScreen(new HudEditorScreen(this)));
		String status = DiscordPresence.statusLine();
		if (status != null) {
			d.text(status, px + 12, by + 6, Theme.MUTED, false);
		}
	}

	private void select(Category category) {
		selected = category;
		scroll = 0;
	}

	private void sidebarItem(Draw d, Minecraft mc, int x, int y, int w, String label, boolean active, int mx,
			int my, Runnable action) {
		boolean hover = mx >= x && mx < x + w && my >= y && my < y + 18;
		if (active) {
			d.fill(x, y, x + w, y + 18, 0x405BC8E6);
			d.fill(x, y, x + 2, y + 18, Theme.CYAN);
		} else if (hover) {
			d.fill(x, y, x + w, y + 18, 0x208CC4EA);
		}
		d.text(label, x + 8, y + 5, active ? Theme.ICE : Theme.SKY, false);
		hits.add(new Hit(x, y, x + w, y + 18, action));
	}

	private void card(Draw d, Minecraft mc, Entry entry, int x, int y, int w, int h, int mx, int my, int clipTop,
			int clipBottom) {
		boolean hover = mx >= x && mx < x + w && my >= y && my < y + h && my >= clipTop && my < clipBottom;
		boolean on = entry.on() != null && entry.on().get();
		d.fill(x, y, x + w, y + h, hover ? 0xFF16307A : Theme.DEEP);
		if (on) d.fill(x, y, x + 2, y + h, Theme.CYAN);
		int controlW = 26;
		String value = entry.value() != null ? entry.value().get() : null;
		if (value != null) controlW = mc.font.width(value) + 12;
		int textMax = w - 16 - controlW;
		d.text(trim(mc, entry.name(), textMax), x + 9, y + 7, Theme.ICE, false);
		d.push();
		d.translate(x + 9, y + 20);
		d.scale(0.8f);
		d.text(trim(mc, entry.description(), Math.round(textMax / 0.8f)), 0, 0, Theme.MUTED, false);
		d.pop();

		int ctrlX = x + w - 8 - controlW;
		int ctrlY = y + (h - 12) / 2;
		if (value != null) {
			d.fill(ctrlX, ctrlY - 2, ctrlX + controlW, ctrlY + 14, 0xFF22428F);
			d.text(value, ctrlX + 6, ctrlY + 1, Theme.ICE, false);
		} else {
			// Toggle switch
			d.fill(ctrlX, ctrlY, ctrlX + controlW, ctrlY + 12, on ? Theme.CYAN : 0xFF22428F);
			int knobX = on ? ctrlX + controlW - 10 : ctrlX + 2;
			d.fill(knobX, ctrlY + 2, knobX + 8, ctrlY + 10, on ? Theme.NIGHT : Theme.SKY);
		}
		int top = Math.max(y, clipTop);
		int bottom = Math.min(y + h, clipBottom);
		if (bottom > top) hits.add(new Hit(x, top, x + w, bottom, entry.click()));
	}

	private void button(Draw d, Minecraft mc, int x, int y, int w, int h, String label, boolean primary, int mx,
			int my, Runnable action) {
		boolean hover = mx >= x && mx < x + w && my >= y && my < y + h;
		int bg = primary ? (hover ? 0xFF7FD6EF : Theme.CYAN) : (hover ? 0xFF2A4C9E : 0xFF22428F);
		d.fill(x + 1, y, x + w - 1, y + h, bg);
		d.fill(x, y + 1, x + w, y + h - 1, bg);
		d.fill(x + 1, y + h - 2, x + w - 1, y + h, 0x50000000);
		int tw = mc.font.width(label);
		d.text(label, x + (w - tw) / 2, y + (h - 9) / 2, primary ? Theme.NIGHT : Theme.ICE, false);
		hits.add(new Hit(x, y, x + w, y + h, action));
	}

	private static String trim(Minecraft mc, String text, int maxWidth) {
		if (mc.font.width(text) <= maxWidth) return text;
		String ellipsis = "...";
		String t = text;
		while (!t.isEmpty() && mc.font.width(t + ellipsis) > maxWidth) {
			t = t.substring(0, t.length() - 1);
		}
		return t + ellipsis;
	}

	// ---------- Input ----------

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == 0) {
			for (Hit hit : List.copyOf(hits)) {
				if (hit.contains(event.x(), event.y())) {
					hit.action().run();
					return true;
				}
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
		scroll = Math.max(0, Math.min(maxScroll, scroll - vertical * 20));
		return true;
	}

	@Override
	public void onClose() {
		Config.save();
		Minecraft.getInstance().setScreen(parent);
	}
}
