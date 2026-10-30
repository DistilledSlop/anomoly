package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class NoFall extends Module {
    private final FloatSetting threshold = add(new FloatSetting("threshold", "Fall distance", 2.5F, 2.0F, 10.0F));

    public NoFall() {
        super("no_fall", "No Fall", "Report landing after a damaging fall to prevent vanilla fall damage.", Category.MOVEMENT);
    }

    public boolean spoofGround(LocalPlayer player) {
        return enabled() && !player.onGround() && player.fallDistance >= threshold.get()
            && player.getDeltaMovement().y < 0 && !player.isInWater() && !player.isInLava()
            && !player.isFallFlying() && !player.getAbilities().flying;
    }
}
