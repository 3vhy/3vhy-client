package dev.threevhy.client.module;

import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;

/**
 * Borderless fullscreen: instead of taking over the monitor, fullscreen becomes a window without
 * a border that covers the monitor, so alt-tabbing is instant.
 */
public final class Borderless {
	private static boolean active = false;
	private static Boolean applied = null;

	private Borderless() {
	}

	/**
	 * Called when Minecraft switches between windowed and fullscreen. Returns true when it took
	 * care of the switch; saveWindowed is run first so the windowed size comes back afterwards.
	 */
	public static boolean apply(long handle, boolean fullscreen, Runnable saveWindowed) {
		applied = Modules.BORDERLESS.enabled();
		if (!fullscreen || !Modules.BORDERLESS.enabled()) {
			if (active) {
				GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_DECORATED, GLFW.GLFW_TRUE);
				active = false;
			}
			return false;
		}
		long monitor = monitorFor(handle);
		GLFWVidMode mode = monitor == 0 ? null : GLFW.glfwGetVideoMode(monitor);
		if (mode == null) return false;
		int[] mx = new int[1];
		int[] my = new int[1];
		GLFW.glfwGetMonitorPos(monitor, mx, my);
		if (!active && GLFW.glfwGetWindowMonitor(handle) == 0) saveWindowed.run();
		GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_DECORATED, GLFW.GLFW_FALSE);
		GLFW.glfwSetWindowMonitor(handle, 0L, mx[0], my[0], mode.width(), mode.height(), GLFW.GLFW_DONT_CARE);
		active = true;
		return true;
	}

	/** Whether the module changed since fullscreen was last set up. */
	public static boolean needsReapply() {
		return applied != null && applied != Modules.BORDERLESS.enabled();
	}

	/** The monitor the window is mostly on, or the primary one. */
	private static long monitorFor(long handle) {
		int[] wx = new int[1];
		int[] wy = new int[1];
		int[] ww = new int[1];
		int[] wh = new int[1];
		GLFW.glfwGetWindowPos(handle, wx, wy);
		GLFW.glfwGetWindowSize(handle, ww, wh);
		int centerX = wx[0] + ww[0] / 2;
		int centerY = wy[0] + wh[0] / 2;
		PointerBuffer monitors = GLFW.glfwGetMonitors();
		if (monitors != null) {
			for (int i = 0; i < monitors.limit(); i++) {
				long monitor = monitors.get(i);
				GLFWVidMode mode = GLFW.glfwGetVideoMode(monitor);
				if (mode == null) continue;
				int[] mx = new int[1];
				int[] my = new int[1];
				GLFW.glfwGetMonitorPos(monitor, mx, my);
				if (centerX >= mx[0] && centerX < mx[0] + mode.width() && centerY >= my[0] && centerY < my[0] + mode.height()) {
					return monitor;
				}
			}
		}
		return GLFW.glfwGetPrimaryMonitor();
	}
}
