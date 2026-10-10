package dev.threevhy.client.hud;

import dev.threevhy.client.Draw;

/** Draws in place of one of Minecraft's own HUD pieces; original draws Minecraft's version. */
@FunctionalInterface
public interface HudOverride {
	void draw(Draw d, Runnable original);
}
