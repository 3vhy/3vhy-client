package dev.threevhy.client.mixin;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Lets the hub draw the scoreboard itself, sharp above the blurred game. */
@Mixin(Gui.class)
public interface GuiAccessor {
	@Invoker("displayScoreboardSidebar")
	void threevhy$displayScoreboardSidebar(GuiGraphics graphics, Objective objective);
}
