package dev.anomoly.modules;

import dev.anomoly.AnomolyClient;
import dev.anomoly.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;

/** Implemented by modules that substitute a modified copy of an outgoing packet. */
public interface PacketRewriter {
    Packet<?> rewrite(Packet<?> packet);

    static Packet<?> rewriteAll(Packet<?> packet) {
        if (PacketInterceptor.REPLAYING.get() || Minecraft.getInstance().player == null) return packet;
        Packet<?> current = packet;
        for (Module module : AnomolyClient.MODULES.all()) {
            if (module.enabled() && module instanceof PacketRewriter rewriter) current = rewriter.rewrite(current);
        }
        return current;
    }
}
