package dev.anomoly.modules;

import dev.anomoly.Module;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;

public final class AbortBreaking extends Module implements PacketInterceptor {
    public AbortBreaking() {
        super("abort_breaking", "AbortBreaking", "Do not tell the server when you stop mining a block, so it may keep your progress.", Category.EXPLOIT);
    }

    @Override public boolean intercept(Packet<?> packet) {
        return packet instanceof ServerboundPlayerActionPacket action
            && action.getAction() == ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK;
    }
}
