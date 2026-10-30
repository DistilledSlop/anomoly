package dev.anomoly.modules;

import dev.anomoly.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class HotbarBackdrop extends Module implements HudDrawable {
    public HotbarBackdrop() {
        super("hotbar", "Hotbar", "Draw a tinted bar and highlight behind the hotbar slots.", Category.RENDER);
    }

    @Override public void draw(Minecraft client, GuiGraphicsExtractor graphics) {
        int left = graphics.guiWidth() / 2 - 91;
        int top = graphics.guiHeight() - 22;
        graphics.fill(left, top, left + 182, top + 22, 0x6055DDBB);
        int selected = client.player.getInventory().getSelectedSlot();
        graphics.fill(left + selected * 20, top, left + selected * 20 + 22, top + 22, 0x80FFFFFF);
    }
}
