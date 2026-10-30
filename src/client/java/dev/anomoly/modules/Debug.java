package dev.anomoly.modules;

import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;

public final class Debug extends TextHud {
    public Debug() {
        super("debug", "Debug", "Show diagnostic counters on the HUD: entities, loaded chunks, tick count and server silence.");
    }

    @Override protected List<String> lines(Minecraft client) {
        int entities = 0;
        for (var ignored : client.level.entitiesForRendering()) entities++;
        return List.of("Entities " + entities, "Ticks " + client.player.tickCount, String.format(Locale.ROOT, "Last packet %.2fs ago, TPS %.1f",
            ServerClock.secondsSincePacket(), ServerClock.tps()), "Chunks " + client.level.getChunkSource().getLoadedChunksCount());
    }
}
