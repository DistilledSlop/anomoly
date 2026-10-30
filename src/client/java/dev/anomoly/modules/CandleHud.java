package dev.anomoly.modules;

import dev.anomoly.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class CandleHud extends Module implements HudDrawable {
    public CandleHud() {
        super("candle_hud", "CandleHud", "Draw a small animated candle in the corner of the HUD.", Category.RENDER);
    }

    @Override public void draw(Minecraft client, GuiGraphicsExtractor graphics) {
        int x = graphics.guiWidth() - 20;
        int y = graphics.guiHeight() - 40;
        long t = System.currentTimeMillis() / 120;
        int flicker = (int)(t % 3);
        graphics.fill(x, y + 8, x + 6, y + 28, 0xFFF2E9D0);
        graphics.fill(x + 2, y + 4 - flicker, x + 4, y + 8, 0xFFFFA630);
        graphics.fill(x + 2, y + 6 - flicker, x + 4, y + 8, 0xFFFFE066);
    }
}
