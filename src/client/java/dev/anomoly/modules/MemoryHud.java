package dev.anomoly.modules;

import java.util.List;
import net.minecraft.client.Minecraft;

public final class MemoryHud extends TextHud {
    public MemoryHud() {
        super("memory_hud", "MemoryHud", "Show used and allocated Java heap memory on the HUD.");
    }

    @Override protected List<String> lines(Minecraft client) {
        Runtime runtime = Runtime.getRuntime();
        long used = (runtime.totalMemory() - runtime.freeMemory()) >> 20;
        return List.of("Mem " + used + " / " + (runtime.maxMemory() >> 20) + " MB");
    }
}
