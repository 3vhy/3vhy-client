package dev.threevhy.client.hud;

import dev.threevhy.client.Draw;
import dev.threevhy.client.gui.Accent;
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
 * The Module List, like 67's: every mod you have on, longest name first, down the right of the
 * screen. Each name sits on its own dark strip with a thin bar in the client's colour on its edge.
 */
final class ModuleListWidget extends HudWidget {
	private static final float TEXT = 0.75f;
	private static final int LINE = 9;
	private static final int PAD = 3;
	private static final int BAR = 1;
	private static final int FILL = 0x0B0C10;
	private static final String[] SAMPLE = {"CUSTOM CROSSHAIR", "KEYSTROKES", "FREELOOK", "ZOOM"};

	ModuleListWidget() {
		super("module_list", Modules.MODULE_LIST, Anchor.TOP_RIGHT);
	}

	private static Component label(String name) {
		return UiFont.clean() ? UiFont.bold(name) : Component.literal(name);
	}

	private static int lineWidth(Minecraft mc, String name) {
		return Math.round(mc.font.width(label(name)) * TEXT) + PAD * 2 + BAR;
	}

	private static List<String> names(Minecraft mc, boolean preview) {
		List<String> names = new ArrayList<>();
		for (Module module : Modules.ALL) {
			if (module == Modules.MODULE_LIST || !module.toggleable() || !module.enabled()) continue;
			names.add(module.name.toUpperCase(Locale.ROOT));
		}
		if (names.isEmpty() && preview) names.addAll(List.of(SAMPLE));
		names.sort(Comparator.comparingInt((String name) -> lineWidth(mc, name)).reversed());
		return names;
	}

	@Override
	public boolean visible(Minecraft mc) {
		return !names(mc, false).isEmpty();
	}

	@Override
	public int width(Minecraft mc) {
		int widest = 40;
		for (String name : names(mc, true)) widest = Math.max(widest, lineWidth(mc, name));
		return widest;
	}

	@Override
	public int height(Minecraft mc) {
		return Math.max(1, names(mc, true).size()) * LINE;
	}

	@Override
	public void draw(Draw d, int x, int y, Minecraft mc, boolean preview) {
		List<String> names = names(mc, preview);
		int w = width(mc);
		int alpha = backgroundAlpha();
		int accent = Accent.get();
		boolean shadow = textShadow();
		int lineY = y;
		for (String name : names) {
			int lw = lineWidth(mc, name);
			int lx = x + w - lw;
			if (alpha > 0) d.fill(lx, lineY, x + w, lineY + LINE, (alpha << 24) | FILL);
			d.fill(x + w - BAR, lineY, x + w, lineY + LINE, accent);
			d.push();
			d.translate(lx + PAD, lineY + 2);
			d.scale(TEXT);
			d.text(label(name), 0, 0, 0xFFFFFFFF, shadow);
			d.pop();
			lineY += LINE;
		}
	}
}
