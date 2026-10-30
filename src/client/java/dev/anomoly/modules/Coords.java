package dev.anomoly.modules;

import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;

public final class Coords extends TextHud {
    public Coords() {
        super("coords", "Coords", "Show your position, facing direction, and the matching Nether or Overworld coordinates on the HUD.");
    }

    @Override protected List<String> lines(Minecraft client) {
        var player = client.player;
        String position = String.format(Locale.ROOT, "XYZ %.1f %.1f %.1f  %s", player.getX(), player.getY(), player.getZ(),
            player.getDirection().getName());
        boolean nether = client.level.dimension() == Level.NETHER;
        if (!nether && client.level.dimension() != Level.OVERWORLD) return List.of(position);
        double scale = nether ? 8 : 0.125;
        return List.of(position, String.format(Locale.ROOT, "%s %.1f %.1f", nether ? "Overworld" : "Nether",
            player.getX() * scale, player.getZ() * scale));
    }
}
