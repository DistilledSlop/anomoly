package dev.anomoly.mixin;

import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.NoServerPack;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class ResourcePackNoServerPackMixin {
    @Inject(method = "handleResourcePackPush", at = @At("HEAD"), cancellable = true)
    private void anomoly$skipServerPack(ClientboundResourcePackPushPacket packet, CallbackInfo ci) {
        NoServerPack module = AnomolyClient.MODULES.get(NoServerPack.class);
        if (!module.enabled()) return;
        module.answer((ClientCommonPacketListenerImpl)(Object)this, packet);
        ci.cancel();
    }
}
