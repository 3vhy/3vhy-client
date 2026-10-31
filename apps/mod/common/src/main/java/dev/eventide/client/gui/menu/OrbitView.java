package dev.eventide.client.gui.menu;

import dev.eventide.client.Draw;
import dev.eventide.client.gui.Accent;
import dev.eventide.client.module.Module;
import dev.eventide.client.module.Modules;
import dev.eventide.client.ui.Anim;
import dev.eventide.client.ui.Ease;
import dev.eventide.client.ui.Keys;
import dev.eventide.client.ui.Logo;
import dev.eventide.client.ui.Paint;
import dev.eventide.client.ui.Sky;
import dev.eventide.client.ui.Spring;
import dev.eventide.client.ui.Tokens;
import dev.eventide.client.ui.Type;
import dev.eventide.client.ui.Type.Weight;
import dev.eventide.client.ui.Widgets;
import java.util.Arrays;
import java.util.List;

/**
 * The Orbit (Right Shift): the menu's tabs as discs on a ring seen from slightly above, with the Eventide
 * trident standing in the middle. The tab at the front is the big violet disc at the trident's foot, named
 * underneath with its tagline and how many of its mods are on; the rest shrink and fade as they go round the
 * back, the nearer ones labelled. Drag, scroll or use
 * the arrow keys to turn the ring, click a disc to bring it to the front, and click the front one (or
 * press Enter) to open that tab. It opens at once, since it's opened from the keyboard.
 */
public final class OrbitView {
	/** What the Orbit needs from the game. */
	public interface Host {
		void open(int tab);

		void close();

		void clickSound();

		boolean inWorld();
	}

	/** The angle on the ring that is the front: the bottom of the ellipse, nearest you. */
	private static final double FRONT = Math.PI / 2;
	/**
	 * Sizes in design units, measured off purxiy's hub as shares of the front disc: the ring is 4.2 front
	 * radii across each way and 1.4 deep, tilted a little; the trident stands 1.75 discs tall on the front
	 * disc; the title's capitals are 0.3 of a disc and sit a disc below its middle.
	 */
	private static final float FRONT_R = 55;
	private static final float RING_X = FRONT_R * 4.2f;
	private static final float RING_Y = FRONT_R * 1.4f;
	private static final float TILT = (float) Math.toRadians(-9);
	private static final float MARK = FRONT_R * 2 * 1.9f;
	private static final int RING_POINTS = 72;
	private static final int DISC = 0xF01E1934;
	private static final int DISC_HOVER = 0xF02A2347;
	private static final int PILL = 0xD91B1530;
	private static final int PILL_EDGE = 0x1AFFFFFF;

	private final Host host;
	private final List<MenuView.Tab> tabs = MenuView.TABS;
	private final double step;
	private final Spring spin;
	private final Anim[] hover;
	private final Anim[] lit;
	private final Integer[] order;
	private int front = -1;
	private long frontSince = 0;
	private boolean dragging = false;
	private boolean dragged = false;
	private double dragLastX;
	private long dragLastAt;
	private double velocity;

	private final float[] ringXY = new float[(RING_POINTS + 1) * 2];
	private final int[] ringColors = new int[RING_POINTS + 1];
	private float unit = 1;
	private float cx;
	private float cy;
	private float shiftX;
	private float shiftY;
	private float mouseX;
	private float mouseY;

	public OrbitView(Host host, int startTab) {
		this.host = host;
		this.step = Math.PI * 2 / tabs.size();
		this.spin = new Spring(FRONT - startTab * step, 260, 30);
		this.hover = new Anim[tabs.size()];
		this.lit = new Anim[tabs.size()];
		this.order = new Integer[tabs.size()];
		for (int i = 0; i < tabs.size(); i++) {
			hover[i] = new Anim(0, Tokens.T_HOVER, Ease.OUT);
			lit[i] = new Anim(i == startTab ? 1 : 0, Tokens.T_COLOR, Ease.OUT);
			order[i] = i;
		}
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
		d.push();
		d.scale(unit);
		Sky.backdrop(d, w, h, host.inWorld(), 1);
		if (Modules.THEME_SNOW.get()) Sky.stars(d, w, h, (float) (Modules.THEME_SNOW_AMOUNT.get() / 100.0), 1, now);

		float mark = 26;
		float cap = 12;
		float lockup = mark * 1.24f + Logo.wordmarkWidth(cap);
		Logo.lockup(d, w / 2f - lockup / 2f, 30, mark, cap, now);

		float discY = h * 0.485f;
		cx = w / 2f;
		cy = discY - RING_Y;
		shiftX = 0;
		shiftY = 0;
		shiftX = cx - px(FRONT);
		shiftY = discY - py(FRONT);
		int current = Math.floorMod((int) Math.round((FRONT - angle) / step), tabs.size());
		if (current != front) {
			front = current;
			frontSince = now;
		}
		drawRing(d);
		drawDiscs(d, angle, now);
		drawTitle(d, w, discY + FRONT_R * 2 * 1.01f, now);
		d.pop();
	}

	private float px(double a) {
		float x = (float) Math.cos(a) * RING_X;
		float y = (float) Math.sin(a) * RING_Y;
		return cx + shiftX + x * (float) Math.cos(TILT) - y * (float) Math.sin(TILT);
	}

	private float py(double a) {
		float x = (float) Math.cos(a) * RING_X;
		float y = (float) Math.sin(a) * RING_Y;
		return cy + shiftY + x * (float) Math.sin(TILT) + y * (float) Math.cos(TILT);
	}

	/** The ring the discs ride on: a faint line, as on purxiy's hub. */
	private void drawRing(Draw d) {
		for (int i = 0; i <= RING_POINTS; i++) {
			double a = Math.PI * 2 * i / RING_POINTS;
			ringXY[2 * i] = px(a);
			ringXY[2 * i + 1] = py(a);
			ringColors[i] = Paint.fade(Accent.get(), 0.08f + 0.14f * depth(a));
		}
		Paint.line(d, ringXY, ringColors, RING_POINTS + 1, false, 1.4f);
	}

	/** How near the front a disc at angle {@code a} is, 0 at the back to 1 at the front. */
	private static float depth(double a) {
		return ((float) Math.sin(a) + 1) / 2f;
	}

	private static float radius(float depth) {
		return FRONT_R * (0.42f + 0.58f * depth * depth);
	}

	/**
	 * The discs from the back of the ring to the front, with the Eventide trident standing in the middle of
	 * the ring: the far discs go behind it, the near ones (the front disc too) in front of its foot.
	 */
	private void drawDiscs(Draw d, double angle, long now) {
		Arrays.sort(order, (a, b) -> Float.compare(depth(angle + a * step), depth(angle + b * step)));
		boolean markDrawn = false;
		for (int i : order) {
			double a = angle + i * step;
			float depth = depth(a);
			if (!markDrawn && depth > 0.5f) {
				drawMark(d, now);
				markDrawn = true;
			}
			float r = radius(depth);
			float x = px(a);
			float y = py(a);
			boolean isFront = i == front;
			hover[i].set(!isFront && Math.hypot(mouseX - x, mouseY - y) <= r);
			lit[i].set(isFront);
			float hv = hover[i].get();
			float on = lit[i].get();
			float old = Paint.opacity(Paint.opacity() * (0.3f + 0.7f * (float) Math.pow(depth, 1.3)));
			int fill = Widgets.mixColor(Widgets.mixColor(DISC, DISC_HOVER, hv), Accent.get(), on);
			Paint.circle(d, x, y, r, fill);
			float icon = r * (isFront ? 0.6f : 0.66f);
			int color = Widgets.mixColor(Widgets.mixColor(Tokens.TEXT_2, Tokens.TEXT, hv), 0xFFFFFFFF, on);
			Paint.icon(d, tabs.get(i).icon(), x - icon / 2f, y - icon / 2f, icon, color);
			if (!isFront && depth > 0.3f) {
				Type.center(d, tabs.get(i).name(), x, y + r + 12, 13, Weight.MEDIUM, Widgets.mixColor(Tokens.TEXT_3, Tokens.TEXT_2, hv));
			}
			Paint.opacity(old);
		}
	}

	/** The trident standing upright on the front disc, its foot behind it, like the mascot on purxiy's hub. */
	private void drawMark(Draw d, long now) {
		float x = px(FRONT);
		float discY = py(FRONT);
		Logo.upright(d, x - MARK / 2f, discY + MARK * 0.04f - MARK, MARK, now);
	}

	/** The front tab's name, tagline and how many of its mods are on. */
	private void drawTitle(Draw d, float w, float top, long now) {
		MenuView.Tab tab = tabs.get(Math.max(0, front));
		float old = Paint.opacity(Paint.opacity() * Ease.OUT.apply((now - frontSince) / (double) Tokens.T_COLOR));
		float disc = FRONT_R * 2;
		Type.center(d, tab.name(), w / 2f, top, disc * 0.3f, Weight.BOLD, Tokens.TEXT);
		Type.center(d, tab.tagline(), w / 2f, top + disc * 0.55f, disc * 0.137f, Weight.MEDIUM, Tokens.TEXT_2);
		String count = count(tab);
		if (count != null) {
			float pillCap = disc * 0.125f;
			float ph = disc * 0.36f;
			float textW = Type.width(count, pillCap, Weight.MEDIUM);
			float pw = textW + ph * 1.35f;
			float x = w / 2f - pw / 2f;
			float y = top + disc * 0.94f;
			Paint.rect(d, x, y, x + pw, y + ph, ph / 2f, PILL);
			Paint.stroke(d, x, y, x + pw, y + ph, ph / 2f, 1, PILL_EDGE);
			Paint.circle(d, x + ph * 0.45f, y + ph / 2f, ph * 0.09f, Accent.get());
			Type.draw(d, count, x + ph * 0.75f, y + (ph - pillCap) / 2f, pillCap, Weight.MEDIUM, Tokens.TEXT_2);
		}
		Paint.opacity(old);
	}

	/** "9 mods · 2 on" for a category tab, or null for Profile. */
	private static String count(MenuView.Tab tab) {
		if (tab.category() == null) return null;
		int all = 0;
		int on = 0;
		for (Module module : Modules.ALL) {
			if (module.category != tab.category() || !module.toggleable()) continue;
			all++;
			if (module.enabled()) on++;
		}
		return all + (all == 1 ? " mod" : " mods") + " · " + on + " on";
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

	/** The disc under the mouse, nearest first, or -1. */
	private int discAt(float x, float y) {
		int best = -1;
		float bestDepth = -1;
		double angle = spin.value();
		for (int i = 0; i < tabs.size(); i++) {
			double a = angle + i * step;
			float depth = depth(a);
			if (Math.hypot(x - px(a), y - py(a)) <= radius(depth) && depth > bestDepth) {
				best = i;
				bestDepth = depth;
			}
		}
		return best;
	}

	public boolean mouseClicked(double x, double y, int button) {
		dragging = true;
		dragged = false;
		dragLastX = x / unit;
		dragLastAt = System.nanoTime();
		velocity = 0;
		return true;
	}

	public boolean mouseDragged(double x, double y) {
		if (!dragging) return false;
		double mx = x / unit;
		double dx = mx - dragLastX;
		if (Math.abs(dx) > 2) dragged = true;
		if (!dragged) return true;
		long now = System.nanoTime();
		double delta = -dx / RING_X;
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
			spinTo(Math.floorMod((int) Math.round((FRONT - fling) / step), tabs.size()));
			return true;
		}
		int disc = discAt((float) (x / unit), (float) (y / unit));
		if (disc < 0) return true;
		host.clickSound();
		if (disc != front()) spinTo(disc);
		else host.open(front());
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
