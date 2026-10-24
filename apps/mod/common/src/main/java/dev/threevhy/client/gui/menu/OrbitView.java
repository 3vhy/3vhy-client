package dev.threevhy.client.gui.menu;

import dev.threevhy.client.Draw;
import dev.threevhy.client.gui.Accent;
import dev.threevhy.client.module.Modules;
import dev.threevhy.client.ui.Anim;
import dev.threevhy.client.ui.Ease;
import dev.threevhy.client.ui.Grad;
import dev.threevhy.client.ui.Keys;
import dev.threevhy.client.ui.Logo;
import dev.threevhy.client.ui.Paint;
import dev.threevhy.client.ui.Sky;
import dev.threevhy.client.ui.Spring;
import dev.threevhy.client.ui.Tokens;
import dev.threevhy.client.ui.Type;
import dev.threevhy.client.ui.Type.Weight;
import dev.threevhy.client.ui.Widgets;
import java.util.List;

/**
 * The Orbit (Right Shift): the menu's tabs as planets on a tilted ring around the 3vhy logo, like the
 * website's renderer section. The ring spins on a spring; the planet at the front is lit and named
 * underneath. Drag, scroll or use the arrow keys to spin it, click a planet to bring it round, and click
 * the front one (or press Enter) to open that tab.
 */
public final class OrbitView {
	/** What the Orbit needs from the game. */
	public interface Host {
		void open(int tab);

		void close();

		void clickSound();

		boolean inWorld();
	}

	private static final float TILT = (float) Math.toRadians(-11);
	private static final double FRONT = Math.PI / 2;
	private static final int RING_POINTS = 96;
	private static final Grad RING = new Grad(0xFF6A2CFF, 0xFFE05CF0, 90);

	private final Host host;
	private final List<MenuView.Tab> tabs = MenuView.TABS;
	private final double step;
	private final Spring spin;
	private final Anim[] hover;
	private final long openedAt = System.currentTimeMillis();
	private final float[] ringXY = new float[(RING_POINTS + 1) * 2];
	private final int[] ringColors = new int[RING_POINTS + 1];
	private final float[] dashXY = new float[14 * 2];
	private final int[] dashColors = new int[14];
	private int front = -1;
	private long frontSince = 0;
	private boolean dragging = false;
	private boolean dragged = false;
	private double dragLastX;
	private long dragLastAt;
	private double velocity;

	private float unit = 1;
	private float cx;
	private float cy;
	private float rx;
	private float ry;
	private float mouseX;
	private float mouseY;

	public OrbitView(Host host, int startTab) {
		this.host = host;
		this.step = Math.PI * 2 / tabs.size();
		double target = FRONT - startTab * step;
		this.spin = new Spring(target - 0.9, 55, 11.5);
		spin.target(target);
		this.hover = new Anim[tabs.size()];
		for (int i = 0; i < tabs.size(); i++) hover[i] = new Anim(0, Tokens.T_COLOR, Ease.OUT);
	}

	/** The tab at the front right now. */
	public int front() {
		return Math.floorMod((int) Math.round((FRONT - spin.target()) / step), tabs.size());
	}

	public void draw(Draw d, float guiW, float guiH, double mx, double my) {
		long now = System.currentTimeMillis();
		unit = Math.min(guiW / MenuView.DESIGN_W, guiH / MenuView.DESIGN_H);
		float w = guiW / unit;
		float h = guiH / unit;
		mouseX = (float) (mx / unit);
		mouseY = (float) (my / unit);
		double angle = spin.update();
		float intro = Ease.OUT.apply((now - openedAt) / 700.0);
		float old = Paint.opacity(Ease.OUT.apply((now - openedAt) / 380.0));
		d.push();
		d.scale(unit);
		Sky.backdrop(d, w, h, host.inWorld(), 1);
		if (Modules.THEME_SNOW.get()) Sky.stars(d, w, h, (float) (Modules.THEME_SNOW_AMOUNT.get() / 100.0), 1, now);

		float size = Math.min(w * 0.58f, 980);
		cx = w / 2f;
		cy = h / 2f - 46;
		rx = size * 0.44f * (0.9f + 0.1f * intro);
		ry = size * 0.15f * (0.9f + 0.1f * intro);

		int current = Math.floorMod((int) Math.round((FRONT - angle) / step), tabs.size());
		if (current != front) {
			front = current;
			frontSince = now;
		}

		drawRing(d, false, now);
		drawPlanets(d, angle, false, now);
		drawCore(d, size, now);
		drawRing(d, true, now);
		drawPlanets(d, angle, true, now);
		drawLabel(d, w, cy + ry + size * 0.16f, now);
		d.pop();
		Paint.opacity(old);
	}

	private float px(double a) {
		float x = (float) Math.cos(a) * rx;
		float y = (float) Math.sin(a) * ry;
		return cx + x * (float) Math.cos(TILT) - y * (float) Math.sin(TILT);
	}

	private float py(double a) {
		float x = (float) Math.cos(a) * rx;
		float y = (float) Math.sin(a) * ry;
		return cy + x * (float) Math.sin(TILT) + y * (float) Math.cos(TILT);
	}

	/** One half of the ring, as on the website: the back half faint and thin, the front half bright. */
	private void drawRing(Draw d, boolean frontHalf, long now) {
		double start = frontHalf ? 0 : Math.PI;
		int n = RING_POINTS / 2;
		float alpha = frontHalf ? 0.95f : 0.55f;
		float glowAlpha = frontHalf ? 0.16f : 0.08f;
		for (int pass = 0; pass < 2; pass++) {
			for (int i = 0; i <= n; i++) {
				double a = start + Math.PI * i / n;
				float x = px(a);
				float y = py(a);
				ringXY[2 * i] = x;
				ringXY[2 * i + 1] = y;
				int c = RING.at(x, y, cx - rx, cy - ry, cx + rx, cy + ry);
				ringColors[i] = Paint.fade(c, pass == 0 ? glowAlpha : alpha);
			}
			Paint.line(d, ringXY, ringColors, n + 1, false, pass == 0 ? 7 : frontHalf ? 2 : 1.4f);
		}
		double run = (now % 9000) / 9000.0 * Math.PI * 2;
		double length = Math.PI * 2 * 0.04;
		boolean runFront = Math.sin(run + length / 2) > 0;
		if (runFront != frontHalf) return;
		for (int i = 0; i < 14; i++) {
			double a = run + length * i / 13;
			dashXY[2 * i] = px(a);
			dashXY[2 * i + 1] = py(a);
			float edge = 1 - Math.abs(i - 6.5f) / 6.5f;
			dashColors[i] = Paint.fade(0xFFFFFFFF, 0.25f + 0.75f * edge);
		}
		Paint.glow(d, dashXY[14], dashXY[15], 3, 14, 0x99EBC8FF);
		Paint.line(d, dashXY, dashColors, 14, false, 2);
	}

	private void drawCore(Draw d, float size, long now) {
		float mark = size * 0.33f;
		float breathe = 1 + 0.025f * (float) Math.sin(now / 2600.0 * Math.PI * 2);
		float m = mark * breathe;
		float x = cx - m / 2f;
		float y = cy - m * 0.6f;
		Paint.glow(d, cx, cy - m * 0.1f, m * 0.55f, m * 0.9f, Paint.fade(0xFF9628FA, 0.3f + 0.06f * (breathe - 1) * 40));
		Logo.mark(d, x, y, m, 1, now);
	}

	private void drawPlanets(Draw d, double angle, boolean frontHalf, long now) {
		for (int i = 0; i < tabs.size(); i++) {
			double a = angle + i * step;
			float sin = (float) Math.sin(a);
			if ((sin > 0) != frontHalf) continue;
			float depth = (sin + 1) / 2f;
			float scale = 0.6f + 0.4f * depth;
			float alpha = 0.3f + 0.7f * depth;
			float r = 33 * scale;
			float x = px(a);
			float y = py(a);
			boolean isFront = i == front;
			boolean over = Math.hypot(mouseX - x, mouseY - y) <= r;
			hover[i].set(over);
			float hv = hover[i].get();
			float old = Paint.opacity(Paint.opacity() * alpha);
			Paint.shadow(d, x - r, y - r, x + r, y + r, r, 0, r * 0.35f, r * 0.8f, -r * 0.35f, 0xE6000000);
			if (isFront) {
				float lit = Ease.OUT.apply((System.currentTimeMillis() - frontSince) / 400.0);
				Paint.glow(d, x, y, r, 34 * lit, Accent.glow(0.9f * lit));
				Paint.circle(d, x, y, r, Accent.primary());
				Paint.glow(d, x - r * 0.32f, y - r * 0.44f, r * 0.3f, r * 0.7f, 0x59FFFFFF);
				Paint.ring(d, x, y, r, 1, 0x59FFFFFF);
			} else {
				Paint.circle(d, x, y, r, Widgets.mixColor(0xD1221C3C, 0xE02A2350, hv));
				Paint.glow(d, x - r * 0.32f, y - r * 0.44f, r * 0.25f, r * 0.6f, 0x29FFFFFF);
				Paint.ring(d, x, y, r, 1, Widgets.mixColor(0x21FFFFFF, 0x4DCDB6FF, hv));
			}
			float icon = 24 * scale;
			int color = isFront ? 0xFFFFFFFF : Widgets.mixColor(Tokens.TEXT_2, Tokens.TEXT, hv);
			Paint.icon(d, tabs.get(i).icon(), x - icon / 2f, y - icon / 2f, icon, color);
			Paint.opacity(old);
		}
	}

	private void drawLabel(Draw d, float w, float top, long now) {
		float cap = 10.5f;
		String hint = "opens the Orbit";
		float kw = Widgets.kbdWidth("Right Shift", cap);
		float total = kw + 8 + Type.width(hint, cap, Weight.MEDIUM);
		float kx = w / 2f - total / 2f;
		Widgets.kbd(d, kx, top, "Right Shift", cap);
		Type.draw(d, hint, kx + kw + 8, top + (cap * 2.4f - cap) / 2f, cap, Weight.MEDIUM, Tokens.TEXT_3);
		MenuView.Tab tab = tabs.get(Math.max(0, front));
		float t = Ease.OUT.apply((now - frontSince) / 600.0);
		float rise = (1 - t) * 10;
		float old = Paint.opacity(Paint.opacity() * t);
		Type.center(d, tab.name(), w / 2f, top + 46 + rise, 22, Weight.BOLD, Tokens.TEXT);
		Type.center(d, tab.tagline(), w / 2f, top + 80 + rise, 12.5f, Weight.MEDIUM, Tokens.TEXT_2);
		Paint.opacity(old);
		String help = "Drag or scroll to spin  ·  Click or Enter to open  ·  Esc to close";
		Type.center(d, help, w / 2f, top + 122, 10.5f, Weight.MEDIUM, Paint.fade(Tokens.TEXT_3, 0.85f));
	}

	private void spinTo(int tab) {
		double target = FRONT - tab * step;
		double current = spin.value();
		while (target - current > Math.PI) target -= Math.PI * 2;
		while (current - target > Math.PI) target += Math.PI * 2;
		spin.target(target);
	}

	private void spinBy(int steps) {
		spinTo(Math.floorMod(front() + steps, tabs.size()));
	}

	/** The planet under the mouse, nearest first, or -1. */
	private int planetAt(float x, float y) {
		int best = -1;
		float bestDepth = -2;
		double angle = spin.value();
		for (int i = 0; i < tabs.size(); i++) {
			double a = angle + i * step;
			float sin = (float) Math.sin(a);
			float r = 33 * (0.6f + 0.4f * (sin + 1) / 2f);
			if (Math.hypot(x - px(a), y - py(a)) <= r && sin > bestDepth) {
				best = i;
				bestDepth = sin;
			}
		}
		return best;
	}

	public boolean mouseClicked(double x, double y, int button) {
		float mx = (float) (x / unit);
		float my = (float) (y / unit);
		dragging = true;
		dragged = false;
		dragLastX = mx;
		dragLastAt = System.nanoTime();
		velocity = 0;
		return true;
	}

	public boolean mouseDragged(double x, double y) {
		if (!dragging) return false;
		float mx = (float) (x / unit);
		double dx = mx - dragLastX;
		if (Math.abs(dx) > 2) dragged = true;
		if (!dragged) return true;
		long now = System.nanoTime();
		double delta = -dx / Math.max(1, rx) * 1.1;
		spin.push(delta);
		velocity = delta / Math.max(1e-3, (now - dragLastAt) / 1e9);
		dragLastX = mx;
		dragLastAt = now;
		return true;
	}

	public boolean mouseReleased(double x, double y) {
		if (!dragging) return false;
		dragging = false;
		if (dragged) {
			double fling = spin.value() + velocity * 0.12;
			int nearest = Math.floorMod((int) Math.round((FRONT - fling) / step), tabs.size());
			spinTo(nearest);
			return true;
		}
		float mx = (float) (x / unit);
		float my = (float) (y / unit);
		int planet = planetAt(mx, my);
		boolean onCore = Math.hypot(mx - cx, my - cy) < rx * 0.3f;
		if (planet >= 0 && planet != front()) {
			host.clickSound();
			spinTo(planet);
		} else if (planet == front() || onCore) {
			host.clickSound();
			host.open(front());
		}
		return true;
	}

	public boolean mouseScrolled(double amount) {
		spinBy(amount > 0 ? -1 : 1);
		return true;
	}

	public boolean keyPressed(int key) {
		if (key == Keys.LEFT || key == Keys.UP) {
			spinBy(-1);
			return true;
		}
		if (key == Keys.RIGHT || key == Keys.DOWN || key == Keys.TAB) {
			spinBy(1);
			return true;
		}
		if (Keys.enter(key) || key == Keys.SPACE) {
			host.clickSound();
			host.open(front());
			return true;
		}
		if (key == Keys.ESCAPE) {
			host.close();
			return true;
		}
		return false;
	}
}
