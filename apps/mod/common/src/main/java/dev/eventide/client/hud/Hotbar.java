package dev.eventide.client.hud;

import dev.eventide.client.Draw;
import dev.eventide.client.Hooks;
import dev.eventide.client.gui.Accent;
import dev.eventide.client.module.Modules;
import dev.eventide.client.ui.Anim;
import dev.eventide.client.ui.Ease;
import dev.eventide.client.ui.Paint;
import dev.eventide.client.ui.Tokens;
import dev.eventide.client.ui.Type;
import dev.eventide.client.ui.Type.Weight;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;

/**
 * The Hotbar mod: Minecraft's hotbar as one dark rounded bar with no slot borders, a light rounded
 * square gliding to the selected slot, the off hand in its own little square, and (if asked) the
 * server's address in a pill beside it. It keeps Minecraft's size and place, so hearts, hunger and
 * the experience bar sit where they always do.
 */
public final class Hotbar {
	private static final int WIDTH = 182;
	private static final int HEIGHT = 22;
	private static final Anim SELECTED = new Anim(0, 170, Ease.OUT);
	private static final float[] ARC = new float[29 * 2];
	private static boolean placed = false;

	private Hotbar() {
	}

	public static void draw(Draw d, Runnable original) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (!Modules.HOTBAR.enabled() || player == null || player.isSpectator()) {
			original.run();
			return;
		}
		Hooks.hotbarDrawn = true;
		int w = d.width();
		int h = d.height();
		float x1 = w / 2f - WIDTH / 2f;
		float y1 = h - HEIGHT - 1;
		float strength = (float) (Modules.HOTBAR_BACKGROUND.get() / 100.0);
		bar(d, x1, y1, x1 + WIDTH, y1 + HEIGHT, 7, strength);

		int selected = player.getInventory().getSelectedSlot();
		if (!placed) {
			SELECTED.snap(selected);
			placed = true;
		}
		SELECTED.set(selected);
		float sx = x1 + 1 + SELECTED.get() * 20;
		float sy = y1 + 1;
		boolean white = Modules.HOTBAR_SELECTED.is("White");
		Paint.shadow(d, sx, sy, sx + 20, sy + 20, 5, 0, 1, 5, 0, white ? 0x59000000 : Accent.glow(0.7f));
		if (white) {
			Paint.rect(d, sx, sy, sx + 20, sy + 20, 5, 0xF2F4F0FF);
		} else {
			Paint.rect(d, sx, sy, sx + 20, sy + 20, 5, Accent.primary());
			Paint.stroke(d, sx, sy, sx + 20, sy + 20, 5, Hud.hairline(d), 0x47FFFFFF);
		}

		for (int i = 0; i < 9; i++) {
			ItemStack stack = player.getInventory().getItem(i);
			if (stack.isEmpty()) continue;
			int ix = Math.round(x1) + 3 + i * 20;
			int iy = Math.round(y1) + 3;
			d.item(stack, ix, iy);
			d.itemOverlay(stack, ix, iy, null);
		}

		ItemStack offhand = player.getOffhandItem();
		boolean rightHanded = player.getMainArm() == HumanoidArm.RIGHT;
		if (!offhand.isEmpty()) {
			float ox = rightHanded ? x1 - 29 : x1 + WIDTH + 7;
			bar(d, ox, y1, ox + 22, y1 + HEIGHT, 7, strength);
			d.item(offhand, Math.round(ox) + 3, Math.round(y1) + 3);
			d.itemOverlay(offhand, Math.round(ox) + 3, Math.round(y1) + 3, null);
		}

		float right = x1 + WIDTH + 6;
		if (!rightHanded && !offhand.isEmpty()) right += 29;
		if (mc.options.attackIndicator().get() == AttackIndicatorStatus.HOTBAR) {
			float charge = player.getAttackStrengthScale(0);
			if (charge < 1) {
				float cx = right + 9;
				float cy = y1 + HEIGHT / 2f;
				Paint.circle(d, cx, cy, 8, Paint.fade(Tokens.GLASS_SOLID, strength));
				Paint.ring(d, cx, cy, 6, 1.6f, 0x33FFFFFF);
				arc(d, cx, cy, 6, charge);
			}
			right += 22;
		}
		if (Modules.HOTBAR_SERVER.get()) server(d, mc, right, y1 + HEIGHT / 2f, strength);
	}

	/** The dark rounded bar: violet glass with a hairline, like the rest of the HUD. */
	private static void bar(Draw d, float x1, float y1, float x2, float y2, float r, float strength) {
		Hud.glass(d, x1, y1, x2, y2, Modules.THEME_ROUNDED_HUD.get() ? r : 1.5f, Math.max(0.05f, strength));
	}

	/** How far the attack has charged, as a line round the ring. */
	private static void arc(Draw d, float cx, float cy, float r, float fraction) {
		int steps = Math.max(2, Math.round(28 * fraction));
		for (int i = 0; i <= steps; i++) {
			double a = -Math.PI / 2 + Math.PI * 2 * fraction * i / steps;
			ARC[i * 2] = cx + (float) Math.cos(a) * r;
			ARC[i * 2 + 1] = cy + (float) Math.sin(a) * r;
		}
		Paint.line(d, ARC, steps + 1, false, 1.6f, Accent.get());
	}

	/** The server's address in a small glass pill (or "Singleplayer" in your own world). */
	private static void server(Draw d, Minecraft mc, float x, float cy, float strength) {
		String address;
		String icon;
		ServerData server = mc.getCurrentServer();
		if (server != null && server.ip != null && !server.ip.isBlank()) {
			address = server.ip.endsWith(":25565") ? server.ip.substring(0, server.ip.length() - 6) : server.ip;
			icon = "globe";
		} else if (mc.getSingleplayerServer() != null) {
			address = "Singleplayer";
			icon = "house";
		} else {
			return;
		}
		float cap = Hud.SMALL;
		address = Type.fit(address, 120, cap, Weight.SEMIBOLD);
		float w = 6 + 7 + 4 + Type.width(address, cap, Weight.SEMIBOLD) + 7;
		float h = 14;
		float y = cy - h / 2f;
		Hud.glass(d, x, y, x + w, y + h, Modules.THEME_ROUNDED_HUD.get() ? h / 2f : 1.5f, Math.max(0.05f, strength));
		Paint.icon(d, icon, x + 6, cy - 3.5f, 7, Accent.text());
		Hud.text(d, address, x + 17, cy - cap / 2f, cap, Weight.SEMIBOLD, Tokens.TEXT, strength < 0.35f);
		Hooks.serverAddressDrawn = true;
	}
}
