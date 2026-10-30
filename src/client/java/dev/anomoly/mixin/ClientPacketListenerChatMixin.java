package dev.anomoly.mixin;

import dev.anomoly.modules.ChatFilter;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerChatMixin {
    @ModifyVariable(method = "sendChat", at = @At("HEAD"), argsOnly = true)
    private String anomoly$rewriteOutgoing(String message) {
        return ChatFilter.outgoing(message);
    }
}
