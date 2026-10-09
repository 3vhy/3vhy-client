package dev.threevhy.client;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

/** The game window says "3vhy Client" and shows the 3vhy icon instead of the grass block. */
public final class WindowBranding {
	private static final int[] ICON_SIZES = {16, 32, 48, 64, 128, 256};
	private static boolean iconSet = false;

	private WindowBranding() {
	}

	public static String title() {
		return "3vhy Client " + ThreevhyClient.minecraftVersion();
	}

	/** Runs on the render thread. */
	public static void apply() {
		long window = Hooks.windowHandle();
		if (window == 0) return;
		GLFW.glfwSetWindowTitle(window, title());
		if (!iconSet) {
			iconSet = setIcon(window);
		}
	}

	private static boolean setIcon(long window) {
		// macOS windows have no icon of their own (the Dock shows the app's)
		if (System.getProperty("os.name", "").toLowerCase().contains("mac")) return true;
		List<ByteBuffer> files = new ArrayList<>();
		List<ByteBuffer> pixels = new ArrayList<>();
		try (MemoryStack stack = MemoryStack.stackPush()) {
			GLFWImage.Buffer images = GLFWImage.malloc(ICON_SIZES.length, stack);
			int count = 0;
			for (int size : ICON_SIZES) {
				ByteBuffer file = read("/assets/threevhy/icons/icon_" + size + ".png");
				if (file == null) continue;
				files.add(file);
				var w = stack.mallocInt(1);
				var h = stack.mallocInt(1);
				var channels = stack.mallocInt(1);
				ByteBuffer image = STBImage.stbi_load_from_memory(file, w, h, channels, 4);
				if (image == null) continue;
				pixels.add(image);
				images.position(count);
				images.width(w.get(0));
				images.height(h.get(0));
				images.pixels(image);
				count++;
			}
			if (count == 0) return false;
			images.position(0);
			images.limit(count);
			GLFW.glfwSetWindowIcon(window, images);
			return true;
		} catch (Throwable t) {
			ThreevhyClient.LOG.warn("Couldn't set the 3vhy window icon", t);
			return false;
		} finally {
			for (ByteBuffer image : pixels) STBImage.stbi_image_free(image);
			for (ByteBuffer file : files) MemoryUtil.memFree(file);
		}
	}

	private static ByteBuffer read(String path) {
		try (InputStream in = WindowBranding.class.getResourceAsStream(path)) {
			if (in == null) return null;
			byte[] bytes = in.readAllBytes();
			ByteBuffer buffer = MemoryUtil.memAlloc(bytes.length);
			buffer.put(bytes).flip();
			return buffer;
		} catch (IOException e) {
			return null;
		}
	}
}
