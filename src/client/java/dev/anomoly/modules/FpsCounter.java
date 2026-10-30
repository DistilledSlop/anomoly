package dev.anomoly.modules;

import java.util.List;
import net.minecraft.client.Minecraft;

public final class FpsCounter extends TextHud {
    public FpsCounter() {
        super("fps_counter", "FpsCounter", "Show the current frames per second on the HUD.");
    }

    @Override protected List<String> lines(Minecraft client) { return List.of(client.getFps() + " FPS"); }
}
