package dev.threevhy.client;

import net.minecraft.client.Minecraft;

/**
 * Small shared helpers, plus flags the mixins set when they run. The in-game test checks the
 * flags to prove each hook works on every Minecraft version 3vhy supports.
 */
public final class Hooks {
	public static volatile boolean titleHookRan = false;
	public static volatile boolean fovHookRan = false;
	public static volatile boolean entityEventHookRan = false;

	private Hooks() {
	}

	/** The GLFW handle of the game window, or 0 before the window exists. */
	public static long windowHandle() {
		Minecraft mc = Minecraft.getInstance();
		if (mc == null || mc.getWindow() == null) return 0;
		return mc.getWindow().handle();
	}
}
