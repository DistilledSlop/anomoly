package dev.anomoly.modules;

import dev.anomoly.Module;
import java.awt.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class Lsd extends Module implements HudDrawable {
    public Lsd() {
        super("lsd", "Lsd", "Wash the screen in slowly cycling translucent colors (also covers RainbowUi).", Category.FUN);
    }

    @Override public void draw(Minecraft client, GuiGraphicsExtractor graphics) {
        float hue = (System.currentTimeMillis() % 6000) / 6000F;
        int rgb = Color.HSBtoRGB(hue, 0.8F, 1.0F) & 0xFFFFFF;
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), 0x30000000 | rgb);
    }
}
