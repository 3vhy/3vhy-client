package dev.threevhy.client.gui;

import dev.threevhy.client.Draw;
import dev.threevhy.client.Hooks;
import dev.threevhy.client.ThreevhyClient;
import dev.threevhy.client.compat.Compat;
import dev.threevhy.client.hud.Theme;
import dev.threevhy.client.mixin.TitleScreenAccessor;
import dev.threevhy.client.module.Modules;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

/**
 * The 3vhy main menu, laid over Minecraft's own title screen in Lunar's style: the 3vhy sky with
 * snow, the 3vhy emblem and name where Minecraft's logo was, and the menu buttons as slim glass.
 * The title screen itself stays, so buttons and panels other mods add to it (Mod Menu, Essential
 * and so on) still show up. Only Minecraft's and Mod Menu's own buttons are restyled; anything
 * else keeps its own look. A "3vhy Mods" button in the top left corner (or Right Shift) opens the
 * 3vhy mod menu.
 */
public final class TitleScreenStyle {
	public static final String MODS_LABEL = "3vhy Mods";
	private static final int MARGIN = 6;
	private static final Set<AbstractWidget> STYLED = Collections.newSetFromMap(new WeakHashMap<>());
	private static final Snow SNOW = new Snow();
	private static long openedAt = 0;

	private TitleScreenStyle() {
	}

	public static boolean enabled() {
		return Modules.MAIN_MENU.enabled();
	}

	private static boolean onTitleScreen() {
		return enabled() && Minecraft.getInstance().screen instanceof TitleScreen;
	}

	/** Whether Minecraft's logo (and its splash text) should be left out right now. */
	public static boolean hidesMinecraftLogo() {
		return onTitleScreen();
	}

	/** Called once the title screen has made its buttons (and other mods have added theirs). */
	public static void prepare(Screen screen) {
		if (!enabled() || !(screen instanceof TitleScreen)) return;
		((TitleScreenAccessor) screen).threevhy$setSplash(null);
		List<AbstractWidget> widgets = Compat.widgets(screen);
		for (AbstractWidget widget : widgets) {
			if (restyles(widget)) STYLED.add(widget);
		}
		Minecraft mc = Minecraft.getInstance();
		Button mods = Button.builder(Component.literal(MODS_LABEL), button -> mc.setScreen(ModMenuScreen.create(screen)))
				.pos(MARGIN, freeSpot(widgets, MARGIN, 80, 20)).size(80, 20).build();
		Compat.addWidget(screen, mods);
		STYLED.add(mods);
		ScreenKeyboardEvents.afterKeyPress(screen).register((shown, event) -> {
			if (ThreevhyClient.openSettingsKey != null && ThreevhyClient.openSettingsKey.matches(event) && mc.screen == shown) {
				mc.setScreen(ModMenuScreen.create(shown));
			}
		});
		openedAt = System.currentTimeMillis();
		Hooks.mainMenuShown = true;
	}

	/** The first height down the left edge where a button of this size doesn't cover another mod's button. */
	private static int freeSpot(List<AbstractWidget> widgets, int x, int w, int h) {
		int y = MARGIN;
		boolean moved = true;
		while (moved && y < 200) {
			moved = false;
			for (AbstractWidget other : widgets) {
				if (!other.visible) continue;
				boolean overlaps = other.getX() < x + w && other.getX() + other.getWidth() > x && other.getY() < y + h
						&& other.getY() + other.getHeight() > y;
				if (overlaps) {
					y = other.getY() + other.getHeight() + 4;
					moved = true;
				}
			}
		}
		return y;
	}

	/** Plain text buttons from Minecraft and Mod Menu. Icon buttons, links and other mods' own widgets keep their look. */
	private static boolean restyles(AbstractWidget widget) {
		if (!(widget instanceof Button) || widget instanceof SpriteIconButton || widget instanceof PlainTextButton) return false;
		String type = widget.getClass().getName();
		return type.startsWith("net.minecraft.") || type.contains("modmenu");
	}

	public static boolean styled(AbstractWidget widget) {
		return widget.visible && STYLED.contains(widget) && onTitleScreen();
	}

	/** The background: the 3vhy sky and snow, a light dim like Lunar's blur, then the emblem and name. */
	public static void drawBackground(Draw d, int w, int h) {
		MainMenuScreen.drawBackdrop(d, w, h);
		SNOW.draw(d, w, h);
		d.fill(0, 0, w, h, 0x26000000);
		float intro = Math.min(1f, (System.currentTimeMillis() - openedAt) / 400f);
		int alpha = Math.round(255 * intro);
		int cell = h >= 300 ? 3 : 2;
		int size = 16 * cell;
		int firstButton = h / 4 + 48;
		int nameY = firstButton - 22;
		int top = Math.max(4, nameY - 8 - size) + Math.round((1 - intro) * -6);
		HubScreen.drawEmblem(d, (w - size) / 2, top, size, cell, alpha, 0xFFFFFFFF);
		Minecraft mc = Minecraft.getInstance();
		d.push();
		d.translate(w / 2f, nameY);
		d.scale(1.5f);
		String brand = "3VHY";
		String client = " CLIENT";
		int brandW = mc.font.width(Ui.bold(brand));
		int textW = brandW + mc.font.width(client);
		d.text(Ui.bold(brand), -textW / 2, 0, MenuTheme.alpha(0xFFFFFF, alpha), true);
		d.text(client, -textW / 2 + brandW, 0, MenuTheme.alpha(0xD8E3F3FC, alpha), true);
		d.pop();
	}

	/** Draws a restyled button in place of Minecraft's: slim smoked glass, an ice-blue edge on hover, uppercase text. */
	public static void drawButton(Draw d, AbstractWidget widget, int mouseX, int mouseY) {
		Hooks.titleButtonsStyled = true;
		int x = widget.getX();
		int y = widget.getY();
		int w = widget.getWidth();
		int h = widget.getHeight();
		boolean active = widget.active;
		boolean hover = active && (widget.isFocused() || mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h);
		d.roundRect(x, y, x + w, y + h, 4, hover ? 0xB0163056 : 0x8C080C18);
		d.roundOutline(x, y, x + w, y + h, 4, hover ? Theme.CYAN : 0x59FFFFFF);
		Minecraft mc = Minecraft.getInstance();
		String label = widget.getMessage().getString().toUpperCase(Locale.ROOT);
		int max = w - 8;
		if (mc.font.width(label) > max) {
			while (!label.isEmpty() && mc.font.width(label + "...") > max) label = label.substring(0, label.length() - 1);
			label = label + "...";
		}
		int color = !active ? 0x80FFFFFF : hover ? 0xFFFFFFFF : 0xFFE3F3FC;
		d.text(label, x + (w - mc.font.width(label)) / 2, y + (h - 8) / 2, color, false);
	}
}
