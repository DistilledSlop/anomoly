package dev.anomoly.modules;

import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;

public final class Speedometer extends TextHud {
    private double lastX, lastY, lastZ, speed;
    private boolean seeded;

    public Speedometer() {
        super("speedometer", "Speedometer", "Show your measured travel speed in blocks per second on the HUD.");
    }

    @Override public void tick(Minecraft client) {
        var player = client.player;
        if (seeded) speed = Math.sqrt(Math.pow(player.getX() - lastX, 2) + Math.pow(player.getZ() - lastZ, 2)) * 20;
        lastX = player.getX();
        lastY = player.getY();
        lastZ = player.getZ();
        seeded = true;
    }

    @Override protected void onDisable(Minecraft client) { seeded = false; }

    @Override protected List<String> lines(Minecraft client) {
        return List.of(String.format(Locale.ROOT, "%.2f b/s", speed));
    }
}
