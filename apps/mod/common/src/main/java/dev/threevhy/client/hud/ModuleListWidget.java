package dev.threevhy.client.hud;

import dev.threevhy.client.Draw;
import dev.threevhy.client.gui.Accent;
import dev.threevhy.client.gui.Smooth;
import dev.threevhy.client.gui.UiFont;
import dev.threevhy.client.module.Module;
import dev.threevhy.client.module.Modules;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * The Module List, like 67's: every mod you have on, longest name first, in small capitals down the
 * top right corner of the screen. Each name sits on its own dark strip, tinted with the client's
 * colour, with a short bar in that colour at the screen's edge.
 */
final class ModuleListWidget extends HudWidget {
	private static final float TEXT = 0.62f;
	private static final float CAP = 6.8f * TEXT;
	private static final float LINE = 2f * CAP;
	private static final float PAD = 0.35f * CAP;
	private static final float TO_BAR = 0.7f * CAP;
	private static final float BAR = 0.35f * CAP;
	private static final float BAR_INSET = 0.08f * CAP;
	private static final int FILL = 0x12121A;
	private static final String[] SAMPLE = {"CUSTOMCROSSHAIR", "KEYSTROKES", "FREELOOK", "ZOOM"};

	ModuleListWidget() {
		super("module_list", Modules.MODULE_LIST, Anchor.TOP_RIGHT);
	}

	private static Component label(String name) {
		return UiFont.clean() ? UiFont.bold(name) : Component.literal(name);
	}

	private static float nameWidth(Minecraft mc, String name) {
		return mc.font.width(label(name)) * TEXT;
	}

	private static float lineWidth(Minecraft mc, String name) {
		return PAD + nameWidth(mc, name) + TO_BAR + BAR;
	}

	private static List<String> names(Minecraft mc, boolean preview) {
		List<String> names = new ArrayList<>();
		for (Module module : Modules.ALL) {
			if (module == Modules.MODULE_LIST || !module.toggleable() || !module.enabled()) continue;
			names.add(module.name.replace(" ", "").toUpperCase(Locale.ROOT));
		}
		if (names.isEmpty() && preview) names.addAll(List.of(SAMPLE));
		names.sort(Comparator.comparingDouble((String name) -> lineWidth(mc, name)).reversed());
		return names;
	}

	@Override
	public boolean visible(Minecraft mc) {
		return !names(mc, false).isEmpty();
	}

	@Override
	public int width(Minecraft mc) {
		float widest = 30;
		for (String name : names(mc, true)) widest = Math.max(widest, lineWidth(mc, name));
		return (int) Math.ceil(widest);
	}

	@Override
	public int height(Minecraft mc) {
		return (int) Math.ceil(Math.max(1, names(mc, true).size()) * LINE);
	}

	/** Flush with the top right corner of the screen, like 67's. */
	@Override
	protected int[] defaultPosition(Minecraft mc, int screenWidth, int screenHeight) {
		return new int[] {screenWidth - width(mc), 0};
	}

	@Override
	public void draw(Draw d, int x, int y, Minecraft mc, boolean preview) {
		List<String> names = names(mc, preview);
		int right = x + width(mc);
		int alpha = backgroundAlpha();
		boolean shadow = textShadow();
		for (int i = 0; i < names.size(); i++) {
			String name = names.get(i);
			float top = y + i * LINE;
			float textW = nameWidth(mc, name);
			float left = right - BAR - TO_BAR - textW - PAD;
			int accent = Accent.get(i * 0.035f);
			if (alpha > 0) Smooth.roundRect(d, left, top, right, top + LINE, 0, (alpha << 24) | FILL);
			Smooth.roundRect(d, right - BAR, top + BAR_INSET, right, top + LINE - BAR_INSET, 0, accent);
			d.push();
			d.translate(left + PAD, top + (LINE - CAP) / 2f);
			d.scale(TEXT);
			d.text(label(name), 0, 0, Draw.mix(accent, 0xFFFFFFFF, 0.3), shadow);
			d.pop();
		}
	}
}
