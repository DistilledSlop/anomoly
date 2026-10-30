package dev.anomoly.mixin;

import dev.anomoly.modules.PacketInterceptor;
import dev.anomoly.modules.PacketRewriter;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public abstract class ConnectionInterceptMixin {
    @ModifyVariable(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V", at = @At("HEAD"), argsOnly = true)
    private Packet<?> anomoly$rewrite(Packet<?> packet) {
        return PacketRewriter.rewriteAll(packet);
    }

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V", at = @At("HEAD"), cancellable = true)
    private void anomoly$intercept(Packet<?> packet, ChannelFutureListener listener, boolean flush, CallbackInfo ci) {
        if (PacketInterceptor.anyIntercepts(packet)) ci.cancel();
    }
}
