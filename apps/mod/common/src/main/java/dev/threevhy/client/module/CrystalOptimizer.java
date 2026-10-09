package dev.threevhy.client.module;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Removes an end crystal on your screen the moment you hit it, instead of waiting for the
 * server to confirm. The attack still goes to the server as normal.
 */
public final class CrystalOptimizer {
	private CrystalOptimizer() {
	}

	public static InteractionResult onAttack(Player player, Level level, InteractionHand hand, Entity entity,
			EntityHitResult hit) {
		if (!Modules.CRYSTAL_OPTIMIZER.enabled() || !level.isClientSide()) {
			return InteractionResult.PASS;
		}
		if (!(entity instanceof EndCrystal) || player != Minecraft.getInstance().player) {
			return InteractionResult.PASS;
		}
		// With Weakness and no Strength the hit may not break the crystal, so leave it alone
		if (player.hasEffect(MobEffects.WEAKNESS) && !player.hasEffect(MobEffects.STRENGTH)) {
			return InteractionResult.PASS;
		}
		entity.discard();
		return InteractionResult.PASS;
	}
}
