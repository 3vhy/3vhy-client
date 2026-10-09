package dev.threevhy.client.hud;

import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * Counts mouse clicks in the last second. Reads the mouse buttons once per frame, which is
 * accurate for normal, jitter and butterfly clicking at playable frame rates.
 */
public final class CpsTracker {
	private static final Deque<Long> LEFT = new ArrayDeque<>();
	private static final Deque<Long> RIGHT = new ArrayDeque<>();
	private static boolean leftWasDown = false;
	private static boolean rightWasDown = false;

	private CpsTracker() {
	}

	/** Called every frame from the HUD. */
	public static void poll(Minecraft mc) {
		long window = GLFW.glfwGetCurrentContext();
		long now = System.currentTimeMillis();
		boolean inGame = mc.screen == null && window != 0;
		boolean left = inGame && GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		boolean right = inGame && GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
		if (left && !leftWasDown) LEFT.addLast(now);
		if (right && !rightWasDown) RIGHT.addLast(now);
		leftWasDown = left;
		rightWasDown = right;
		trim(LEFT, now);
		trim(RIGHT, now);
	}

	private static void trim(Deque<Long> clicks, long now) {
		while (!clicks.isEmpty() && now - clicks.peekFirst() > 1000) {
			clicks.pollFirst();
		}
	}

	public static int left() {
		return LEFT.size();
	}

	public static int right() {
		return RIGHT.size();
	}
}
