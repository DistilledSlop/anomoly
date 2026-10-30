package dev.anomoly.modules;

import dev.anomoly.Module;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;

public final class AntiHunger extends Module implements PacketInterceptor {
    public AntiHunger() {
        super("anti_hunger", "AntiHunger", "Never tell the server you started sprinting, so sprinting and sprint-jumping cost no extra hunger.", Category.EXPLOIT);
    }

    @Override public boolean intercept(Packet<?> packet) {
        return packet instanceof ServerboundPlayerCommandPacket command && command.getAction() == ServerboundPlayerCommandPacket.Action.START_SPRINTING;
    }
}
