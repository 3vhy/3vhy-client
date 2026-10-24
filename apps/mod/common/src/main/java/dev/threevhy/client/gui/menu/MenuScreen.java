package dev.threevhy.client.gui.menu;

import dev.threevhy.client.Config;
import dev.threevhy.client.Draw;
import dev.threevhy.client.ThreevhyClient;
import dev.threevhy.client.compat.CompatScreen;
import dev.threevhy.client.gui.HudEditorScreen;
import dev.threevhy.client.gui.MenuNameTag;
import dev.threevhy.client.module.Module;
import dev.threevhy.client.module.Module.Category;
import dev.threevhy.client.module.Modules;
import dev.threevhy.client.ui.Paint;
import java.util.Locale;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.PlayerSkin;

/**
 * The 3vhy mod menu in game and on the title screen: hosts {@link MenuView} over the blurred game, and
 * sends Esc back to the Orbit it came from (or out of the menu).
 */
public final class MenuScreen extends CompatScreen implements MenuView.Host {
	private final Screen parent;
	private final boolean fromOrbit;
	private final SheetExtras extras;
	private final MenuView view;

	public MenuScreen(Screen parent, Category start) {
		this(parent, start, false);
	}

	MenuScreen(Screen parent, Category start, boolean fromOrbit) {
		super(Component.literal("3vhy Client"));
		this.parent = parent;
		this.fromOrbit = fromOrbit;
		this.extras = new SheetExtras(this);
		this.view = new MenuView(this, start == null ? Category.RENDER : start);
	}

	/** The menu at the Profile page. */
	public static MenuScreen profiles(Screen parent) {
		MenuScreen screen = new MenuScreen(parent, null);
		screen.view.openProfiles();
		return screen;
	}

	/** The menu with one module's settings open. */
	public static MenuScreen settings(Screen parent, Module module) {
		MenuScreen screen = new MenuScreen(parent, module.category);
		screen.view.openSettings(module);
		return screen;
	}

	/** What Right Shift opens: the Orbit, or the menu straight away (Theme > Right Shift opens). */
	public static Screen fromKey(Screen parent) {
		return Modules.THEME_RIGHT_SHIFT.is("Menu") ? new MenuScreen(parent, Category.RENDER) : new OrbitScreen(parent);
	}

	public MenuView view() {
		return view;
	}

	@Override
	protected boolean menuTexture() {
		return false;
	}

	@Override
	protected void drawBackdrop(Draw d) {
	}

	@Override
	protected void draw(Draw d, int mouseX, int mouseY, float delta) {
		view.draw(d, this.width, this.height, mouseX, mouseY);
	}

	@Override
	public String keyFor(Module module) {
		KeyMapping key = module == Modules.ZOOM ? ThreevhyClient.zoomKey : module == Modules.FREELOOK ? ThreevhyClient.freelookKey
				: module == Modules.BLADES ? ThreevhyClient.inspectKey : null;
		return key == null ? null : shortKey(key.getTranslatedKeyMessage().getString());
	}

	/** A key's name made short enough for a card's key badge: Left Alt is L-ALT. */
	static String shortKey(String name) {
		String upper = name.toUpperCase(Locale.ROOT);
		if (upper.startsWith("LEFT ")) return "L-" + upper.substring(5);
		if (upper.startsWith("RIGHT ")) return "R-" + upper.substring(6);
		return upper.length() > 6 ? upper.substring(0, 6) : upper;
	}

	@Override
	public String playerName() {
		return Minecraft.getInstance().getUser().getName();
	}

	@Override
	public void drawFace(Draw d, float x, float y, float size, float radius) {
		face(d, MenuNameTag.ownSkin(Minecraft.getInstance()), x, y, size, radius);
	}

	/** A player's head (with its hat layer) as a sharp, rounded picture. */
	public static void face(Draw d, PlayerSkin skin, float x, float y, float size, float radius) {
		Identifier texture = skin.body().texturePath();
		Paint.image(d, texture, x, y, x + size, y + size, radius, 8 / 64f, 8 / 64f, 16 / 64f, 16 / 64f, 0xFFFFFFFF, false);
		Paint.image(d, texture, x, y, x + size, y + size, radius, 40 / 64f, 8 / 64f, 48 / 64f, 16 / 64f, 0xFFFFFFFF, false);
	}

	@Override
	public boolean inWorld() {
		return Minecraft.getInstance().level != null;
	}

	@Override
	public void openHudEditor() {
		Minecraft.getInstance().setScreen(new HudEditorScreen(this));
	}

	@Override
	public void back() {
		if (fromOrbit) {
			Minecraft.getInstance().setScreen(new OrbitScreen(parent, MenuView.TABS.indexOf(currentTab())));
		} else {
			onClose();
		}
	}

	private MenuView.Tab currentTab() {
		Category category = view.category();
		for (MenuView.Tab tab : MenuView.TABS) {
			if (tab.category() == category) return tab;
		}
		return MenuView.TABS.get(0);
	}

	@Override
	public void clickSound() {
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
	}

	@Override
	public SettingsSheet.Extras extras() {
		return extras;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (extras.pickingKey()) {
			extras.pickMouse(event.button());
			return true;
		}
		if (view.mouseClicked(event.x(), event.y(), event.button())) return true;
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (view.mouseDragged(event.x(), event.y())) return true;
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (view.mouseReleased()) return true;
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
		return view.mouseScrolled(x, y, vertical);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		return view.charTyped(event.codepointAsString());
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		boolean ctrl = (event.modifiers() & 2) != 0;
		if (view.keyPressed(event.key(), ctrl)) return true;
		if (!view.typing() && ThreevhyClient.openSettingsKey != null && ThreevhyClient.openSettingsKey.matches(event)) {
			onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public void onClose() {
		Config.save();
		Minecraft.getInstance().setScreen(parent);
	}
}
