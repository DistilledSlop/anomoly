package dev.anomoly.modules;

import dev.anomoly.Module;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;

public final class XCarry extends Module implements PacketInterceptor {
    public XCarry() {
        super("xcarry", "XCarry", "Keep items in the 2x2 crafting grid after closing the inventory by not telling the server it closed.", Category.PLAYER);
    }

    @Override public boolean intercept(Packet<?> packet) {
        return packet instanceof ServerboundContainerClosePacket close && close.getContainerId() == 0;
    }
}
