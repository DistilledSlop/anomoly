package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class DamageTint extends Module implements HudDrawable {
    private final FloatSetting threshold = add(new FloatSetting("threshold", "Tint below health", 8, 2, 20));

    public DamageTint() {
        super("damage_tint", "DamageTint", "Tint the whole screen red as your health drops below a threshold.", Category.RENDER);
    }

    @Override public void draw(Minecraft client, GuiGraphicsExtractor graphics) {
        float health = client.player.getHealth();
        if (health >= threshold.get()) return;
        int alpha = (int)(110 * (1 - health / threshold.get()));
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), (alpha << 24) | 0xAA0000);
    }
}
