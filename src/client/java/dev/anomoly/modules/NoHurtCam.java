package dev.anomoly.modules;

import dev.anomoly.Module;
import net.minecraft.client.Minecraft;

public final class NoHurtCam extends Module {
    private double previous = 1.0;

    public NoHurtCam() {
        super("no_hurt_cam", "No Hurt Cam", "Remove the damage tilt effect.", Category.RENDER);
    }

    @Override protected void onEnable(Minecraft client) {
        previous = client.options.damageTiltStrength().get();
        client.options.damageTiltStrength().set(0.0);
    }

    @Override protected void onDisable(Minecraft client) { client.options.damageTiltStrength().set(previous); }
    @Override public void tick(Minecraft client) { client.options.damageTiltStrength().set(0.0); }
}
