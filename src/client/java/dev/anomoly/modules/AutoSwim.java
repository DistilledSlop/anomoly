package dev.anomoly.modules;

import dev.anomoly.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/** Keeps forward swimming in its sprint pose while submerged. */
public final class AutoSwim extends Module {
    public AutoSwim() {
        super("auto_swim", "Auto Swim", "Enter and maintain the swimming pose while moving forward underwater.", Category.MOVEMENT);
    }

    @Override public void tick(Minecraft client) {
        if (client.gui.screen() != null || client.player == null) return;
        LocalPlayer player = client.player;
        if (!player.isInWater() || (!player.isUnderWater() && !player.isSwimming())
            || player.horizontalCollision || player.isShiftKeyDown() || player.getAbilities().flying
            || !player.input.hasForwardImpulse()) return;
        player.setSprinting(true);
    }
}
