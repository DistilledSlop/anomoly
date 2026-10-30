package dev.anomoly.modules;

import dev.anomoly.AnomolyClient;
import dev.anomoly.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;

/** Implemented by modules that may hold back or drop packets the client is about to send. */
public interface PacketInterceptor {
    /** Returns true if the module took ownership of the packet and it must not be sent now. */
    boolean intercept(Packet<?> packet);

    /** Set while held packets are being replayed so interceptors do not capture them again. */
    ThreadLocal<Boolean> REPLAYING = ThreadLocal.withInitial(() -> false);

    static boolean anyIntercepts(Packet<?> packet) {
        if (REPLAYING.get() || Minecraft.getInstance().player == null) return false;
        for (Module module : AnomolyClient.MODULES.all()) {
            if (module.enabled() && module instanceof PacketInterceptor interceptor && interceptor.intercept(packet)) return true;
        }
        return false;
    }

    static void replay(Minecraft client, Packet<?> packet) {
        if (client.getConnection() == null) return;
        REPLAYING.set(true);
        try {
            client.getConnection().send(packet);
        } finally {
            REPLAYING.set(false);
        }
    }
}
