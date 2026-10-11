package dev.threevhy.client.gui;

import dev.threevhy.client.Config;
import dev.threevhy.client.Draw;
import dev.threevhy.client.Hooks;
import dev.threevhy.client.ThreevhyClient;
import dev.threevhy.client.compat.Compat;
import dev.threevhy.client.compat.CompatTitleScreen;
import dev.threevhy.client.hud.Theme;
import dev.threevhy.client.module.Modules;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.MouseButtonEvent;

/**
 * The 3vhy main menu, in the style of the 3vhy installer: the banner's sky with drifting clouds, the
 * pixel wordmark dropping in letter by letter, glass pills, and a rounded glass card holding a big
 * cyan Multiplayer button. Replaces Minecraft's title screen (turn it off in the mod menu).
 */
public final class MainMenuScreen extends CompatTitleScreen {
	/** Banner colours from top to bottom, as {position 0..1, rgb}. */
	private static final int[][] SKY = {
			{0, 0x5BC8E6}, {7, 0x1A4FBA}, {22, 0x061D8C}, {50, 0x0B2A9A}, {76, 0x2A6FC4}, {100, 0x8CC4EA}
	};
	/** Pixel clouds as {x per mille of width, y per mille of height, width in cells, speed}. */
	private static final int[][] CLOUDS = {{80, 140, 22, 7}, {560, 90, 30, 5}, {340, 260, 18, 9}, {820, 210, 26, 6}};

	private final List<Ui.Hit> hits = Ui.newHits();
	private final Snow snow = new Snow();
	private final long openedAt = System.currentTimeMillis();

	public MainMenuScreen() {
		super();
	}

	/** Swaps Minecraft's own title screen for this one, when the main menu module is on. */
	public static Screen replace(Screen screen) {
		if (screen != null && screen.getClass() == TitleScreen.class && Modules.MAIN_MENU.enabled()) {
			return new MainMenuScreen();
		}
		return screen;
	}

	@Override
	protected void init() {
		Hooks.mainMenuShown = true;
	}

	@Override
	public void tick() {
	}

	@Override
	public void added() {
	}

	@Override
	public void removed() {
	}

	@Override
	protected void draw(Draw d, int mouseX, int mouseY, float delta) {
		Minecraft mc = Minecraft.getInstance();
		hits.clear();
		int w = this.width;
		int h = this.height;
		long elapsed = System.currentTimeMillis() - openedAt;
		drawBackdrop(d, w, h);
		snow.draw(d, w, h);
		Ui ui = new Ui(d, mouseX, mouseY, hits);

		drawTitleBar(ui, mc, w);

		int cell = Math.max(2, Math.min(5, Math.min(w / 150, (h - 190) / 15)));
		int cardW = Math.min(300, w - 24);
		int blockH = 15 * cell + 8 + 9 + 10 + 14 + 12 + CARD_H;
		int top = Math.max(30, (h - blockH) / 2);

		int markX = (w - 43 * cell) / 2;
		Wordmark.drawDropping(d, markX, top, cell, Theme.ICE, 0xFF04104A, elapsed);
		int y = top + 15 * cell + 8;
		String client = "C L I E N T";
		d.text(client, (w - mc.font.width(client)) / 2, y, fade(Theme.CYAN, elapsed, 450), false);
		y += 9 + 10;

		float rise = (float) Math.max(0, Math.min(1, (elapsed - 350) / 300.0));
		int lift = Math.round((1 - easeOut(rise)) * 8);
		d.push();
		d.translate(0, lift);
		drawChips(ui, mc, w, y);
		y += 14 + 12;
		drawCard(ui, mc, (w - cardW) / 2, y, cardW);
		d.pop();

		String version = "3vhy Client " + ThreevhyClient.version() + "  ·  Minecraft " + ThreevhyClient.minecraftVersion();
		d.text(version, 8, h - 23, 0xFF0B2A6E, false);
		d.text("Not affiliated with Mojang or Microsoft", 8, h - 13, 0xFF0B2A6E, false);
	}

	private static final int CARD_H = 10 + 26 + 7 + 22 + 8 + 12 + 8;
	private static final int GLASS = 0x8C061654;
	private static final int GLASS_EDGE = 0x33A9D8F2;

	/** The installer's title bar: the 3 app icon and the name. Who's playing is at the bottom right (MenuNameTag). */
	private void drawTitleBar(Ui ui, Minecraft mc, int w) {
		Draw d = ui.d;
		d.roundRect(8, 7, 24, 23, 4, 0xFF1A4FBA);
		d.roundOutline(8, 7, 24, 23, 4, 0x4DE3F3FC);
		Wordmark.drawGlyph(d, 11, 9, 1, Theme.ICE);
		d.text(Ui.bold("3vhy Client"), 30, 11, 0xE6CFE8F7, false);
	}

	/** Glass pills like the installer's feature chips: the game version, how many mods are on, the profile. */
	private void drawChips(Ui ui, Minecraft mc, int w, int y) {
		int on = 0;
		for (var module : Modules.ALL) {
			if (module.toggleable() && module.enabled()) on++;
		}
		List<String> chips = List.of("Minecraft " + ThreevhyClient.minecraftVersion(), on + " mods on",
				"Profile: " + Config.get().profile);
		int gap = 6;
		int total = -gap;
		for (String chip : chips) total += mc.font.width(chip) + 18 + gap;
		int x = (w - total) / 2;
		for (String chip : chips) {
			int cw = mc.font.width(chip) + 18;
			ui.d.roundRect(x, y, x + cw, y + 14, 7, GLASS);
			ui.d.roundOutline(x, y, x + cw, y + 14, 7, GLASS_EDGE);
			ui.d.roundRect(x + 6, y + 5, x + 10, y + 9, 2, Theme.CYAN);
			ui.d.text(chip, x + 13, y + 3, Theme.ICE, false);
			x += cw + gap;
		}
	}

	/** The rounded glass card with the big Multiplayer button, three smaller ones and Quit. */
	private void drawCard(Ui ui, Minecraft mc, int x, int y, int w) {
		Draw d = ui.d;
		d.roundRect(x, y, x + w, y + CARD_H, 10, 0xB8051246);
		d.roundOutline(x, y, x + w, y + CARD_H, 10, GLASS_EDGE);
		int inner = w - 20;
		int bx = x + 10;
		int by = y + 10;
		heroButton(ui, bx, by, inner, 26, "Multiplayer", () -> mc.setScreen(new JoinMultiplayerScreen(this)));
		by += 26 + 7;
		int gap = 6;
		String[] labels = {"Singleplayer", "Options", "3vhy Mods"};
		Runnable[] actions = {
				() -> mc.setScreen(new SelectWorldScreen(this)),
				() -> mc.setScreen(Compat.optionsScreen(this)),
				() -> mc.setScreen(new ModMenuScreen(this))
		};
		int textTotal = 0;
		for (String label : labels) textTotal += ui.titleWidth(label);
		int padding = Math.max(0, (inner - gap * (labels.length - 1) - textTotal) / labels.length);
		int sx = bx;
		for (int i = 0; i < labels.length; i++) {
			int bw = i == labels.length - 1 ? bx + inner - sx : ui.titleWidth(labels[i]) + padding;
			glassButton(ui, sx, by, bw, 22, labels[i], actions[i]);
			sx += bw + gap;
		}
		by += 22 + 8;
		String quit = "Quit Game";
		int qw = mc.font.width(quit);
		int qx = x + (w - qw) / 2;
		boolean hover = ui.hover(qx - 6, by - 2, qx + qw + 6, by + 10);
		d.text(quit, qx, by, hover ? Theme.ICE : 0xFFA9C3E6, false);
		if (hover) d.fill(qx, by + 9, qx + qw, by + 10, Theme.ICE);
		ui.hit(qx - 6, by - 2, qx + qw + 6, by + 10, mc::stop);
	}

	/** The installer's Install button: a cyan face shaded top to bottom on a deep blue edge. */
	private static void heroButton(Ui ui, int x, int y, int w, int h, String label, Runnable action) {
		boolean hover = ui.hover(x, y, x + w, y + h);
		int top = hover ? 0xFFA2E9F8 : 0xFF8AE0F5;
		int bottom = hover ? 0xFF3A9EE4 : 0xFF2B8FDB;
		ui.d.roundRect(x, y, x + w, y + h, 7, 0xFF0D3F99);
		ui.d.roundGradient(x, y, x + w, y + h - 3, 7, top, bottom);
		ui.d.fill(x + 7, y + 1, x + w - 7, y + 2, 0x8CFFFFFF);
		String text = label.toUpperCase(java.util.Locale.ROOT);
		ui.d.text(Ui.bold(text), x + (w - ui.boldWidth(text)) / 2, y + (h - 3 - 8) / 2 + 1, 0xFF04123F, false);
		ui.hit(x, y, x + w, y + h, action);
	}

	/** A frosted glass button with a thin edge that lights up on hover. */
	private static void glassButton(Ui ui, int x, int y, int w, int h, String label, Runnable action) {
		boolean hover = ui.hover(x, y, x + w, y + h);
		ui.d.roundRect(x, y, x + w, y + h, 6, hover ? 0xC0123A9A : GLASS);
		ui.d.roundOutline(x, y, x + w, y + h, 6, hover ? 0xA05BC8E6 : GLASS_EDGE);
		String text = ui.trimTitle(label, w - 8);
		ui.d.text(Ui.bold(text), x + (w - ui.boldWidth(text)) / 2, y + (h - 8) / 2, Theme.ICE, false);
		ui.hit(x, y, x + w, y + h, action);
	}

	private static int fade(int argb, long elapsed, long startMs) {
		double t = Math.max(0, Math.min(1, (elapsed - startMs) / 300.0));
		return ((int) Math.round(t * (argb >>> 24)) << 24) | (argb & 0xFFFFFF);
	}

	/** The banner sky with drifting clouds and hills. Also behind 3vhy Settings when it's opened from here. */
	public static void drawBackdrop(Draw d, int w, int h) {
		int bands = Math.max(1, h / 2);
		for (int i = 0; i < bands; i++) {
			int y1 = i * h / bands;
			int y2 = (i + 1) * h / bands;
			d.fill(0, y1, w, y2, 0xFF000000 | skyColor(i * 100.0 / bands));
		}
		long now = System.currentTimeMillis();
		int unit = Math.max(2, w / 220);
		for (int[] cloud : CLOUDS) {
			int cw = cloud[2] * unit;
			int span = w + cw;
			int x = (int) ((cloud[0] * w / 1000 + now / 1000.0 * cloud[3]) % span) - cw;
			int y = cloud[1] * h / 1000;
			int color = 0x22E3F3FC;
			d.fill(x + 3 * unit, y, x + cw - 4 * unit, y + unit * 2, color);
			d.fill(x, y + unit * 2, x + cw, y + unit * 4, color);
		}
		// Stepped pixel hills along the bottom
		int[][] back = {{0, 30}, {9, 34}, {20, 28}, {31, 33}, {43, 27}, {55, 31}, {66, 26}, {78, 32}, {89, 29}};
		int[][] front = {{0, 18}, {12, 21}, {25, 16}, {37, 20}, {50, 17}, {62, 22}, {75, 18}, {88, 20}};
		hills(d, w, h, back, 0x30CFE8F7);
		hills(d, w, h, front, 0x50CFE8F7);
	}

	private static void hills(Draw d, int w, int h, int[][] steps, int color) {
		for (int i = 0; i < steps.length; i++) {
			int x1 = steps[i][0] * w / 100;
			int x2 = i + 1 < steps.length ? steps[i + 1][0] * w / 100 : w;
			int top = h - steps[i][1] * h / 200;
			d.fill(x1, top, x2, h, color);
		}
	}

	private static int skyColor(double position) {
		for (int i = 0; i < SKY.length - 1; i++) {
			if (position <= SKY[i + 1][0]) {
				double t = (position - SKY[i][0]) / (SKY[i + 1][0] - SKY[i][0]);
				return mix(SKY[i][1], SKY[i + 1][1], t);
			}
		}
		return SKY[SKY.length - 1][1];
	}

	private static int mix(int a, int b, double t) {
		int r = (int) Math.round(((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
		int g = (int) Math.round(((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
		int bl = (int) Math.round((a & 255) * (1 - t) + (b & 255) * t);
		return (r << 16) | (g << 8) | bl;
	}

	private static float easeOut(float t) {
		return 1 - (1 - t) * (1 - t) * (1 - t);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == 0) {
			for (int i = hits.size() - 1; i >= 0; i--) {
				Ui.Hit hit = hits.get(i);
				if (hit.contains(event.x(), event.y()) && hit.action() != null) {
					Minecraft.getInstance().getSoundManager().play(
							net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
									net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0f));
					hit.action().run();
					return true;
				}
			}
		}
		return false;
	}
}
