package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;

public final class PingSpoof extends Module implements PacketInterceptor {
    private final FloatSetting extra = add(new FloatSetting("extra_ms", "Added latency (ms)", 300, 20, 2500));
    private final PacketDelay held = new PacketDelay();

    public PingSpoof() {
        super("ping_spoof", "PingSpoof", "Delay keep-alive and pong replies so the server measures a higher ping.", Category.EXPLOIT);
    }

    @Override public boolean intercept(Packet<?> packet) {
        if (!(packet instanceof ServerboundKeepAlivePacket) && !(packet instanceof ServerboundPongPacket)) return false;
        held.hold(packet, (long)extra.get());
        return true;
    }

    @Override public void tick(Minecraft client) { held.releaseDue(client); }

    @Override protected void onDisable(Minecraft client) { held.releaseAll(client); }
}
