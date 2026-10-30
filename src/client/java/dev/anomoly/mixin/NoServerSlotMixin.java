package dev.anomoly.mixin;

import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.NoServerSlot;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class NoServerSlotMixin {
    @Inject(method = "handleSetHeldSlot", at = @At("HEAD"), cancellable = true)
    private void anomoly$ignoreServerSlot(ClientboundSetHeldSlotPacket packet, CallbackInfo ci) {
        if (AnomolyClient.MODULES.get(NoServerSlot.class).enabled()) ci.cancel();
    }
}
