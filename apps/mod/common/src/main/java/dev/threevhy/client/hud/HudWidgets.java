package dev.threevhy.client.hud;

import dev.threevhy.client.Draw;
import dev.threevhy.client.module.Modules;
import dev.threevhy.client.module.PopTracker;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** All HUD widgets, in drawing order. */
public final class HudWidgets {
	private HudWidgets() {
	}

	// ---------- Single-line stats ----------

	/** A small panel: label in sky blue, value in white. */
	private abstract static class StatWidget extends HudWidget {
		StatWidget(String id, dev.threevhy.client.module.Module module, double x, double y) {
			super(id, module, x, y);
		}

		abstract String label();

		abstract String value(Minecraft mc, boolean preview);

		@Override
		public int width(Minecraft mc) {
			return 10 + textWidth(mc, label()) + 4 + textWidth(mc, value(mc, true));
		}

		@Override
		public int height(Minecraft mc) {
			return 14;
		}

		@Override
		public void draw(Draw d, int x, int y, Minecraft mc, boolean preview) {
			String label = label();
			String value = value(mc, preview);
			int w = 10 + textWidth(mc, label) + 4 + textWidth(mc, value);
			panel(d, x, y, w, 14);
			d.text(label, x + 6, y + 3, Theme.SKY, false);
			d.text(value, x + 6 + textWidth(mc, label) + 4, y + 3, Theme.ICE, false);
		}
	}

	public static final HudWidget FPS = new StatWidget("fps", Modules.FPS, 0.0, 0.0) {
		@Override
		String label() {
			return "FPS";
		}

		@Override
		String value(Minecraft mc, boolean preview) {
			return Integer.toString(mc.getFps());
		}
	};

	public static final HudWidget CPS = new StatWidget("cps", Modules.CPS, 0.0, 0.045) {
		@Override
		String label() {
			return "CPS";
		}

		@Override
		String value(Minecraft mc, boolean preview) {
			return CpsTracker.left() + " | " + CpsTracker.right();
		}
	};

	public static final HudWidget PING = new StatWidget("ping", Modules.PING, 0.0, 0.09) {
		@Override
		String label() {
			return "Ping";
		}

		@Override
		String value(Minecraft mc, boolean preview) {
			ClientPacketListener connection = mc.getConnection();
			LocalPlayer player = mc.player;
			if (connection == null || player == null) {
				return preview ? "42 ms" : "-";
			}
			PlayerInfo info = connection.getPlayerInfo(player.getUUID());
			return info == null ? "-" : info.getLatency() + " ms";
		}

		@Override
		public boolean visible(Minecraft mc) {
			return mc.getSingleplayerServer() == null;
		}
	};

	public static final HudWidget COORDS = new HudWidget("coords", Modules.COORDS, 0.0, 0.135) {
		@Override
		public int width(Minecraft mc) {
			return 10 + textWidth(mc, "XYZ -30000000 320 -30000000");
		}

		@Override
		public int height(Minecraft mc) {
			return 24;
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
			int w = width(mc);
			panel(d, x, y, w, 24);
			d.text("XYZ", x + 6, y + 3, Theme.SKY, false);
			d.text(xyz, x + 6 + textWidth(mc, "XYZ") + 4, y + 3, Theme.ICE, false);
			d.text("Facing", x + 6, y + 13, Theme.SKY, false);
			d.text(facing, x + 6 + textWidth(mc, "Facing") + 4, y + 13, Theme.ICE, false);
		}
	};

	// ---------- Keystrokes ----------

	public static final HudWidget KEYSTROKES = new HudWidget("keystrokes", Modules.KEYSTROKES, 0.0, 0.32) {
		private static final int K = 20;
		private static final int GAP = 2;

		@Override
		public int width(Minecraft mc) {
			return K * 3 + GAP * 2;
		}

		@Override
		public int height(Minecraft mc) {
			return K * 2 + GAP + GAP + 22 + GAP + 10;
		}

		@Override
		public void draw(Draw d, int x, int y, Minecraft mc, boolean preview) {
			var o = mc.options;
			key(d, mc, x + K + GAP, y, K, K, "W", o.keyUp);
			int row2 = y + K + GAP;
			key(d, mc, x, row2, K, K, "A", o.keyLeft);
			key(d, mc, x + K + GAP, row2, K, K, "S", o.keyDown);
			key(d, mc, x + (K + GAP) * 2, row2, K, K, "D", o.keyRight);
			int row3 = row2 + K + GAP;
			int half = (width(mc) - GAP) / 2;
			mouse(d, mc, x, row3, half, 22, "LMB", CpsTracker.left(), o.keyAttack);
			mouse(d, mc, x + half + GAP, row3, half, 22, "RMB", CpsTracker.right(), o.keyUse);
			int row4 = row3 + 22 + GAP;
			boolean jump = o.keyJump.isDown();
			d.fill(x, row4, x + width(mc), row4 + 10, jump ? Theme.KEY_DOWN : Theme.KEY_IDLE);
			int barW = width(mc) / 3;
			d.fill(x + (width(mc) - barW) / 2, row4 + 4, x + (width(mc) + barW) / 2, row4 + 6,
					jump ? Theme.NIGHT : Theme.SKY);
		}

		private void key(Draw d, Minecraft mc, int x, int y, int w, int h, String label, KeyMapping key) {
			boolean down = key.isDown();
			d.fill(x, y, x + w, y + h, down ? Theme.KEY_DOWN : Theme.KEY_IDLE);
			if (!down) d.fill(x, y + h - 1, x + w, y + h, 0x60000000);
			int tw = textWidth(mc, label);
			d.text(label, x + (w - tw) / 2, y + (h - 8) / 2, down ? Theme.NIGHT : Theme.ICE, false);
		}

		private void mouse(Draw d, Minecraft mc, int x, int y, int w, int h, String label, int cps, KeyMapping key) {
			boolean down = key.isDown();
			d.fill(x, y, x + w, y + h, down ? Theme.KEY_DOWN : Theme.KEY_IDLE);
			int color = down ? Theme.NIGHT : Theme.ICE;
			int tw = textWidth(mc, label);
			d.text(label, x + (w - tw) / 2, y + 3, color, false);
			String c = cps + " CPS";
			d.push();
			float s = 0.75f;
			int cw = textWidth(mc, c);
			d.translate(x + (w - cw * s) / 2f, y + 13);
			d.scale(s);
			d.text(c, 0, 0, down ? Theme.NIGHT : Theme.SKY, false);
			d.pop();
		}
	};

	// ---------- Armor ----------

	public static final HudWidget ARMOR = new HudWidget("armor", Modules.ARMOR, 1.0, 0.55) {
		private static final EquipmentSlot[] SLOTS = {
				EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
		};

		@Override
		public int width(Minecraft mc) {
			return 20 + 2 + textWidth(mc, "100%") + 6;
		}

		@Override
		public int height(Minecraft mc) {
			return SLOTS.length * 18 + 4;
		}

		@Override
		public void draw(Draw d, int x, int y, Minecraft mc, boolean preview) {
			panel(d, x, y, width(mc), height(mc));
			LocalPlayer player = mc.player;
			for (int i = 0; i < SLOTS.length; i++) {
				int rowY = y + 2 + i * 18;
				ItemStack stack = player == null ? ItemStack.EMPTY : player.getItemBySlot(SLOTS[i]);
				if (stack.isEmpty()) {
					if (preview) d.text("-", x + 10, rowY + 5, Theme.MUTED, false);
					continue;
				}
				d.item(stack, x + 4, rowY);
				if (stack.isDamageableItem() && stack.getMaxDamage() > 0) {
					int left = stack.getMaxDamage() - stack.getDamageValue();
					int percent = Math.round(left * 100f / stack.getMaxDamage());
					int color = percent > 50 ? Theme.GOOD : percent > 20 ? Theme.WARN : Theme.BAD;
					d.text(percent + "%", x + 22, rowY + 5, color, true);
				}
			}
		}
	};

	// ---------- PvP item counter ----------

	public static final HudWidget ITEMS = new HudWidget("items", Modules.ITEMS, 1.0, 0.82) {
		private final List<Item> items = List.of(
				Items.TOTEM_OF_UNDYING, Items.END_CRYSTAL, Items.OBSIDIAN, Items.EXPERIENCE_BOTTLE,
				Items.ENCHANTED_GOLDEN_APPLE);

		@Override
		public int width(Minecraft mc) {
			return 4 + items.size() * 20;
		}

		@Override
		public int height(Minecraft mc) {
			return 20;
		}

		@Override
		public void draw(Draw d, int x, int y, Minecraft mc, boolean preview) {
			panel(d, x, y, width(mc), 20);
			LocalPlayer player = mc.player;
			for (int i = 0; i < items.size(); i++) {
				Item item = items.get(i);
				int count = player == null ? (preview ? 8 : 0) : count(player.getInventory(), item);
				ItemStack icon = new ItemStack(item);
				int ix = x + 4 + i * 20;
				d.item(icon, ix, y + 2);
				String label = Integer.toString(count);
				d.itemOverlay(icon, ix, y + 2, label);
				if (count == 0) {
					d.fill(ix, y + 2, ix + 16, y + 18, 0x80060E2B);
				}
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

	// ---------- Totem pops ----------

	public static final HudWidget POPS = new HudWidget("pops", Modules.POPS, 1.0, 0.0) {
		private static final int MAX = 5;

		@Override
		public int width(Minecraft mc) {
			return 110;
		}

		@Override
		public int height(Minecraft mc) {
			return 14 + MAX * 10 + 2;
		}

		@Override
		public boolean visible(Minecraft mc) {
			return !PopTracker.top(1).isEmpty();
		}

		@Override
		public void draw(Draw d, int x, int y, Minecraft mc, boolean preview) {
			List<PopTracker.Entry> entries = PopTracker.top(MAX);
			int rows = Math.max(entries.size(), preview ? 2 : 0);
			int h = 14 + rows * 10 + 2;
			panel(d, x, y, width(mc), h);
			d.text("Totem pops", x + 6, y + 3, Theme.SKY, false);
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
				d.text(name, x + 6, rowY, Theme.ICE, false);
				String pops = Integer.toString(entry.pops());
				d.text(pops, x + width(mc) - 6 - textWidth(mc, pops), rowY, Theme.CYAN, false);
			}
		}
	};

	public static final List<HudWidget> ALL = List.of(FPS, CPS, PING, COORDS, KEYSTROKES, ARMOR, ITEMS, POPS);
}
