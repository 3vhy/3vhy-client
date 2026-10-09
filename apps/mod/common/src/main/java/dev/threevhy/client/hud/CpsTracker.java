package dev.threevhy.client.hud;

import dev.threevhy.client.Hooks;
import dev.threevhy.client.ThreevhyClient;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallbackI;

/**
 * Counts mouse clicks in the last second. Listens to every press GLFW reports (so jitter and
 * butterfly clicks that are shorter than a frame still count) and hands each one on to
 * Minecraft unchanged. Falls back to checking the buttons once per frame if that fails.
 */
public final class CpsTracker {
	private static final Deque<Long> LEFT = new ArrayDeque<>();
	private static final Deque<Long> RIGHT = new ArrayDeque<>();
	private static boolean leftWasDown = false;
	private static boolean rightWasDown = false;
	private static boolean listening = false;
	/** Kept so the garbage collector never frees the native callback. */
	private static GLFWMouseButtonCallback callback;

	private CpsTracker() {
	}

	/** Called once the game window exists. */
	public static void install() {
		long window = Hooks.windowHandle();
		if (window == 0 || listening) return;
		try {
			GLFWMouseButtonCallbackI[] previous = new GLFWMouseButtonCallbackI[1];
			callback = GLFWMouseButtonCallback.create((handle, button, action, mods) -> {
				if (action == GLFW.GLFW_PRESS && Minecraft.getInstance().screen == null) {
					record(button);
				}
				if (previous[0] != null) previous[0].invoke(handle, button, action, mods);
			});
			previous[0] = GLFW.glfwSetMouseButtonCallback(window, callback);
			listening = true;
		} catch (Throwable t) {
			ThreevhyClient.LOG.warn("Couldn't listen for mouse clicks, counting once per frame instead", t);
		}
	}

	private static synchronized void record(int button) {
		long now = System.currentTimeMillis();
		if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) LEFT.addLast(now);
		else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) RIGHT.addLast(now);
	}

	/** Called every frame from the HUD. */
	public static synchronized void poll(Minecraft mc) {
		long now = System.currentTimeMillis();
		if (!listening) {
			long window = Hooks.windowHandle();
			boolean inGame = mc.screen == null && window != 0;
			boolean left = inGame && GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
			boolean right = inGame && GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
			if (left && !leftWasDown) LEFT.addLast(now);
			if (right && !rightWasDown) RIGHT.addLast(now);
			leftWasDown = left;
			rightWasDown = right;
		}
		trim(LEFT, now);
		trim(RIGHT, now);
	}

	private static void trim(Deque<Long> clicks, long now) {
		while (!clicks.isEmpty() && now - clicks.peekFirst() > 1000) {
			clicks.pollFirst();
		}
	}

	public static synchronized int left() {
		return LEFT.size();
	}

	public static synchronized int right() {
		return RIGHT.size();
	}
}
