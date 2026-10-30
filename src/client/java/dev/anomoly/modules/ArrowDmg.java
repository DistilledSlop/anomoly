package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.item.Items;

public final class ArrowDmg extends Module implements PacketInterceptor {
    private final IntSetting packets = add(new IntSetting("packets", "Extra movement packets per shot", 20, 2, 100));

    public ArrowDmg() {
        super("arrow_dmg", "ArrowDmg", "Send a burst of tiny movement packets as a bow is released so the server fires a faster arrow; costs hunger (also covers BowPop).", Category.COMBAT);
    }

    @Override public boolean intercept(Packet<?> packet) {
        Minecraft client = Minecraft.getInstance();
        if (!(packet instanceof ServerboundPlayerActionPacket action) || action.getAction() != ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM
            || client.player == null || !client.player.getUseItem().is(Items.BOW)) return false;
        var player = client.player;
        PacketInterceptor.replay(client, new ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));
        for (int i = 0; i < packets.get() / 2; i++) {
            PacketInterceptor.replay(client, new ServerboundMovePlayerPacket.Pos(player.position().add(0, 1.0E-10, 0), true, false));
            PacketInterceptor.replay(client, new ServerboundMovePlayerPacket.Pos(player.position(), false, false));
        }
        return false;
    }
}
