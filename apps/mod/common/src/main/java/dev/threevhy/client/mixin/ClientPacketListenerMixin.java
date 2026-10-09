package dev.threevhy.client.mixin;

import dev.threevhy.client.module.PopTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Totem pops and deaths arrive as entity events; hand them to the pop tracker. */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
	@Inject(method = "handleEntityEvent", at = @At("RETURN"))
	private void threevhy$entityEvent(ClientboundEntityEventPacket packet, CallbackInfo ci) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return;
		Entity entity = packet.getEntity(mc.level);
		if (entity != null) {
			PopTracker.onEntityEvent(entity, packet.getEventId());
		}
	}
}
