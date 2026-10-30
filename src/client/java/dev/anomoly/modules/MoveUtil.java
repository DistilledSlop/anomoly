package dev.anomoly.modules;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

/** Shared helpers for modules that steer the local player's motion. */
final class MoveUtil {
    private MoveUtil() { }

    static boolean moving(LocalPlayer player) {
        return player.input.getMoveVector().lengthSquared() > 1.0E-4F;
    }

    /** Horizontal world-space motion of the given magnitude along the player's pressed movement keys. */
    static Vec3 direction(LocalPlayer player, double speed) {
        Vec2 input = player.input.getMoveVector();
        double radians = Math.toRadians(player.getYRot());
        double dx = input.x * Math.cos(radians) - input.y * Math.sin(radians);
        double dz = input.x * Math.sin(radians) + input.y * Math.cos(radians);
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 1.0E-4) return Vec3.ZERO;
        return new Vec3(dx / length * speed, 0, dz / length * speed);
    }

    static double horizontalSpeed(LocalPlayer player) {
        Vec3 velocity = player.getDeltaMovement();
        return Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
    }

    static boolean freeMovement(LocalPlayer player) {
        return !player.isPassenger() && !player.isFallFlying() && !player.getAbilities().flying;
    }

    static void setHorizontal(LocalPlayer player, Vec3 horizontal) {
        player.setDeltaMovement(horizontal.x, player.getDeltaMovement().y, horizontal.z);
    }
}
