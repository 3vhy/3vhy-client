package dev.threevhy.client.mixin;

import dev.threevhy.client.module.PlayerList;
import dev.threevhy.client.module.PopTracker;
import dev.threevhy.client.module.TpsTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Totem pops and deaths arrive as entity events; the world time feeds the TPS estimate; command
 * suggestions answer the Player List's online checks.
 */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
	@Inject(method = "handleEntityEvent", at = @At("RETURN"))
	private void threevhy$entityEvent(ClientboundEntityEventPacket packet, CallbackInfo ci) {
		dev.threevhy.client.Hooks.entityEventHookRan = true;
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return;
		Entity entity = packet.getEntity(mc.level);
		if (entity != null) {
			PopTracker.onEntityEvent(entity, packet.getEventId());
		}
	}

	@Inject(method = "handleSetTime", at = @At("TAIL"))
	private void threevhy$time(ClientboundSetTimePacket packet, CallbackInfo ci) {
		dev.threevhy.client.Hooks.timePacketRan = true;
		TpsTracker.onTime(packet.gameTime());
	}

	/** Answers to the Player List's "/msg name" questions are taken here, before the chat box sees them. */
	@Inject(method = "handleCommandSuggestions", at = @At("HEAD"), cancellable = true)
	private void threevhy$suggestions(ClientboundCommandSuggestionsPacket packet, CallbackInfo ci) {
		java.util.List<String> texts = new java.util.ArrayList<>();
		for (var suggestion : packet.toSuggestions().getList()) texts.add(suggestion.getText());
		if (PlayerList.onSuggestions(packet.id(), texts)) ci.cancel();
	}

	@Inject(method = "handleLogin", at = @At("TAIL"))
	private void threevhy$login(CallbackInfo ci) {
		TpsTracker.reset();
	}
}
