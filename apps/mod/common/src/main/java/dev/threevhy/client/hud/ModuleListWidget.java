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
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * The Module List, like 67's: every mod you have on, longest name first, in small capitals down the
 * top right of the screen. Each name sits on its own dark rounded strip, tinted with the client's
 * colour, with a short rounded bar in that colour just right of it. Sizes are shares of the capital
 * height, measured off 67's.
 */
final class ModuleListWidget extends HudWidget {
	private static final float TEXT = 0.62f;
	private static final float CAP = 6.8f * TEXT;
	private static final float LINE = 2.07f * CAP;
	private static final float PAD = 0.65f * CAP;
	private static final float RADIUS = 0.4f * CAP;
	private static final float BAR_GAP = 0.25f * CAP;
	private static final float BAR = 0.35f * CAP;
	private static final float BAR_H = 0.78f * LINE;
	private static final int FILL = 0x13101A;
	private static final String[] SAMPLE = {"CUSTOMCROSSHAIR", "KEYSTROKES", "FREELOOK", "ZOOM"};
	private static final Set<Module> OUT_OF_GAME = Set.of(Modules.MENU_NAME, Modules.MAIN_MENU, Modules.DISCORD,
			Modules.BORDERLESS, Modules.PROFILES);

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
		return PAD + nameWidth(mc, name) + PAD + BAR_GAP + BAR;
	}

	/** Whether a mod that's on belongs in the list: not the menu and window ones, and HUD mods only if asked for. */
	private static boolean listed(Module module) {
		if (module == Modules.MODULE_LIST || !module.toggleable() || !module.enabled()) return false;
		if (OUT_OF_GAME.contains(module)) return false;
		return module.category != Module.Category.HUD || Modules.MODULE_LIST_HUD.get();
	}

	private static List<String> names(Minecraft mc, boolean preview) {
		List<String> names = new ArrayList<>();
		for (Module module : Modules.ALL) {
			if (listed(module)) names.add(module.name.replace(" ", "").toUpperCase(Locale.ROOT));
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
			float stripRight = right - BAR - BAR_GAP;
			float left = stripRight - PAD - textW - PAD;
			int accent = Accent.get(i * 0.035f);
			if (alpha > 0) Smooth.roundRect(d, left, top, stripRight, top + LINE, RADIUS, (alpha << 24) | FILL);
			float barTop = top + (LINE - BAR_H) / 2f;
			Smooth.roundRect(d, right - BAR, barTop, right, barTop + BAR_H, BAR / 2f, accent);
			d.push();
			d.translate(left + PAD, top + (LINE - CAP) / 2f);
			d.scale(TEXT);
			d.text(label(name), 0, 0, Draw.mix(accent, 0xFFFFFFFF, 0.3), shadow);
			d.pop();
		}
	}
}
