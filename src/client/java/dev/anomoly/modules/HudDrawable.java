package dev.anomoly.modules;

import dev.anomoly.AnomolyClient;
import dev.anomoly.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Implemented by modules that paint directly onto the HUD each frame. */
public interface HudDrawable {
    void draw(Minecraft client, GuiGraphicsExtractor graphics);

    static void drawAll(Minecraft client, GuiGraphicsExtractor graphics) {
        if (client.player == null || client.level == null) return;
        for (Module module : AnomolyClient.MODULES.all()) {
            if (module.enabled() && module instanceof HudDrawable drawable) drawable.draw(client, graphics);
        }
    }
}
