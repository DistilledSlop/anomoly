package dev.anomoly.modules;

import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;

public final class HudSummary extends TextHud {
    public HudSummary() {
        super("hud", "HUD", "Show a compact overlay of FPS, coordinates, facing, health and held item.");
    }

    @Override protected List<String> lines(Minecraft client) {
        var p = client.player;
        return List.of(client.getFps() + " FPS", String.format(Locale.ROOT, "%.1f %.1f %.1f  %s", p.getX(), p.getY(), p.getZ(), p.getDirection().getName()),
            String.format(Locale.ROOT, "HP %.1f  Food %d", p.getHealth(), p.getFoodData().getFoodLevel()), "Holding " + p.getMainHandItem().getHoverName().getString());
    }
}
