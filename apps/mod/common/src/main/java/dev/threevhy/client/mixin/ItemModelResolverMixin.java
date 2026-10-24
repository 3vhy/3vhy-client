package dev.threevhy.client.mixin;

import dev.threevhy.client.module.Blades;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Blades: a sword's model swapped for a katana's or a scythe's as it's looked up. */
@Mixin(ItemModelResolver.class)
public abstract class ItemModelResolverMixin {
	@ModifyVariable(method = "appendItemLayers", at = @At("STORE"), ordinal = 0)
	private Identifier threevhy$blade(Identifier model, ItemStackRenderState state, ItemStack stack, ItemDisplayContext context, Level level,
			ItemOwner owner, int seed) {
		return Blades.model(model, stack, owner);
	}
}
