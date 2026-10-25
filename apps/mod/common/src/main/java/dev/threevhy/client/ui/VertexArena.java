package dev.threevhy.client.ui;

/**
 * Where 3vhy's shapes keep their vertices until Minecraft's GUI renderer draws them, later in the frame.
 * Instead of every shape copying its vertices into new arrays (which made most of the garbage the HUD
 * left each frame), they're appended to one big block that's reused from the start once the frame's GUI
 * has been drawn. A shape that doesn't fit starts a bigger block; ones already handed out keep theirs.
 * Render thread only.
 */
public final class VertexArena {
	private static final int START = 1 << 15;
	private static final int MAX = 1 << 21;

	/** One block of vertices: positions (x, y), texture coordinates (u, v) and colours. */
	public static final class Block {
		public final float[] xy;
		public final float[] uv;
		public final int[] colors;
		private int used;

		Block(int vertices) {
			xy = new float[vertices * 2];
			uv = new float[vertices * 2];
			colors = new int[vertices];
		}

		int capacity() {
			return colors.length;
		}
	}

	private static Block current = new Block(START);
	private static Block last;
	private static int lastOffset;

	private VertexArena() {
	}

	/**
	 * Copies {@code vertices} vertices in; {@link #block()} and {@link #offset()} then say where they went.
	 * {@code uv} may be null for shapes without a picture.
	 */
	public static void put(float[] xy, float[] uv, int[] colors, int vertices) {
		Block block = current;
		if (block.used + vertices > block.capacity()) {
			int size = Math.min(MAX, Math.max(block.capacity() * 2, vertices));
			if (size < vertices) size = vertices;
			block = new Block(size);
			current = block;
		}
		int offset = block.used;
		System.arraycopy(xy, 0, block.xy, offset * 2, vertices * 2);
		if (uv != null) System.arraycopy(uv, 0, block.uv, offset * 2, vertices * 2);
		System.arraycopy(colors, 0, block.colors, offset, vertices);
		block.used = offset + vertices;
		last = block;
		lastOffset = offset;
	}

	/** The block the last {@link #put} went into. */
	public static Block block() {
		return last;
	}

	/** Where in its block the last {@link #put} starts, in vertices. */
	public static int offset() {
		return lastOffset;
	}

	/** Called once the frame's GUI has been drawn: everything handed out is done with. */
	public static void reset() {
		current.used = 0;
	}
}
