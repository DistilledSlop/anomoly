package dev.anomoly.modules;

import dev.anomoly.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Jump just before horizontal movement would leave the current support. */
public final class Parkour extends Module {
    public Parkour() {
        super("parkour", "Parkour", "Jump automatically at the edge of a block while moving.", Category.MOVEMENT);
    }

    @Override public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (client.gui.screen() != null || !player.onGround() || player.isShiftKeyDown()
            || player.isInWater() || player.isInLava() || player.isPassenger()
            || player.isFallFlying() || client.options.keyJump.isDown()) return;

        Vec3 velocity = player.getDeltaMovement();
        double dx = velocity.x;
        double dz = velocity.z;
        if (dx * dx + dz * dz < 1.0E-4) return;

        AABB nextStep = player.getBoundingBox().move(dx, -0.08, dz);
        if (!client.level.hasChunkAt(BlockPos.containing(nextStep.getCenter()))) return;
        if (client.level.noCollision(player, nextStep)) player.jumpFromGround();
    }
}
