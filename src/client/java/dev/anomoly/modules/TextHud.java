package dev.anomoly.modules;

import dev.anomoly.AnomolyClient;
import dev.anomoly.Module;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** A module whose only effect is a block of text stacked in the HUD's top-left corner. */
public abstract class TextHud extends Module {
    protected TextHud(String id, String name, String description) {
        super(id, name, description, Category.RENDER);
    }

    protected abstract List<String> lines(Minecraft client);

    public static void drawAll(Minecraft client, GuiGraphicsExtractor graphics) {
        if (client.player == null || client.level == null) return;
        int y = 4;
        for (Module module : AnomolyClient.MODULES.all()) {
            if (!(module instanceof TextHud hud) || !module.enabled()) continue;
            for (String line : hud.lines(client)) {
                graphics.fill(2, y - 1, 6 + client.font.width(line), y + 9, 0x80101420);
                graphics.text(client.font, line, 4, y, 0xFFE4F6F3);
                y += 11;
            }
        }
    }
}
