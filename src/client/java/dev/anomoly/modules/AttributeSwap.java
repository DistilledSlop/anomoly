package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;

public final class AttributeSwap extends Module implements PacketInterceptor {
    private final IntSetting slot = add(new IntSetting("slot", "Hotbar slot to swap to (1-9)", 2, 1, 9));
    private int restoreTo = -1;

    public AttributeSwap() {
        super("attribute_swap", "Attribute Swap", "Tell the server you hold another hotbar slot while each attack lands, then switch back on the next tick.", Category.COMBAT);
    }

    @Override public boolean intercept(Packet<?> packet) {
        Minecraft client = Minecraft.getInstance();
        if (!(packet instanceof ServerboundAttackPacket) || client.player == null) return false;
        int current = client.player.getInventory().getSelectedSlot();
        if (current == slot.get() - 1) return false;
        restoreTo = current;
        PacketInterceptor.replay(client, new ServerboundSetCarriedItemPacket(slot.get() - 1));
        return false;
    }

    @Override public void tick(Minecraft client) {
        if (restoreTo < 0) return;
        PacketInterceptor.replay(client, new ServerboundSetCarriedItemPacket(restoreTo));
        restoreTo = -1;
    }
}
