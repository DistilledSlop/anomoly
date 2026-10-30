package dev.anomoly.modules;

import dev.anomoly.Module;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public final class AutoWalk extends Module {
    public AutoWalk() {
        super("auto_walk", "Auto Walk", "Hold forward while playing.", Category.MOVEMENT);
    }

    @Override public void tick(Minecraft client) {
        if (client.gui.screen() == null) client.options.keyUp.setDown(true);
        else client.options.keyUp.setDown(false);
    }

    @Override protected void onDisable(Minecraft client) {
        KeyMapping.setAll();
    }
}
