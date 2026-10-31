package dev.eventide.client.sky;

import dev.eventide.client.module.Modules;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

/**
 * Light weather to go with the Rain, Snow and Fire skies: a few drops, flakes or embers a tick around you,
 * only where the sky is open above, and only on your screen. Kept to a handful so it costs next to nothing.
 */
public final class SkyWeather {
	private static final int RADIUS = 14;
	private static final BlockPos.MutableBlockPos POS = new BlockPos.MutableBlockPos();

	private SkyWeather() {
	}

	public static void tick(Minecraft mc) {
		ClientLevel level = mc.level;
		LocalPlayer player = mc.player;
		if (level == null || player == null || mc.isPaused() || !Modules.SKIES_PARTICLES.get() || !LivingSky.active()
				|| dev.eventide.client.module.Performance.fewerParticles()) return;
		if (level.dimensionType().hasCeiling() || level.dimensionType().skybox() == net.minecraft.world.level.dimension.DimensionType.Skybox.END) return;
		SkyType sky = LivingSky.selected();
		int count;
		ParticleOptions particle;
		switch (sky) {
			case RAIN -> {
				count = 6;
				particle = ParticleTypes.FALLING_WATER;
			}
			case SNOW -> {
				count = 4;
				particle = ParticleTypes.SNOWFLAKE;
			}
			case FIRE -> {
				count = 3;
				particle = ParticleTypes.SMALL_FLAME;
			}
			default -> {
				return;
			}
		}
		ThreadLocalRandom random = ThreadLocalRandom.current();
		for (int i = 0; i < count; i++) {
			double x = player.getX() + random.nextDouble(-RADIUS, RADIUS);
			double z = player.getZ() + random.nextDouble(-RADIUS, RADIUS);
			double y = player.getY() + random.nextDouble(2, 10);
			POS.set(x, y, z);
			if (!level.canSeeSky(POS)) continue;
			switch (sky) {
				case SNOW -> level.addParticle(particle, x, y, z, random.nextDouble(-0.02, 0.02), -0.04, random.nextDouble(-0.02, 0.02));
				case FIRE -> {
					level.addParticle(particle, x, player.getY() + random.nextDouble(0, 4), z, 0, 0.03, 0);
					if (random.nextInt(3) == 0) level.addParticle(ParticleTypes.ASH, x, y, z, 0, 0, 0);
				}
				default -> level.addParticle(particle, x, y, z, 0, 0, 0);
			}
		}
	}
}
