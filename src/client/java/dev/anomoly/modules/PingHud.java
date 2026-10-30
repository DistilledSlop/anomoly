package dev.anomoly.modules;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;

public final class PingHud extends TextHud {
    public PingHud() {
        super("ping_hud", "PingHud", "Show the server-reported latency on the HUD.");
    }

    @Override protected List<String> lines(Minecraft client) {
        PlayerInfo info = client.getConnection() == null ? null : client.getConnection().getPlayerInfo(client.player.getUUID());
        return List.of(info == null ? "Ping --" : "Ping " + info.getLatency() + " ms");
    }
}
