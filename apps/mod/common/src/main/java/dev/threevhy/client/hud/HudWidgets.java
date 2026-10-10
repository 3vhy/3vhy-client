package dev.threevhy.client.hud;

import dev.threevhy.client.Draw;
import dev.threevhy.client.gui.SafeItems;
import dev.threevhy.client.hud.HudWidget.Anchor;
import dev.threevhy.client.module.Combo;
import dev.threevhy.client.module.JumpReset;
import dev.threevhy.client.module.Module;
import dev.threevhy.client.module.Modules;
import dev.threevhy.client.module.PopTracker;
import dev.threevhy.client.module.Target;
import dev.threevhy.client.module.TpsTracker;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** All HUD widgets, in drawing order. */
public final class HudWidgets {
	private HudWidgets() {
	}

	/** Green when good, yellow, orange, then red when bad. */
	static int qualityColor(double badness) {
		if (badness < 0.25) return 0xFF4ADE80;
		if (badness < 0.5) return 0xFFFFD166;
		if (badness < 0.75) return 0xFFFF9F43;
		return 0xFFFF6B81;
	}

	/** A small box with an optional label and a coloured value, like "137 ms". */
	private abstract static class StatWidget extends HudWidget {
		StatWidget(String id, Module module, Anchor anchor) {
			super(id, module, anchor);
		}

		abstract String label();

		abstract String value(Minecraft mc, boolean preview);

		int color(Minecraft mc) {
			return valueColor();
		}

		private String labelText() {
			return showLabel() ? label() : "";
		}

		@Override
		public int width(Minecraft mc) {
			String label = labelText();
			return 10 + (label.isEmpty() ? 0 : textWidth(mc, label) + 4) + textWidth(mc, value(mc, true));
		}

		@Override
		public int height(Minecraft mc) {
			return 14;
		}

		@Override
		public void draw(Draw d, int x, int y, Minecraft mc, boolean preview) {
			String label = labelText();
			String value = value(mc, preview);
			int w = 10 + (label.isEmpty() ? 0 : textWidth(mc, label) + 4) + textWidth(mc, value);
			panel(d, x, y, w, 14);
			boolean shadow = textShadow();
			int tx = x + 6;
			if (!label.isEmpty()) {
				d.text(label, tx, y + 3, Theme.MUTED, shadow);
				tx += textWidth(mc, label) + 4;
			}
			d.text(value, tx, y + 3, color(mc), shadow);
		}
	}

	public static final HudWidget FPS = new StatWidget("fps", Modules.FPS, Anchor.TOP_LEFT) {
		@Override
		String label() {
			return "FPS";
		}

		@Override
		String value(Minecraft mc, boolean preview) {
			return Integer.toString(mc.getFps());
		}
	};

	public static final HudWidget CPS = new StatWidget("cps", Modules.CPS, Anchor.TOP_LEFT) {
		@Override
		String label() {
			return "CPS";
		}

		@Override
		String value(Minecraft mc, boolean preview) {
			return CpsTracker.left() + " | " + CpsTracker.right();
		}
	};

	public static final HudWidget PING = new StatWidget("ping", Modules.PING, Anchor.TOP_LEFT) {
		@Override
		String label() {
			return "Ping";
		}

		private int latency(Minecraft mc) {
			ClientPacketListener connection = mc.getConnection();
			LocalPlayer player = mc.player;
			if (connection == null || player == null) return -1;
			PlayerInfo info = connection.getPlayerInfo(player.getUUID());
			return info == null ? -1 : info.getLatency();
		}

		@Override
		String value(Minecraft mc, boolean preview) {
			int latency = latency(mc);
			if (latency < 0) return preview ? "42 ms" : "-";
			return latency + " ms";
		}

		@Override
		int color(Minecraft mc) {
			int latency = latency(mc);
			if (!Modules.PING_COLOURS.get() || latency < 0) return valueColor();
			return qualityColor(latency / 200.0);
		}

		@Override
		public boolean visible(Minecraft mc) {
			return mc.getSingleplayerServer() == null;
		}
	};

	public static final HudWidget TPS = new StatWidget("tps", Modules.TPS, Anchor.TOP_LEFT) {
		@Override
		String label() {
			return "TPS";
		}

		@Override
		String value(Minecraft mc, boolean preview) {
			double tps = TpsTracker.tps();
			if (tps < 0) return preview ? "20.0" : "-";
			return String.format(Locale.ROOT, "%.1f", tps);
		}

		@Override
		int color(Minecraft mc) {
			double tps = TpsTracker.tps();
			if (!Modules.TPS_COLOURS.get() || tps < 0) return valueColor();
			return qualityColor((20 - tps) / 10.0);
		}
	};

	public static final HudWidget CLOCK = new StatWidget("clock", Modules.CLOCK, Anchor.TOP_LEFT) {
		@Override
		String label() {
			return "Time";
		}

		@Override
		String value(Minecraft mc, boolean preview) {
			boolean seconds = Modules.CLOCK_SECONDS.get();
			String pattern = Modules.CLOCK_24H.get() ? (seconds ? "HH:mm:ss" : "HH:mm")
					: (seconds ? "h:mm:ss a" : "h:mm a");
			return LocalTime.now().format(DateTimeFormatter.ofPattern(pattern, Locale.ROOT));
		}
	};

	public static final HudWidget COMBO = new StatWidget("combo", Modules.COMBO, Anchor.TOP_LEFT) {
		@Override
		String label() {
			return "Combo";
		}

		@Override
		String value(Minecraft mc, boolean preview) {
			int combo = Combo.count();
			return combo == 0 && preview ? "3 hits" : combo + (combo == 1 ? " hit" : " hits");
		}

		@Override
		public boolean visible(Minecraft mc) {
			return Combo.count() > 0;
		}
	};

	public static final HudWidget COORDS = new HudWidget("coords", Modules.COORDS, Anchor.TOP_LEFT) {
		@Override
		public int width(Minecraft mc) {
			return 10 + textWidth(mc, "XYZ -30000000 320 -30000000");
		}

		@Override
		public int height(Minecraft mc) {
			return Modules.COORDS_FACING.get() ? 24 : 14;
		}

		@Override
		public void draw(Draw d, int x, int y, Minecraft mc, boolean preview) {
			LocalPlayer player = mc.player;
			String xyz;
			String facing;
			if (player == null) {
				xyz = "120 64 -32";
				facing = "North";
			} else {
				BlockPos pos = player.blockPosition();
				xyz = pos.getX() + " " + pos.getY() + " " + pos.getZ();
				String dir = player.getDirection().toString();
				facing = dir.isEmpty() ? dir : Character.toUpperCase(dir.charAt(0)) + dir.substring(1);
			}
			panel(d, x, y, width(mc), height(mc));
			boolean shadow = textShadow();
			int labelW = showLabel() ? textWidth(mc, "Facing") + 4 : 0;
			if (showLabel()) d.text("XYZ", x + 6, y + 3, Theme.MUTED, shadow);
			d.text(xyz, x + 6 + labelW, y + 3, valueColor(), shadow);
			if (Modules.COORDS_FACING.get()) {
				if (showLabel()) d.text("Facing", x + 6, y + 13, Theme.MUTED, shadow);
				d.text(facing, x + 6 + labelW, y + 13, valueColor(), shadow);
			}
		}
	};

	public static final HudWidget KEYSTROKES = new HudWidget("keystrokes", Modules.KEYSTROKES, Anchor.TOP_LEFT) {
		private static final int K = 20;
		private static final int GAP = 2;

		@Override
		public int width(Minecraft mc) {
			return K * 3 + GAP * 2;
		}

		@Override
		public int height(Minecraft mc) {
			int h = K * 2 + GAP;
			if (Modules.KEYSTROKES_MOUSE.get()) h += GAP + 22;
			if (Modules.KEYSTROKES_SPACE.get()) h += GAP + 10;
			return h;
		}

		@Override
		public void draw(Draw d, int x, int y, Minecraft mc, boolean preview) {
			var o = mc.options;
			key(d, mc, x + K + GAP, y, K, K, "W", o.keyUp);
			int row = y + K + GAP;
			key(d, mc, x, row, K, K, "A", o.keyLeft);
			key(d, mc, x + K + GAP, row, K, K, "S", o.keyDown);
			key(d, mc, x + (K + GAP) * 2, row, K, K, "D", o.keyRight);
			row += K + GAP;
			if (Modules.KEYSTROKES_MOUSE.get()) {
				int half = (width(mc) - GAP) / 2;
				mouse(d, mc, x, row, half, 22, "LMB", CpsTracker.left(), o.keyAttack);
				mouse(d, mc, x + half + GAP, row, half, 22, "RMB", CpsTracker.right(), o.keyUse);
				row += 22 + GAP;
			}
			if (Modules.KEYSTROKES_SPACE.get()) {
				boolean jump = o.keyJump.isDown();
				d.fill(x, row, x + width(mc), row + 10, jump ? Theme.KEY_DOWN : backgroundColor());
				int barW = width(mc) / 3;
				d.fill(x + (width(mc) - barW) / 2, row + 4, x + (width(mc) + barW) / 2, row + 6,
						jump ? Theme.NIGHT : Theme.SKY);
			}
		}

		private void key(Draw d, Minecraft mc, int x, int y, int w, int h, String label, KeyMapping key) {
			boolean down = key.isDown();
			d.fill(x, y, x + w, y + h, down ? Theme.KEY_DOWN : backgroundColor());
			int tw = textWidth(mc, label);
			d.text(label, x + (w - tw) / 2, y + (h - 8) / 2, down ? Theme.NIGHT : Theme.ICE, !down && textShadow());
		}

		private void mouse(Draw d, Minecraft mc, int x, int y, int w, int h, String label, int cps, KeyMapping key) {
			boolean down = key.isDown();
			d.fill(x, y, x + w, y + h, down ? Theme.KEY_DOWN : backgroundColor());
			int color = down ? Theme.NIGHT : Theme.ICE;
			int tw = textWidth(mc, label);
			d.text(label, x + (w - tw) / 2, y + 3, color, !down && textShadow());
			String c = cps + " CPS";
			d.push();
			float s = 0.75f;
			int cw = textWidth(mc, c);
			d.translate(x + (w - cw * s) / 2f, y + 13);
			d.scale(s);
			d.text(c, 0, 0, down ? Theme.NIGHT : Theme.SKY, !down && textShadow());
			d.pop();
		}
	};

	public static final HudWidget ARMOR = new HudWidget("armor", Modules.ARMOR, Anchor.BOTTOM_RIGHT) {
		private static final EquipmentSlot[] ARMOR_SLOTS = {
				EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
		};
		private static final EquipmentSlot[] WITH_HAND = {
				EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND
		};

		private EquipmentSlot[] slots() {
			return Modules.ARMOR_HAND.get() ? WITH_HAND : ARMOR_SLOTS;
		}

		private boolean row() {
			return Modules.ARMOR_LAYOUT.is("Row");
		}

		private int labelWidth(Minecraft mc) {
			return Modules.ARMOR_DURABILITY.is("Hidden") ? 0 : textWidth(mc, Modules.ARMOR_DURABILITY.is("Points") ? "2031" : "100%");
		}

		@Override
		public int width(Minecraft mc) {
			if (row()) return slots().length * 20 + 4;
			return 20 + 2 + labelWidth(mc) + 6;
		}

		@Override
		public int height(Minecraft mc) {
			if (row()) return Modules.ARMOR_DURABILITY.is("Hidden") ? 20 : 28;
			return slots().length * 18 + 4;
		}

		@Override
		protected int[] defaultPosition(Minecraft mc, int screenWidth, int screenHeight) {
			if (row()) return new int[] {(screenWidth - width(mc)) / 2 + 100, screenHeight - 38 - height(mc)};
			return super.defaultPosition(mc, screenWidth, screenHeight);
		}

		@Override
		protected boolean usesAnchor() {
			return !row();
		}

		@Override
		public void draw(Draw d, int x, int y, Minecraft mc, boolean preview) {
			panel(d, x, y, width(mc), height(mc));
			LocalPlayer player = mc.player;
			EquipmentSlot[] slots = slots();
			for (int i = 0; i < slots.length; i++) {
				int ix = row() ? x + 4 + i * 20 : x + 4;
				int iy = row() ? y + 2 : y + 2 + i * 18;
				ItemStack stack = player == null ? ItemStack.EMPTY : player.getItemBySlot(slots[i]);
				if (stack.isEmpty()) {
					if (preview) d.text("-", ix + 6, iy + 5, Theme.MUTED, false);
					continue;
				}
				d.item(stack, ix, iy);
				String label = durability(stack);
				if (label == null) continue;
				int color = durabilityColor(stack);
				if (row()) {
					d.push();
					d.translate(ix + 8 - textWidth(mc, label) * 0.375f, iy + 18);
					d.scale(0.75f);
					d.text(label, 0, 0, color, true);
					d.pop();
				} else {
					d.text(label, ix + 18, iy + 5, color, true);
				}
			}
		}

		private String durability(ItemStack stack) {
			if (Modules.ARMOR_DURABILITY.is("Hidden") || !stack.isDamageableItem() || stack.getMaxDamage() <= 0) {
				return null;
			}
			int left = stack.getMaxDamage() - stack.getDamageValue();
			if (Modules.ARMOR_DURABILITY.is("Points")) return Integer.toString(left);
			return Math.round(left * 100f / stack.getMaxDamage()) + "%";
		}

		private int durabilityColor(ItemStack stack) {
			float left = (stack.getMaxDamage() - stack.getDamageValue()) / (float) stack.getMaxDamage();
			return left > 0.5f ? Theme.GOOD : left > 0.2f ? Theme.WARN : Theme.BAD;
		}
	};

	public static final HudWidget ITEMS = new HudWidget("items", Modules.ITEMS, Anchor.BOTTOM_RIGHT) {
		private List<Item[]> items() {
			List<Item[]> list = new ArrayList<>();
			if (Modules.ITEMS_TOTEMS.get()) list.add(new Item[] {Items.TOTEM_OF_UNDYING});
			if (Modules.ITEMS_CRYSTALS.get()) list.add(new Item[] {Items.END_CRYSTAL});
			if (Modules.ITEMS_OBSIDIAN.get()) list.add(new Item[] {Items.OBSIDIAN});
			if (Modules.ITEMS_XP.get()) list.add(new Item[] {Items.EXPERIENCE_BOTTLE});
			if (Modules.ITEMS_GAPPLES.get()) list.add(new Item[] {Items.ENCHANTED_GOLDEN_APPLE, Items.GOLDEN_APPLE});
			if (Modules.ITEMS_PEARLS.get()) list.add(new Item[] {Items.ENDER_PEARL});
			if (Modules.ITEMS_ANCHORS.get()) {
				list.add(new Item[] {Items.RESPAWN_ANCHOR});
				list.add(new Item[] {Items.GLOWSTONE});
			}
			return list;
		}

		@Override
		public int width(Minecraft mc) {
			return 4 + Math.max(1, items().size()) * 20;
		}

		@Override
		public int height(Minecraft mc) {
			return 20;
		}

		@Override
		public void draw(Draw d, int x, int y, Minecraft mc, boolean preview) {
			List<Item[]> items = items();
			panel(d, x, y, width(mc), 20);
			LocalPlayer player = mc.player;
			for (int i = 0; i < items.size(); i++) {
				Item[] group = items.get(i);
				int count = 0;
				if (player != null) {
					for (Item item : group) count += count(player.getInventory(), item);
				} else if (preview) {
					count = 8;
				}
				ItemStack icon = SafeItems.stack(group[0]);
				int ix = x + 4 + i * 20;
				String label = Integer.toString(count);
				if (icon != null) {
					d.item(icon, ix, y + 2);
					d.itemOverlay(icon, ix, y + 2, label);
				} else {
					d.text(label, ix + 17 - textWidth(mc, label), y + 11, Theme.ICE, true);
				}
				if (count == 0) d.fill(ix, y + 2, ix + 16, y + 18, background() ? 0x80060E2B : 0x60000000);
			}
		}

		private int count(Inventory inventory, Item item) {
			int total = 0;
			for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
				ItemStack stack = inventory.getItem(slot);
				if (stack.is(item)) total += stack.getCount();
			}
			return total;
		}
	};

	public static final HudWidget POPS = new HudWidget("pops", Modules.POPS, Anchor.TOP_RIGHT) {
		private static final int MAX = 5;

		@Override
		public int width(Minecraft mc) {
			return 110;
		}

		private int rows() {
			return Math.max(2, Math.min(MAX, PopTracker.top(MAX).size()));
		}

		@Override
		public int height(Minecraft mc) {
			return 14 + rows() * 10 + 2;
		}

		@Override
		public boolean visible(Minecraft mc) {
			return !PopTracker.top(1).isEmpty();
		}

		@Override
		public void draw(Draw d, int x, int y, Minecraft mc, boolean preview) {
			List<PopTracker.Entry> entries = PopTracker.top(MAX);
			panel(d, x, y, width(mc), height(mc));
			d.text("Totem pops", x + 6, y + 3, Theme.MUTED, textShadow());
			if (entries.isEmpty() && preview) {
				d.text("Steve", x + 6, y + 14, Theme.ICE, false);
				d.text("3", x + width(mc) - 12, y + 14, Theme.CYAN, false);
				d.text("Alex", x + 6, y + 24, Theme.ICE, false);
				d.text("1", x + width(mc) - 12, y + 24, Theme.CYAN, false);
				return;
			}
			for (int i = 0; i < entries.size(); i++) {
				PopTracker.Entry entry = entries.get(i);
				int rowY = y + 14 + i * 10;
				String name = entry.name();
				while (textWidth(mc, name) > width(mc) - 30 && name.length() > 3) {
					name = name.substring(0, name.length() - 1);
				}
				d.text(name, x + 6, rowY, Theme.ICE, textShadow());
				String pops = Integer.toString(entry.pops());
				d.text(pops, x + width(mc) - 6 - textWidth(mc, pops), rowY, Theme.CYAN, textShadow());
			}
		}
	};

	public static final HudWidget TARGET = new HudWidget("target", Modules.TARGET_HUD, Anchor.BELOW_CROSSHAIR) {
		private static final int W = 150;
		private static final EquipmentSlot[] SLOTS = {
				EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
				EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND
		};

		@Override
		public int width(Minecraft mc) {
			return W;
		}

		@Override
		public int height(Minecraft mc) {
			return Modules.TARGET_ARMOR.get() ? 48 : 28;
		}

		@Override
		public boolean visible(Minecraft mc) {
			return Target.get() != null;
		}

		@Override
		public void draw(Draw d, int x, int y, Minecraft mc, boolean preview) {
			LivingEntity target = Target.get();
			String name = target == null ? "Steve" : target.getName().getString();
			float health = target == null ? 14.5f : target.getHealth();
			float max = target == null ? 20f : Math.max(1f, target.getMaxHealth());
			float absorption = target == null ? 4f : target.getAbsorptionAmount();
			double distance = target == null || mc.player == null ? 3.2 : mc.player.distanceTo(target);
			int h = height(mc);

			panel(d, x, y, W, h);
			while (textWidth(mc, name) > W - 50 && name.length() > 3) {
				name = name.substring(0, name.length() - 1);
			}
			d.text(name, x + 6, y + 4, Theme.ICE, textShadow());
			String dist = String.format(Locale.ROOT, "%.1fm", distance);
			d.text(dist, x + W - 6 - textWidth(mc, dist), y + 4, Theme.MUTED, textShadow());

			int barX = x + 6;
			int barY = y + 15;
			int barW = W - 12;
			float total = max + absorption;
			int healthW = Math.round(barW * Math.min(health, max) / total);
			int absorbW = Math.round(barW * absorption / total);
			d.fill(barX, barY, barX + barW, barY + 5, 0xFF0A1846);
			int healthColor = health / max > 0.5f ? Theme.GOOD : health / max > 0.25f ? Theme.WARN : Theme.BAD;
			d.fill(barX, barY, barX + healthW, barY + 5, healthColor);
			if (absorbW > 0) d.fill(barX + healthW, barY, barX + healthW + absorbW, barY + 5, 0xFFFFC94D);
			String hp = trimHealth(health) + (absorption > 0 ? " +" + trimHealth(absorption) : "") + " HP";
			d.push();
			d.translate(barX + barW - textWidth(mc, hp) * 0.75f, barY + 7);
			d.scale(0.75f);
			d.text(hp, 0, 0, Theme.SKY, false);
			d.pop();

			if (!Modules.TARGET_ARMOR.get()) return;
			int itemY = y + h - 19;
			for (int i = 0; i < SLOTS.length; i++) {
				ItemStack stack = target == null ? ItemStack.EMPTY : target.getItemBySlot(SLOTS[i]);
				int ix = x + 6 + i * 17 + (i >= 4 ? 6 : 0);
				if (stack.isEmpty()) {
					d.fill(ix + 1, itemY + 1, ix + 15, itemY + 15, 0x40060E2B);
					continue;
				}
				d.item(stack, ix, itemY);
				if (SLOTS[i] == EquipmentSlot.OFFHAND && stack.is(Items.TOTEM_OF_UNDYING)) {
					d.outline(ix - 1, itemY - 1, ix + 17, itemY + 17, Theme.CYAN);
				}
			}
		}

		private String trimHealth(float value) {
			return value == Math.floor(value) ? Integer.toString((int) value) : String.format(Locale.ROOT, "%.1f", value);
		}
	};

	public static final HudWidget JUMP_RESET = new HudWidget("jump_reset", Modules.JUMP_RESET, Anchor.ABOVE_HOTBAR) {
		@Override
		public int width(Minecraft mc) {
			return 12 + textWidth(mc, "Jump reset  Perfect +1t");
		}

		@Override
		public int height(Minecraft mc) {
			return 14;
		}

		@Override
		public boolean visible(Minecraft mc) {
			return JumpReset.result() != null;
		}

		@Override
		public void draw(Draw d, int x, int y, Minecraft mc, boolean preview) {
			JumpReset.Result result = JumpReset.result();
			String text = result == null ? "Perfect" : result.text();
			int color = result == null ? 0xFF4ADE80 : result.color();
			String label = showLabel() ? "Jump reset" : "";
			int w = 10 + (label.isEmpty() ? 0 : textWidth(mc, label) + 4) + textWidth(mc, text);
			int cx = x + (width(mc) - w) / 2;
			panel(d, cx, y, w, 14);
			int tx = cx + 6;
			if (!label.isEmpty()) {
				d.text(label, tx, y + 3, Theme.MUTED, textShadow());
				tx += textWidth(mc, label) + 4;
			}
			d.text(text, tx, y + 3, color, textShadow());
		}
	};

	public static final HudWidget TOTEM_WARNING = new HudWidget("totem_warning", Modules.TOTEM_WARNING, Anchor.ABOVE_CROSSHAIR) {
		private static final int H = 20;

		private int spare(Minecraft mc) {
			LocalPlayer player = mc.player;
			if (player == null) return 0;
			Inventory inventory = player.getInventory();
			int total = 0;
			for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
				ItemStack stack = inventory.getItem(slot);
				if (stack.is(Items.TOTEM_OF_UNDYING)) total += stack.getCount();
			}
			return total;
		}

		private String message(int spare) {
			return "No totem in offhand  (" + spare + " in inventory)";
		}

		@Override
		public int width(Minecraft mc) {
			return 26 + textWidth(mc, message(64));
		}

		@Override
		public int height(Minecraft mc) {
			return H;
		}

		@Override
		public boolean visible(Minecraft mc) {
			LocalPlayer player = mc.player;
			if (player == null || player.isSpectator() || player.isCreative()) return false;
			if (player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) return false;
			if (player.getMainHandItem().is(Items.TOTEM_OF_UNDYING)) return false;
			return spare(mc) > 0;
		}

		@Override
		public void draw(Draw d, int x, int y, Minecraft mc, boolean preview) {
			int spare = mc.player == null || preview && !visible(mc) ? 4 : spare(mc);
			String text = message(spare);
			int w = 26 + textWidth(mc, text);
			double pulse = (Math.sin(System.currentTimeMillis() / 160.0) + 1) / 2;
			int alpha = 0x90 + (int) (pulse * 0x50);
			d.fill(x, y, x + w, y + H, (alpha << 24) | 0x5A0A1E);
			d.fill(x, y, x + 1, y + H, Theme.BAD);
			ItemStack totem = SafeItems.stack(Items.TOTEM_OF_UNDYING);
			if (totem != null) d.item(totem, x + 5, y + 2);
			d.text(text, x + 24, y + 6, Theme.ICE, false);
		}
	};

	public static final HudWidget SCOREBOARD = new ScoreboardWidget();

	/** Drawing order; widgets that share an anchor stack in this order (upwards at the bottom). */
	public static final List<HudWidget> ALL = List.of(FPS, CPS, PING, TPS, CLOCK, COMBO, COORDS, KEYSTROKES, ITEMS,
			ARMOR, POPS, TARGET, JUMP_RESET, TOTEM_WARNING, SCOREBOARD);
}
