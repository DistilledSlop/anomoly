package dev.anomoly.modules;

import dev.anomoly.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class FastAccel extends Module {
    public FastAccel() {
        super("fast_accel", "FastAccel", "Reach full walking or sprinting speed instantly on the ground instead of accelerating.", Category.MOVEMENT);
    }

    @Override public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (client.gui.screen() != null || !player.onGround() || !MoveUtil.moving(player) || !MoveUtil.freeMovement(player)
            || player.isInWater() || player.isInLava() || player.onClimbable()) return;
        MoveUtil.setHorizontal(player, MoveUtil.direction(player, player.getSpeed() * 2.1585));
    }
}
