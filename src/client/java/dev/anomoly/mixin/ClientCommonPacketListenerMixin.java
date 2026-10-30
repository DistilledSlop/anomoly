package dev.anomoly.mixin;

import dev.anomoly.AnomolyClient;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class ClientCommonPacketListenerMixin {
    @Inject(method = "handleCustomPayload", at = @At("HEAD"))
    private void anomoly$recordChannel(ClientboundCustomPayloadPacket packet, CallbackInfo ci) {
        String channel = packet.payload().type().id().toString();
        Minecraft.getInstance().execute(() -> AnomolyClient.PROFILE.channel(channel));
    }

    @Inject(method = "onDisconnect", at = @At("HEAD"))
    private void anomoly$recordDisconnect(DisconnectionDetails details, CallbackInfo ci) {
        AnomolyClient.PROFILE.leave(details.reason().getString());
    }
}
