package dev.anomoly.modules;

import dev.anomoly.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public final class Freeze extends Module {
    public Freeze() {
        super("freeze", "Freeze", "Hold the player motionless in place, cancelling gravity and momentum.", Category.MOVEMENT);
    }

    @Override public void tick(Minecraft client) {
        var player = client.player;
        if (!player.isPassenger()) player.setDeltaMovement(Vec3.ZERO);
    }
}
