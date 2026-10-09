package dev.threevhy.client.gui;

import dev.threevhy.client.Draw;
import dev.threevhy.client.Hooks;
import dev.threevhy.client.ThreevhyClient;
import dev.threevhy.client.compat.Compat;
import dev.threevhy.client.compat.CompatTitleScreen;
import dev.threevhy.client.hud.Theme;
import dev.threevhy.client.module.Modules;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.MouseButtonEvent;

/**
 * The 3vhy main menu: the banner's sky with drifting pixel clouds, the pixel 3vhy wordmark and
 * big block buttons. Replaces Minecraft's title screen (turn it off in 3vhy Settings).
 */
public final class MainMenuScreen extends CompatTitleScreen {
	/** Banner colours from top to bottom, as {position 0..1, rgb}. */
	private static final int[][] SKY = {
			{0, 0x5BC8E6}, {7, 0x1A4FBA}, {22, 0x061D8C}, {50, 0x0B2A9A}, {76, 0x2A6FC4}, {100, 0x8CC4EA}
	};
	/** Pixel clouds as {x per mille of width, y per mille of height, width in cells, speed}. */
	private static final int[][] CLOUDS = {{80, 140, 22, 7}, {560, 90, 30, 5}, {340, 260, 18, 9}, {820, 210, 26, 6}};

	private record Hit(int x1, int y1, int x2, int y2, Runnable action) {
		boolean contains(double x, double y) {
			return x >= x1 && x < x2 && y >= y1 && y < y2;
		}
	}

	private final List<Hit> hits = new ArrayList<>();
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

		// Buttons
		int bw = Math.min(220, w - 40);
		int bh = 22;
		int gap = 6;
		int bx = (w - bw) / 2;
		int by = markY + 15 * cell + 30;
		button(d, mc, bx, by, bw, bh, "Multiplayer", true, mouseX, mouseY,
				() -> mc.setScreen(new JoinMultiplayerScreen(this)));
		by += bh + gap;
		button(d, mc, bx, by, bw, bh, "Singleplayer", false, mouseX, mouseY,
				() -> mc.setScreen(new SelectWorldScreen(this)));
		by += bh + gap;
		int half = (bw - gap) / 2;
		button(d, mc, bx, by, half, bh, "Options", false, mouseX, mouseY,
				() -> mc.setScreen(Compat.optionsScreen(this)));
		button(d, mc, bx + half + gap, by, bw - half - gap, bh, "3vhy Settings", false, mouseX, mouseY,
				() -> mc.setScreen(new SettingsScreen(this)));
		by += bh + gap;
		button(d, mc, bx, by, bw, bh, "Quit Game", false, mouseX, mouseY, mc::stop);

		// Who's playing
		String player = mc.getUser().getName();
		int pw = mc.font.width(player) + 21;
		d.roundRect(w - pw - 6, 6, w - 6, 22, 4, 0xA00A1A52);
		d.roundOutline(w - pw - 6, 6, w - 6, 22, 4, 0x505BC8E6);
		d.fill(w - pw + 1, 12, w - pw + 5, 16, Theme.GOOD);
		d.text(player, w - pw + 9, 10, Theme.ICE, false);

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

	private void button(Draw d, Minecraft mc, int x, int y, int w, int h, String label, boolean primary, int mx,
			int my, Runnable action) {
		boolean hover = mx >= x && mx < x + w && my >= y && my < y + h;
		int bg = primary ? (hover ? 0xFF8AE0F5 : Theme.CYAN) : (hover ? 0xD0234C9E : 0xA00A1A52);
		d.roundRect(x, y + 2, x + w, y + h + 2, 5, 0x30000000);
		d.roundRect(x, y, x + w, y + h, 5, bg);
		d.roundOutline(x, y, x + w, y + h, 5, primary ? 0x60FFFFFF : hover ? 0xB05BC8E6 : 0x505BC8E6);
		int tw = mc.font.width(label);
		d.text(label, x + (w - tw) / 2, y + (h - 8) / 2, primary ? Theme.NIGHT : Theme.ICE, false);
		hits.add(new Hit(x, y, x + w, y + h, action));
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == 0) {
			for (Hit hit : List.copyOf(hits)) {
				if (hit.contains(event.x(), event.y())) {
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
