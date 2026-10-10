package dev.threevhy.client.gui;

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
 * The 3vhy main menu: the banner's sky with drifting pixel clouds, the pixel 3vhy wordmark and
 * big block buttons. Replaces Minecraft's title screen (turn it off in the mod menu).
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
		drawBackdrop(d, w, h);
		snow.draw(d, w, h);
		Ui ui = new Ui(d, mouseX, mouseY, hits);
		MenuTheme t = ui.t;

		// Wordmark, sized to the window
		int cell = Math.max(2, Math.min(5, w / 150));
		int markW = 43 * cell;
		int markX = (w - markW) / 2;
		int markY = Math.max(16, h / 2 - 15 * cell - 74);
		float intro = Math.min(1f, (System.currentTimeMillis() - openedAt) / 450f);
		int drop = Math.round((1 - easeOut(intro)) * -12);
		Wordmark.draw(d, markX, markY + drop, cell, Theme.ICE, 0xFF04104A);
		String client = "C L I E N T";
		d.text(client, (w - mc.font.width(client)) / 2, markY + 15 * cell + 8, Theme.CYAN, false);

		// Buttons, NoRisk-style glass in 3vhy colours
		int bw = Math.min(220, w - 40);
		int bh = 20;
		int gap = 5;
		int bx = (w - bw) / 2;
		int by = markY + 15 * cell + 28;
		ui.button(bx, by, bw, bh, "Multiplayer", true, () -> mc.setScreen(new JoinMultiplayerScreen(this)));
		by += bh + gap;
		ui.button(bx, by, bw, bh, "Singleplayer", false, () -> mc.setScreen(new SelectWorldScreen(this)));
		by += bh + gap;
		int half = (bw - gap) / 2;
		ui.button(bx, by, half, bh, "Options", false, () -> mc.setScreen(Compat.optionsScreen(this)));
		ui.button(bx + half + gap, by, bw - half - gap, bh, "3vhy Mods", false, () -> mc.setScreen(new ModMenuScreen(this)));
		by += bh + gap;
		ui.button(bx, by, bw, bh, "Quit Game", false, mc::stop);

		// Who's playing
		String player = mc.getUser().getName();
		int pw = mc.font.width(player) + 21;
		ui.panel(w - pw - 6, 6, w - 6, 22, t.panel());
		d.fill(w - pw + 1, 12, w - pw + 5, 16, t.good());
		d.text(player, w - pw + 9, 10, t.text(), false);

		String left = "3vhy Client " + ThreevhyClient.version() + "  ·  Minecraft " + ThreevhyClient.minecraftVersion();
		d.text(left, 6, h - 12, 0xFF0B2A6E, false);
		String right = "Not affiliated with Mojang or Microsoft";
		d.text(right, w - 6 - mc.font.width(right), h - 12, 0xFF0B2A6E, false);
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
