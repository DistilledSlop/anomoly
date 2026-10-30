package dev.anomoly.mixin;

import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.ServerClock;
import dev.anomoly.modules.TotemPopCounter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerEventsMixin {
    @Inject(method = "handleSetTime", at = @At("HEAD"))
    private void anomoly$timeUpdate(ClientboundSetTimePacket packet, CallbackInfo ci) {
        ServerClock.timeUpdate();
    }

    @Inject(method = "handleEntityEvent", at = @At("HEAD"))
    private void anomoly$entityEvent(ClientboundEntityEventPacket packet, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (packet.getEventId() != 35 || client.level == null || client.player == null) return;
        Entity entity = packet.getEntity(client.level);
        if (entity instanceof Player player) AnomolyClient.MODULES.get(TotemPopCounter.class).popped(client, player);
    }
}
