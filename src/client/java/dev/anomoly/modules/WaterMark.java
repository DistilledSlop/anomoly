package dev.anomoly.modules;

import java.util.List;
import net.minecraft.client.Minecraft;

public final class WaterMark extends TextHud {
    public WaterMark() {
        super("watermark", "WaterMark", "Show the Anomoly name and version on the HUD.");
    }

    @Override protected List<String> lines(Minecraft client) { return List.of("Anomoly 1.0.0"); }
}
