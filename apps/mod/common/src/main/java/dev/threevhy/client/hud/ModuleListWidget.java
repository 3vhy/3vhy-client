package dev.threevhy.client.hud;

import dev.threevhy.client.Draw;
import dev.threevhy.client.gui.Accent;
import dev.threevhy.client.module.Module;
import dev.threevhy.client.module.Modules;
import dev.threevhy.client.ui.Paint;
import dev.threevhy.client.ui.Type;
import dev.threevhy.client.ui.Type.Weight;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;

/**
 * Active Modules: every mod you have on, longest name first, down the top right of the screen. Each
 * name sits on its own strip of violet glass with a short accent bar just right of it that glows, the
 * accent shifting a little down the list.
 */
final class ModuleListWidget extends HudWidget {
	private static final float CAP = Hud.CAP * 0.85f;
	private static final float LINE = 2.15f * CAP;
	private static final float PAD = 0.75f * CAP;
	private static final float RADIUS = 0.5f * CAP;
	private static final float BAR_GAP = 0.3f * CAP;
	private static final float BAR = 0.38f * CAP;
	private static final float BAR_H = 0.7f * LINE;
	private static final String[] SAMPLE = {"Custom Crosshair", "Keystrokes", "Freelook", "Zoom"};
	private static final Set<Module> OUT_OF_GAME = Set.of(Modules.MENU_NAME, Modules.MAIN_MENU, Modules.DISCORD, Modules.BORDERLESS,
			Modules.PROFILES, Modules.THEME);

	ModuleListWidget() {
		super("module_list", Modules.MODULE_LIST, Anchor.TOP_RIGHT);
	}

	private static float nameWidth(String name) {
		return Type.width(name, CAP, Weight.SEMIBOLD);
	}

	private static float lineWidth(String name) {
		return PAD + nameWidth(name) + PAD + BAR_GAP + BAR;
	}

	/** Whether a mod that's on belongs in the list: not the menu and window ones, and HUD mods only if asked for. */
	private static boolean listed(Module module) {
		if (module == Modules.MODULE_LIST || !module.toggleable() || !module.enabled()) return false;
		if (OUT_OF_GAME.contains(module)) return false;
		return module.category != Module.Category.HUD || Modules.MODULE_LIST_HUD.get();
	}

	private static List<String> names(boolean preview) {
		List<String> names = new ArrayList<>();
		for (Module module : Modules.ALL) {
			if (listed(module)) names.add(module.name);
		}
		if (names.isEmpty() && preview) names.addAll(List.of(SAMPLE));
		names.sort(Comparator.comparingDouble(ModuleListWidget::lineWidth).reversed());
		return names;
	}

	@Override
	public boolean visible(Minecraft mc) {
		return !names(false).isEmpty();
	}

	@Override
	public int width(Minecraft mc) {
		float widest = 30;
		for (String name : names(true)) widest = Math.max(widest, lineWidth(name));
		return (int) Math.ceil(widest);
	}

	@Override
	public int height(Minecraft mc) {
		return (int) Math.ceil(Math.max(1, names(true).size()) * LINE);
	}

	@Override
	public void draw(Draw d, int x, int y, Minecraft mc, boolean preview) {
		List<String> names = names(preview);
		int right = x + width(mc);
		boolean shadow = textShadow();
		boolean glow = Modules.MODULE_LIST_GLOW.get();
		float strength = glassStrength();
		for (int i = 0; i < names.size(); i++) {
			String name = names.get(i);
			float top = y + i * LINE;
			float stripRight = right - BAR - BAR_GAP;
			float left = stripRight - PAD - nameWidth(name) - PAD;
			int accent = Accent.get(i * 0.035f);
			if (strength > 0.01f) {
				float old = Paint.opacity(Paint.opacity() * Math.min(1, strength * 1.15f));
				Paint.rect(d, left, top, stripRight, top + LINE, radius(RADIUS), 0xA80A0814);
				Paint.opacity(old);
			}
			float barTop = top + (LINE - BAR_H) / 2f;
			if (glow) Paint.shadow(d, right - BAR, barTop, right, barTop + BAR_H, BAR / 2f, 0, 0, BAR * 4, 0, Paint.fade(accent, 0.75f));
			Paint.rect(d, right - BAR, barTop, right, barTop + BAR_H, BAR / 2f, accent);
			Hud.text(d, name, left + PAD, top + (LINE - CAP) / 2f, CAP, Weight.SEMIBOLD, Draw.mix(accent, 0xFFFFFFFF, 0.55), shadow);
		}
	}
}
