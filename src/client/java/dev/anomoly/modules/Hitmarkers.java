package dev.anomoly.modules;

import dev.anomoly.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;

public final class Hitmarkers extends Module implements HitEvents.Listener {
    private long hitAt;

    public Hitmarkers() {
        super("hitmarkers", "Hitmarkers", "Flash an X at the crosshair each time you attack an entity.", Category.RENDER);
    }

    @Override public void onHit(Entity target) { hitAt = System.currentTimeMillis(); }

    public void draw(Minecraft client, GuiGraphicsExtractor graphics) {
        long age = System.currentTimeMillis() - hitAt;
        if (!enabled() || age > 250) return;
        int color = ((int)(255 * (1 - age / 250.0)) << 24) | 0xFFFFFF;
        int cx = graphics.guiWidth() / 2;
        int cy = graphics.guiHeight() / 2;
        for (int i = 4; i <= 9; i++) {
            graphics.fill(cx + i, cy + i, cx + i + 1, cy + i + 1, color);
            graphics.fill(cx - i - 1, cy + i, cx - i, cy + i + 1, color);
            graphics.fill(cx + i, cy - i - 1, cx + i + 1, cy - i, color);
            graphics.fill(cx - i - 1, cy - i - 1, cx - i, cy - i, color);
        }
    }
}
