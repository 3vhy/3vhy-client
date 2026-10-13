package dev.threevhy.client.gui;

import dev.threevhy.client.ThreevhyClient;
import dev.threevhy.client.module.Modules;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

/**
 * The clean sans-serif (Titillium Web, SIL Open Font License) the Panels menu and the Music HUD use,
 * like the reference client's, in bold and semibold. It's in the mod's resources as TrueType fonts
 * (assets/threevhy/font), with Minecraft's own font behind it for any character it lacks. Theme's
 * Menu font setting can switch back to Minecraft's pixel font.
 */
public final class UiFont {
	private static final FontDescription BOLD = new FontDescription.Resource(
			Identifier.fromNamespaceAndPath(ThreevhyClient.MOD_ID, "ui_bold"));
	private static final FontDescription SEMIBOLD = new FontDescription.Resource(
			Identifier.fromNamespaceAndPath(ThreevhyClient.MOD_ID, "ui_semibold"));

	private UiFont() {
	}

	/** Whether the clean font is picked (Theme > Menu font). */
	public static boolean clean() {
		return !Modules.THEME_FONT.is("Minecraft");
	}

	/** Bold text in the clean font. */
	public static Component bold(String text) {
		return Component.literal(text).withStyle(Style.EMPTY.withFont(BOLD));
	}

	/** Semibold text in the clean font, for labels and smaller text. */
	public static Component semibold(String text) {
		return Component.literal(text).withStyle(Style.EMPTY.withFont(SEMIBOLD));
	}

	public static int width(Component text) {
		return Minecraft.getInstance().font.width(text);
	}
}
