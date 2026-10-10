package dev.threevhy.client.hud;

import dev.threevhy.client.Draw;
import dev.threevhy.client.compat.Compat;
import dev.threevhy.client.module.Modules;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.BlankFormat;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

/**
 * The server's sidebar scoreboard as a HUD widget. Minecraft still draws it; 3vhy only moves it
 * to where the player dragged it, can hide its red numbers, and can hide it altogether.
 */
public final class ScoreboardWidget extends HudWidget {
	private static final int SAMPLE_W = 96;
	private static final int SAMPLE_H = 64;

	ScoreboardWidget() {
		super("scoreboard", Modules.SCOREBOARD, Anchor.MIDDLE_RIGHT);
	}

	/** The objective Minecraft shows in the sidebar for this player, if any. */
	public static Objective sidebarObjective(Minecraft mc) {
		if (mc.level == null || mc.player == null) return null;
		Scoreboard scoreboard = mc.level.getScoreboard();
		Objective objective = null;
		PlayerTeam team = scoreboard.getPlayersTeam(mc.player.getScoreboardName());
		if (team != null) {
			DisplaySlot slot = DisplaySlot.teamColorToSlot(team.getColor());
			if (slot != null) objective = scoreboard.getDisplayObjective(slot);
		}
		return objective != null ? objective : scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
	}

	/** Where Minecraft draws the sidebar: {x1, y1, x2, y2}, or null when there isn't one. */
	private static int[] vanillaBounds(Minecraft mc, int screenWidth, int screenHeight) {
		Objective objective = sidebarObjective(mc);
		if (objective == null) return null;
		Scoreboard scoreboard = objective.getScoreboard();
		NumberFormat format = objective.numberFormatOrDefault(StyledFormat.SIDEBAR_DEFAULT);
		boolean hideNumbers = Modules.SCOREBOARD_HIDE_NUMBERS.get();
		int widest = mc.font.width(objective.getDisplayName());
		int colon = mc.font.width(": ");
		int lines = 0;
		for (PlayerScoreEntry entry : scoreboard.listPlayerScores(objective)) {
			if (entry.isHidden()) continue;
			if (lines >= 15) break;
			lines++;
			Component name = PlayerTeam.formatNameForTeam(scoreboard.getPlayersTeam(entry.owner()), entry.ownerName());
			int scoreWidth = hideNumbers ? 0 : mc.font.width(entry.formatValue(format));
			widest = Math.max(widest, mc.font.width(name) + (scoreWidth > 0 ? colon + scoreWidth : 0));
		}
		int bottom = screenHeight / 2 + lines * 9 / 3;
		int left = screenWidth - widest - 3;
		return new int[] {left - 2, bottom - lines * 9 - 10, screenWidth - 1, bottom};
	}

	@Override
	public boolean scaled() {
		return false;
	}

	@Override
	public boolean visible(Minecraft mc) {
		return sidebarObjective(mc) != null;
	}

	@Override
	public int width(Minecraft mc) {
		int[] bounds = vanillaBounds(mc, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
		return bounds == null ? SAMPLE_W : bounds[2] - bounds[0];
	}

	@Override
	public int height(Minecraft mc) {
		int[] bounds = vanillaBounds(mc, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
		return bounds == null ? SAMPLE_H : bounds[3] - bounds[1];
	}

	@Override
	protected int[] defaultPosition(Minecraft mc, int screenWidth, int screenHeight) {
		int[] bounds = vanillaBounds(mc, screenWidth, screenHeight);
		if (bounds == null) return new int[] {screenWidth - SAMPLE_W - 1, (screenHeight - SAMPLE_H) / 2};
		return new int[] {bounds[0], bounds[1]};
	}

	/** How far the player moved the scoreboard from where Minecraft puts it. */
	private int[] offset(Minecraft mc, int screenWidth, int screenHeight) {
		int[] bounds = vanillaBounds(mc, screenWidth, screenHeight);
		if (bounds == null || storedPosition() == null) return new int[] {0, 0};
		int[] position = position(mc, screenWidth, screenHeight);
		return new int[] {position[0] - bounds[0], position[1] - bounds[1]};
	}

	/** Called in place of Minecraft's own scoreboard drawing. */
	public void drawVanilla(Draw d, Runnable original, boolean editing) {
		Minecraft mc = Minecraft.getInstance();
		if (!module.enabled() || (!editing && mc.screen instanceof dev.threevhy.client.gui.HubScreen)) return;
		dev.threevhy.client.Hooks.scoreboardRan = true;
		int[] offset = offset(mc, d.width(), d.height());
		d.push();
		d.translate(offset[0], offset[1]);
		withNumbersHidden(mc, original);
		d.pop();
	}

	/** Runs Minecraft's scoreboard drawing with the score numbers blanked when the player wants that. */
	private static void withNumbersHidden(Minecraft mc, Runnable draw) {
		Objective objective = Modules.SCOREBOARD_HIDE_NUMBERS.get() ? sidebarObjective(mc) : null;
		if (objective == null) {
			draw.run();
			return;
		}
		NumberFormat previous = objective.numberFormat();
		objective.setNumberFormat(BlankFormat.INSTANCE);
		try {
			draw.run();
		} finally {
			objective.setNumberFormat(previous);
		}
	}

	@Override
	public void draw(Draw d, int x, int y, Minecraft mc, boolean preview) {
		if (!preview) return;
		Objective objective = sidebarObjective(mc);
		if (objective != null) {
			drawVanilla(d, () -> Compat.drawScoreboard(d, objective), true);
			return;
		}
		// A sample, so the scoreboard can be placed before joining a server that has one
		d.fill(x, y, x + SAMPLE_W, y + 10, 0x66000000);
		d.fill(x, y + 10, x + SAMPLE_W, y + SAMPLE_H, 0x4C000000);
		String title = "Scoreboard";
		d.text(title, x + (SAMPLE_W - mc.font.width(title)) / 2, y + 1, Theme.ICE, false);
		String[] lines = {"Kills: 29", "Deaths: 17", "Online: 272", "", "your.server"};
		for (int i = 0; i < lines.length; i++) {
			d.text(lines[i], x + 3, y + 12 + i * 10, Theme.SKY, false);
		}
	}
}
