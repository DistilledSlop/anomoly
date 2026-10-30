package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.item.Items;

public final class MaceKill extends Module implements PacketInterceptor {
    private final FloatSetting height = add(new FloatSetting("height", "Fake fall height (blocks)", 8.0F, 1.0F, 9.0F));

    public MaceKill() {
        super("mace_kill", "MaceKill", "Before a mace attack, report a rise and drop of a few blocks so the server counts a larger fall (also covers MaceDmg).", Category.COMBAT);
    }

    @Override public boolean intercept(Packet<?> packet) {
        Minecraft client = Minecraft.getInstance();
        if (!(packet instanceof ServerboundAttackPacket) || client.player == null || !client.player.getMainHandItem().is(Items.MACE)) return false;
        var player = client.player;
        PacketInterceptor.replay(client, new ServerboundMovePlayerPacket.Pos(player.position().add(0, height.get(), 0), false, false));
        PacketInterceptor.replay(client, new ServerboundMovePlayerPacket.Pos(player.position(), false, false));
        return false;
    }
}
