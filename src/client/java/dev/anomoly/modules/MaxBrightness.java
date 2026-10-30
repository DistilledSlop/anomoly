package dev.anomoly.modules;

import dev.anomoly.Module;
import net.minecraft.client.Minecraft;

public final class MaxBrightness extends Module {
    private double previous = 0.5;

    public MaxBrightness() {
        super("max_brightness", "Max Brightness", "Set client brightness to the vanilla maximum.", Category.RENDER);
    }

    @Override protected void onEnable(Minecraft client) {
        previous = client.options.gamma().get();
        client.options.gamma().set(1.0);
    }

    @Override protected void onDisable(Minecraft client) { client.options.gamma().set(previous); }
    @Override public void tick(Minecraft client) { client.options.gamma().set(1.0); }
}
