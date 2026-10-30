package dev.anomoly.modules;

import dev.anomoly.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class Companion extends Module implements HudDrawable {
    private String speech = "";
    private long speechUntil;

    public Companion() {
        super("companion", "Companion", "Draw a small bobbing HUD companion that comments on totem pops.", Category.RENDER);
    }

    public void say(String text) {
        speech = text;
        speechUntil = System.currentTimeMillis() + 4000;
    }

    @Override public void draw(Minecraft client, GuiGraphicsExtractor graphics) {
        int bob = (int)(Math.sin(System.currentTimeMillis() / 250.0) * 2);
        int x = 10, y = graphics.guiHeight() - 60 + bob;
        graphics.fill(x, y, x + 14, y + 14, 0xFF55DDBB);
        graphics.fill(x + 3, y + 4, x + 5, y + 6, 0xFF101420);
        graphics.fill(x + 9, y + 4, x + 11, y + 6, 0xFF101420);
        graphics.fill(x + 4, y + 9, x + 10, y + 10, 0xFF101420);
        if (System.currentTimeMillis() < speechUntil) graphics.text(client.font, speech, x + 18, y + 3, 0xFFE4F6F3);
    }
}
