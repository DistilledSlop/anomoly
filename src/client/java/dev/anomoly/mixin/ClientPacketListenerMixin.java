package dev.anomoly.mixin;

import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.Velocity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Inject(method = "handleSetEntityMotion", at = @At("TAIL"))
    private void anomoly$scaleVelocity(ClientboundSetEntityMotionPacket packet, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || packet.id() != client.player.getId()) return;
        Velocity velocity = AnomolyClient.MODULES.get(Velocity.class);
        if (velocity.enabled()) client.player.lerpMotion(velocity.scale(packet.movement()));
    }

    @Inject(method = "handleMovePlayer", at = @At("TAIL"))
    private void anomoly$recordCorrection(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
        AnomolyClient.PROFILE.correction(packet.id(), Minecraft.getInstance().player);
    }
}
