package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

public final class Damage extends Module {
    private final IntSetting falls = add(new IntSetting("falls", "Simulated falls", 1, 1, 10));

    public Damage() {
        super("damage", "Damage", "One-shot: report short falls to the server so it hurts you, then switch off. Do not use at low health.", Category.EXPLOIT);
    }

    @Override protected void onEnable(Minecraft client) {
        var player = client.player;
        if (player != null && player.connection != null) {
            for (int i = 0; i < falls.get(); i++) {
                PacketInterceptor.replay(client, new ServerboundMovePlayerPacket.Pos(player.position().add(0, 4.0, 0), false, false));
                PacketInterceptor.replay(client, new ServerboundMovePlayerPacket.Pos(player.position(), true, false));
            }
        }
        setEnabled(false);
    }
}
