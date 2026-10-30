package dev.anomoly.mixin;

import dev.anomoly.AnomolyClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Inject(method = "handleMovePlayer", at = @At("TAIL"))
    private void anomoly$recordCorrection(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
        AnomolyClient.PROFILE.correction(packet.id(), Minecraft.getInstance().player);
    }
}
