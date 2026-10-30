package dev.anomoly.modules;

import dev.anomoly.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public final class Strafe extends Module {
    public Strafe() {
        super("strafe", "Strafe", "Redirect midair momentum to follow the pressed movement keys without losing speed.", Category.MOVEMENT);
    }

    @Override public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (client.gui.screen() != null || player.onGround() || !MoveUtil.moving(player) || !MoveUtil.freeMovement(player)
            || player.isInWater() || player.isInLava() || player.onClimbable()) return;
        double speed = MoveUtil.horizontalSpeed(player);
        if (speed < 0.05) return;
        Vec3 heading = MoveUtil.direction(player, speed);
        MoveUtil.setHorizontal(player, heading);
    }
}
