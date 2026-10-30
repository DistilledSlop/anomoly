package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.phys.EntityHitResult;

public final class Crosshair extends Module implements HudDrawable {
    private final IntSetting size = add(new IntSetting("size", "Arm length", 5, 2, 12));
    private final IntSetting gap = add(new IntSetting("gap", "Center gap", 2, 0, 8));

    public Crosshair() {
        super("crosshair", "Crosshair", "Overlay a thin custom crosshair that turns red while an entity is under it.", Category.RENDER);
    }

    @Override public void draw(Minecraft client, GuiGraphicsExtractor graphics) {
        int cx = graphics.guiWidth() / 2;
        int cy = graphics.guiHeight() / 2;
        int color = client.hitResult instanceof EntityHitResult ? 0xFFFF5555 : 0xFF55DDBB;
        int g = gap.get(), s = size.get();
        graphics.fill(cx - g - s, cy, cx - g, cy + 1, color);
        graphics.fill(cx + g + 1, cy, cx + g + s + 1, cy + 1, color);
        graphics.fill(cx, cy - g - s, cx + 1, cy - g, color);
        graphics.fill(cx, cy + g + 1, cx + 1, cy + g + s + 1, color);
    }
}
