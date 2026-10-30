package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

public final class FakeLag extends Module implements PacketInterceptor {
    private final FloatSetting delay = add(new FloatSetting("delay_ms", "Delay (ms)", 150, 20, 1000));
    private final PacketDelay held = new PacketDelay();

    public FakeLag() {
        super("fake_lag", "FakeLag", "Delay your movement packets by a fixed time to simulate network lag.", Category.COMBAT);
    }

    @Override public boolean intercept(Packet<?> packet) {
        if (!(packet instanceof ServerboundMovePlayerPacket)) return false;
        held.hold(packet, (long)delay.get());
        return true;
    }

    @Override public void tick(Minecraft client) { held.releaseDue(client); }

    @Override protected void onDisable(Minecraft client) { held.releaseAll(client); }
}
