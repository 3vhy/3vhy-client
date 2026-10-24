package dev.threevhy.client.gui.menu;

import dev.threevhy.client.Config;
import dev.threevhy.client.Draw;
import dev.threevhy.client.ThreevhyClient;
import dev.threevhy.client.compat.CompatScreen;
import dev.threevhy.client.module.Module.Category;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/** The Orbit over the blurred game: hosts {@link OrbitView} and opens the menu at the tab you pick. */
public final class OrbitScreen extends CompatScreen implements OrbitView.Host {
	private final Screen parent;
	private final OrbitView view;

	public OrbitScreen(Screen parent) {
		this(parent, MenuView.indexOf(Category.RENDER));
	}

	public OrbitScreen(Screen parent, int tab) {
		super(Component.literal("3vhy Orbit"));
		this.parent = parent;
		this.view = new OrbitView(this, Math.max(0, tab));
	}

	public OrbitView view() {
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
	public void open(int tab) {
		MenuView.Tab picked = MenuView.TABS.get(tab);
		MenuScreen menu = new MenuScreen(parent, picked.category(), true);
		if (picked.category() == null) menu.view().openProfiles();
		Minecraft.getInstance().setScreen(menu);
	}

	@Override
	public void close() {
		onClose();
	}

	@Override
	public void clickSound() {
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
	}

	@Override
	public boolean inWorld() {
		return Minecraft.getInstance().level != null;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		return view.mouseClicked(event.x(), event.y(), event.button());
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		return view.mouseDragged(event.x(), event.y());
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		return view.mouseReleased(event.x(), event.y());
	}

	@Override
	public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
		return view.mouseScrolled(vertical);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (view.keyPressed(event.key())) return true;
		if (ThreevhyClient.openSettingsKey != null && ThreevhyClient.openSettingsKey.matches(event)) {
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
