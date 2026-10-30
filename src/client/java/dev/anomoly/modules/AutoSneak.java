package dev.anomoly.modules;

import dev.anomoly.Module;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public final class AutoSneak extends Module {
    public AutoSneak() {
        super("auto_sneak", "Auto Sneak", "Hold sneak while playing.", Category.MOVEMENT);
    }

    @Override public void tick(Minecraft client) {
        if (client.gui.screen() == null) client.options.keyShift.setDown(true);
        else client.options.keyShift.setDown(false);
    }

    @Override protected void onDisable(Minecraft client) { KeyMapping.setAll(); }
}
