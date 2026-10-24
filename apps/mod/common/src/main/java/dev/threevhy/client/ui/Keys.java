package dev.threevhy.client.ui;

/** The keyboard keys 3vhy's menus listen for, as GLFW key codes. */
public final class Keys {
	public static final int ESCAPE = 256;
	public static final int ENTER = 257;
	public static final int TAB = 258;
	public static final int BACKSPACE = 259;
	public static final int DELETE = 261;
	public static final int RIGHT = 262;
	public static final int LEFT = 263;
	public static final int DOWN = 264;
	public static final int UP = 265;
	public static final int PAGE_UP = 266;
	public static final int PAGE_DOWN = 267;
	public static final int HOME = 268;
	public static final int END = 269;
	public static final int SPACE = 32;
	public static final int SLASH = 47;
	public static final int KP_ENTER = 335;
	public static final int LEFT_CONTROL = 341;
	public static final int A = 65;
	public static final int V = 86;

	private Keys() {
	}

	public static boolean enter(int key) {
		return key == ENTER || key == KP_ENTER;
	}
}
