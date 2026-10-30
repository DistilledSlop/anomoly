package dev.anomoly.modules;

import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;

public final class LegacyHud extends TextHud {
    public LegacyHud() {
        super("legacy_hud", "LegacyHud", "Show a compact text HUD with FPS, ping, position and health.");
    }

    @Override protected List<String> lines(Minecraft client) {
        var player = client.player;
        PlayerInfo info = client.getConnection().getPlayerInfo(player.getUUID());
        return List.of(client.getFps() + " FPS  " + (info == null ? "--" : info.getLatency()) + " ms",
            String.format(Locale.ROOT, "%.0f %.0f %.0f  HP %.1f", player.getX(), player.getY(), player.getZ(), player.getHealth()));
    }
}
