package dev.threevhy.client.gui.menu;

import dev.threevhy.client.Draw;
import dev.threevhy.client.module.Module;
import dev.threevhy.client.module.Setting;
import dev.threevhy.client.ui.Anim;
import dev.threevhy.client.ui.Ease;
import dev.threevhy.client.ui.Hits;
import dev.threevhy.client.ui.Keys;
import dev.threevhy.client.ui.Paint;
import dev.threevhy.client.ui.Tokens;
import dev.threevhy.client.ui.Type;
import dev.threevhy.client.ui.Type.Weight;
import dev.threevhy.client.ui.Widgets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A module's settings, on a glass sheet that slides in from the right over the menu: its tile, name and
 * switch at the top, then a row for each setting (switches, sliders, segmented pickers, text fields,
 * colour swatches) and whatever else the module needs, like the Player List's names ({@link Extras}).
 */
public final class SettingsSheet {
	/** Settings that aren't plain {@link Setting}s: lists, files, status lines and buttons. */
	public interface Extras {
		/** Whether this module has any. */
		boolean has(Module module);

		/** How tall they are, {@code width} wide. */
		float height(Module module, float width);

		void draw(Draw d, Module module, float x, float y, float width, float mouseX, float mouseY, Hits hits, long nowMs);

		boolean keyPressed(int key);

		boolean charTyped(String typed);

		boolean typing();

		void stopTyping();
	}

	private static final float WIDTH = 460;
	private static final float PAD = 26;
	private static final float HEAD = 118;

	private final Extras extras;
	private final Runnable clickSound;
	private final Hits hits = new Hits();
	private final Anim open = new Anim(0, Tokens.T_SHEET, Ease.OUT);
	private final Map<Setting, Anim[]> toggles = new HashMap<>();
	private final Map<Setting, Anim> segments = new HashMap<>();
	private final Anim headKnob = new Anim(0, Tokens.T_SWITCH_KNOB, Ease.SPRING);
	private final Anim headLit = new Anim(0, Tokens.T_SWITCH_FILL, Ease.OUT);
	private Module module;
	private Module closing;
	private double scroll = 0;
	private double shownScroll = 0;
	private double maxScroll = 0;
	private Setting.Slider dragging = null;
	private float dragX;
	private float dragW;
	private Setting.Text editing = null;
	private float sheetX;
	private float sheetY1;
	private float sheetY2;

	public SettingsSheet(Extras extras, Runnable clickSound) {
		this.extras = extras;
		this.clickSound = clickSound;
	}

	public Extras extras() {
		return extras;
	}

	/** Whether a module has anything to show on a sheet. */
	public static boolean hasPage(Module module, Extras extras) {
		return !module.settings().isEmpty() || !module.toggleable() || extras != null && extras.has(module);
	}

	public void open(Module next) {
		if (next != module) {
			scroll = 0;
			shownScroll = 0;
		}
		module = next;
		closing = null;
		headKnob.snap(next.enabled() ? 1 : 0);
		headLit.snap(next.enabled() ? 1 : 0);
		open.set(1);
	}

	public void close() {
		if (module == null) return;
		closing = module;
		module = null;
		editing = null;
		dragging = null;
		if (extras != null) extras.stopTyping();
		open.set(0);
	}

	public boolean isOpen() {
		return module != null;
	}

	public Module module() {
		return module;
	}

	public boolean typing() {
		return editing != null || extras != null && module != null && extras.typing();
	}

	public void draw(Draw d, float canvasW, float canvasH, float mx, float my, long now) {
		float t = open.get();
		Module shown = module != null ? module : closing;
		if (shown == null || t <= 0.001f) {
			if (module == null) closing = null;
			return;
		}
		hits.clear();
		float old = Paint.opacity(Paint.opacity() * t);
		Paint.rect(d, 0, 0, canvasW, canvasH, 0, 0x73030208);
		float right = (canvasW - MenuView.CONTENT_W) / 2f + MenuView.CONTENT_W;
		float x = right - WIDTH + (1 - t) * 48;
		float y1 = 96;
		float y2 = canvasH - 56;
		sheetX = x;
		sheetY1 = y1;
		sheetY2 = y2;
		Widgets.glass(d, x, y1, x + WIDTH, y2, Tokens.R_PANEL);
		drawHead(d, shown, x, y1, mx, my);

		float listTop = y1 + HEAD;
		float listBottom = y2 - 14;
		float inner = WIDTH - 2 * PAD;
		float contentH = 0;
		for (Setting setting : shown.settings()) contentH += rowHeight(setting, inner);
		float extra = extras != null && extras.has(shown) ? extras.height(shown, inner) : 0;
		contentH += extra + 10;
		maxScroll = Math.max(0, contentH - (listBottom - listTop));
		scroll = Math.max(0, Math.min(scroll, maxScroll));
		shownScroll += (scroll - shownScroll) * 0.35;
		if (Math.abs(scroll - shownScroll) < 0.3) shownScroll = scroll;

		d.enableScissor(Math.round(x), Math.round(listTop), Math.round(x + WIDTH), Math.round(listBottom));
		hits.clip(x, listTop, x + WIDTH, listBottom);
		float y = listTop - (float) shownScroll;
		boolean first = true;
		for (Setting setting : shown.settings()) {
			float h = rowHeight(setting, inner);
			if (y + h > listTop - 2 && y < listBottom + 2) {
				if (!first) Paint.rect(d, x + PAD, y, x + WIDTH - PAD, y + 1, 0, 0x0FFFFFFF);
				drawRow(d, setting, x + PAD, y, inner, mx, my, now);
			}
			first = false;
			y += h;
		}
		if (extra > 0) {
			if (!first) Paint.rect(d, x + PAD, y, x + WIDTH - PAD, y + 1, 0, 0x0FFFFFFF);
			extras.draw(d, shown, x + PAD, y + 12, inner, mx, my, hits, now);
		}
		hits.unclip();
		d.disableScissor();
		if (maxScroll > 0) {
			float barH = listBottom - listTop;
			float thumb = Math.max(36, barH * barH / (barH + (float) maxScroll));
			float ty = listTop + (barH - thumb) * (float) (shownScroll / maxScroll);
			Paint.rect(d, x + WIDTH - 10, ty, x + WIDTH - 7, ty + thumb, 1.5f, 0x40CDB6FF);
		}
		Paint.opacity(old);
	}

	private void drawHead(Draw d, Module shown, float x, float y, float mx, float my) {
		boolean on = shown.enabled();
		headKnob.set(on);
		headLit.set(on);
		float tile = 50;
		Widgets.iconTile(d, x + PAD, y + PAD, tile, shown.icon(), shown.toggleable() ? headLit.get() : 1, 0);
		float tx = x + PAD + tile + 16;
		float right = x + WIDTH - PAD;
		float closeR = 15;
		float ccx = right - closeR;
		float ccy = y + PAD + closeR - 2;
		boolean closeHover = within(mx, my, ccx - closeR, ccy - closeR, ccx + closeR, ccy + closeR);
		Paint.circle(d, ccx, ccy, closeR, closeHover ? 0x1FFFFFFF : 0x0AFFFFFF);
		Paint.icon(d, "x", ccx - 7, ccy - 7, 14, closeHover ? Tokens.TEXT : Tokens.TEXT_2);
		hits.add(ccx - closeR, ccy - closeR, ccx + closeR, ccy + closeR, this::close);
		float textRight = ccx - closeR - 12;
		if (shown.toggleable()) {
			float sx = textRight - 44;
			Widgets.toggle(d, sx, y + PAD + 13, 44, 26, headKnob.get(), headLit.get());
			hits.add(sx - 4, y + PAD + 9, sx + 48, y + PAD + 43, shown::toggle);
			textRight = sx - 12;
		}
		Type.draw(d, Type.fit(shown.name, textRight - tx, 16, Weight.BOLD), tx, y + PAD + 7, 16, Weight.BOLD, Tokens.TEXT);
		List<String> lines = wrap(shown.description, textRight - tx, 10.5f, 2);
		for (int i = 0; i < lines.size(); i++) {
			Type.draw(d, lines.get(i), tx, y + PAD + 31 + i * 16, 10.5f, Weight.MEDIUM, Tokens.TEXT_2);
		}
		Paint.rect(d, x + PAD, y + HEAD - 14, x + WIDTH - PAD, y + HEAD - 13, 0, 0x14FFFFFF);
	}

	private static boolean segmentedFits(Setting.Choice choice, float width) {
		List<String> options = choice.options();
		if (options.size() > 4) return false;
		float segment = (width - 8) / options.size();
		for (String option : options) {
			if (Type.width(option, 10.5f, Weight.SEMIBOLD) > segment - 14) return false;
		}
		return true;
	}

	private float rowHeight(Setting setting, float width) {
		if (setting instanceof Setting.Slider) return 76;
		if (setting instanceof Setting.Color) return 92;
		if (setting instanceof Setting.Choice choice && segmentedFits(choice, width)) return 96;
		return 62;
	}

	private void drawRow(Draw d, Setting setting, float x, float y, float w, float mx, float my, long now) {
		float labelTop = y + 15;
		float descTop = y + 34;
		float controlSpace = setting instanceof Setting.Toggle ? 64 : setting instanceof Setting.Text ? 186
				: setting instanceof Setting.Choice c && !segmentedFits(c, w) ? 176 : setting instanceof Setting.Slider ? 70 : 0;
		Type.draw(d, Type.fit(setting.name, w - controlSpace, 11.5f, Weight.SEMIBOLD), x, labelTop, 11.5f, Weight.SEMIBOLD, Tokens.TEXT);
		Type.draw(d, Type.fit(setting.description, w - controlSpace, 9.5f, Weight.MEDIUM), x, descTop, 9.5f, Weight.MEDIUM, Tokens.TEXT_3);
		float right = x + w;
		if (setting instanceof Setting.Toggle toggle) {
			Anim[] anims = toggles.computeIfAbsent(setting, s -> new Anim[] {
					new Anim(toggle.get() ? 1 : 0, Tokens.T_SWITCH_KNOB, Ease.SPRING), new Anim(toggle.get() ? 1 : 0, Tokens.T_SWITCH_FILL, Ease.OUT)});
			anims[0].set(toggle.get());
			anims[1].set(toggle.get());
			Widgets.toggle(d, right - 40, y + 19, 40, 24, anims[0].get(), anims[1].get());
			hits.add(x - 6, y, right + 6, y + 62, toggle::toggle);
		} else if (setting instanceof Setting.Slider slider) {
			String value = slider.format();
			Type.right(d, value, right, labelTop, 11.5f, Weight.SEMIBOLD, Tokens.LILAC);
			float ty = y + 58;
			boolean hover = within(mx, my, x - 6, ty - 12, right + 6, ty + 12) || dragging == slider;
			Widgets.slider(d, x + 7, ty, right - 7, (float) slider.fraction(), hover ? 1 : 0);
			hits.add(x - 6, ty - 12, right + 6, ty + 12, () -> {
				dragging = slider;
				dragX = x + 7;
				dragW = w - 14;
				slider.setFraction((lastX - dragX) / dragW);
			});
		} else if (setting instanceof Setting.Choice choice) {
			List<String> options = choice.options();
			if (segmentedFits(choice, w)) {
				Anim at = segments.computeIfAbsent(setting, s -> new Anim(choice.index(), Tokens.T_INDICATOR, Ease.OUT));
				at.set(choice.index());
				float sy = y + 52;
				float segment = (w - 8) / options.size();
				int hover = -1;
				for (int i = 0; i < options.size(); i++) {
					float sx = x + 4 + i * segment;
					if (within(mx, my, sx, sy, sx + segment, sy + 34)) hover = i;
					String option = options.get(i);
					hits.add(sx, sy, sx + segment, sy + 34, () -> choice.set(option));
				}
				Widgets.segmented(d, x, sy, right, sy + 34, null, options.toArray(new String[0]), at.get(), hover);
			} else {
				float bw = 166;
				float bx = right - bw;
				float by = y + 15;
				boolean hover = within(mx, my, bx, by, right, by + 32);
				Paint.rect(d, bx, by, right, by + 32, 16, hover ? 0x14FFFFFF : 0x0BFFFFFF);
				Paint.stroke(d, bx, by, right, by + 32, 16, 1, hover ? 0x4DCDB6FF : 0x1AFFFFFF);
				Paint.icon(d, "chevron-left", bx + 9, by + 9, 14, Tokens.TEXT_2);
				Paint.icon(d, "chevron-right", right - 23, by + 9, 14, Tokens.TEXT_2);
				Type.center(d, Type.fit(choice.get(), bw - 56, 10.5f, Weight.SEMIBOLD), bx + bw / 2f, by + (32 - 10.5f) / 2f, 10.5f,
						Weight.SEMIBOLD, Tokens.TEXT);
				hits.add(bx, by, bx + bw / 2f, by + 32, choice::previous);
				hits.add(bx + bw / 2f, by, right, by + 32, choice::next);
			}
		} else if (setting instanceof Setting.Text text) {
			float fw = 176;
			float fx = right - fw;
			boolean focused = editing == text;
			Widgets.field(d, fx, y + 14, right, y + 48, "pencil", text.get(), "Type here", null, focused, 0, now);
			hits.add(fx, y + 14, right, y + 48, () -> editing = text);
		} else if (setting instanceof Setting.Color color) {
			int[] swatches = Setting.Color.SWATCHES;
			float size = 22;
			float gap = (w - swatches.length * size) / (swatches.length - 1);
			gap = Math.min(gap, 12);
			float sx = x + 2;
			float sy = y + 56;
			for (int swatch : swatches) {
				boolean selected = (color.get() | 0xFF000000) == swatch;
				boolean hover = within(mx, my, sx, sy, sx + size, sy + size);
				if (selected) {
					Paint.glow(d, sx + size / 2f, sy + size / 2f, size / 2f, 12, Paint.fade(swatch, 0.55f));
					Paint.ring(d, sx + size / 2f, sy + size / 2f, size / 2f + 3.5f, 1.6f, 0xE6FFFFFF);
				}
				Paint.circle(d, sx + size / 2f, sy + size / 2f, size / 2f - (hover && !selected ? 0 : 1), swatch);
				Paint.ring(d, sx + size / 2f, sy + size / 2f, size / 2f, 1, 0x26FFFFFF);
				int pick = swatch;
				hits.add(sx - 2, sy - 2, sx + size + 2, sy + size + 2, () -> color.set(pick));
				sx += size + gap;
			}
		}
	}

	private static List<String> wrap(String text, float width, float cap, int maxLines) {
		List<String> lines = new java.util.ArrayList<>();
		StringBuilder line = new StringBuilder();
		String[] words = text.split(" ");
		for (int i = 0; i < words.length; i++) {
			String next = line.isEmpty() ? words[i] : line + " " + words[i];
			if (Type.width(next, cap, Weight.MEDIUM) <= width || line.isEmpty()) {
				line = new StringBuilder(next);
				continue;
			}
			if (lines.size() == maxLines - 1) {
				StringBuilder rest = new StringBuilder(line);
				for (int j = i; j < words.length; j++) rest.append(' ').append(words[j]);
				lines.add(Type.fit(rest.toString(), width, cap, Weight.MEDIUM));
				return lines;
			}
			lines.add(line.toString());
			line = new StringBuilder(words[i]);
		}
		if (!line.isEmpty()) lines.add(line.toString());
		return lines;
	}

	private static boolean within(float x, float y, float x1, float y1, float x2, float y2) {
		return x >= x1 && x < x2 && y >= y1 && y < y2;
	}

	private float lastX;

	/** A click in layout units; returns whether it landed on the sheet. */
	public boolean mouseClicked(float x, float y, int button) {
		lastX = x;
		boolean inside = x >= sheetX && x < sheetX + WIDTH && y >= sheetY1 && y < sheetY2;
		Runnable action = hits.at(x, y, button == 1 ? 0 : button);
		editing = null;
		if (extras != null) extras.stopTyping();
		if (action == null) return inside;
		clickSound.run();
		action.run();
		return true;
	}

	public boolean mouseDragged(float x, float y) {
		if (dragging == null) return false;
		dragging.setFraction((x - dragX) / dragW);
		return true;
	}

	public boolean mouseReleased() {
		boolean was = dragging != null;
		dragging = null;
		return was;
	}

	public boolean mouseScrolled(float x, float y, double amount) {
		scroll = Math.max(0, Math.min(maxScroll, scroll - amount * 60));
		return true;
	}

	public boolean keyPressed(int key) {
		if (editing != null) {
			if (key == Keys.BACKSPACE) {
				String value = editing.get();
				if (!value.isEmpty()) editing.set(value.substring(0, value.length() - 1));
				return true;
			}
			if (Keys.enter(key) || key == Keys.ESCAPE) {
				editing = null;
				return true;
			}
			return true;
		}
		return extras != null && extras.typing() && extras.keyPressed(key);
	}

	public boolean charTyped(String typed) {
		if (editing != null) {
			if (editing.get().length() < editing.maxLength) editing.set(editing.get() + typed);
			return true;
		}
		return extras != null && extras.typing() && extras.charTyped(typed);
	}
}
