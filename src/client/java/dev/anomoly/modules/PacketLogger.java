package dev.anomoly.modules;

import dev.anomoly.AnomolyClient;
import dev.anomoly.Module;
import net.minecraft.network.protocol.Packet;

public final class PacketLogger extends Module implements PacketInterceptor {
    public PacketLogger() {
        super("packet_logger", "PacketLogger", "Write the class and contents of every packet sent or received to the client log.", Category.MISC);
    }

    @Override public boolean intercept(Packet<?> packet) {
        AnomolyClient.LOGGER.info("[out] {} {}", packet.getClass().getSimpleName(), packet);
        return false;
    }

    public void received(Packet<?> packet) {
        if (enabled()) AnomolyClient.LOGGER.info("[in] {} {}", packet.getClass().getSimpleName(), packet);
    }
}
