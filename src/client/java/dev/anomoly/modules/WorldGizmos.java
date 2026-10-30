package dev.anomoly.modules;

import net.minecraft.client.Minecraft;

/** Runs world-space drawing so its gizmos live for exactly the coming tick. */
final class WorldGizmos {
    private WorldGizmos() { }

    static void draw(Minecraft client, Runnable drawing) {
        try (var ignored = client.collectPerTickGizmos()) {
            drawing.run();
        }
    }
}
