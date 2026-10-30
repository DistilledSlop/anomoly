package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

public final class Blink extends Module implements PacketInterceptor {
    private final FloatSetting maxSeconds = add(new FloatSetting("max_seconds", "Auto release after seconds", 10, 1, 60));
    private final PacketDelay held = new PacketDelay();
    private int ticks;

    public Blink() {
        super("blink", "Blink", "Hold back movement packets so the server sees you stay put, then release the path when disabled.", Category.PLAYER);
    }

    @Override public boolean intercept(Packet<?> packet) {
        if (!(packet instanceof ServerboundMovePlayerPacket)) return false;
        held.hold(packet, 0);
        return true;
    }

    @Override public void tick(Minecraft client) {
        if (++ticks >= maxSeconds.get() * 20) {
            held.releaseAll(client);
            ticks = 0;
        }
    }

    @Override protected void onEnable(Minecraft client) { ticks = 0; held.clear(); }

    @Override protected void onDisable(Minecraft client) { held.releaseAll(client); }
}
