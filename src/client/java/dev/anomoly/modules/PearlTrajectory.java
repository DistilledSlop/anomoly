package dev.anomoly.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Simulates Minecraft 26.3 ender pearls from their current position and velocity. */
final class PearlTrajectory {
    record Landing(Vec3 position, Vec3 hit, int ticks) { }
    record Shot(float yaw, float pitch, Landing landing, double error) { }

    private PearlTrajectory() { }

    static Landing predict(Minecraft client, ThrownEnderpearl pearl) {
        return simulate(client, pearl.position(), pearl.getDeltaMovement(), pearl.getOwner(), pearl, 90);
    }

    static Shot findShot(Minecraft client, Vec3 destination, double maxError) {
        Vec3 origin = client.player.getEyePosition().add(0, -0.1, 0);
        Vec3 delta = destination.subtract(origin);
        float baseYaw = (float)(Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0);
        Shot best = null;
        for (int yawOffset = -12; yawOffset <= 12; yawOffset += 4)
            for (int pitch = -40; pitch <= 65; pitch += 5)
                best = better(client, destination, origin, baseYaw + yawOffset, pitch, best);
        if (best == null) return null;
        float coarseYaw = best.yaw();
        float coarsePitch = best.pitch();
        for (int yawOffset = -4; yawOffset <= 4; yawOffset++)
            for (int pitchOffset = -5; pitchOffset <= 5; pitchOffset++)
                best = better(client, destination, origin, coarseYaw + yawOffset, coarsePitch + pitchOffset, best);
        return best.error() <= maxError ? best : null;
    }

    private static Shot better(Minecraft client, Vec3 destination, Vec3 origin, float yaw, float pitch, Shot best) {
        if (pitch < -89 || pitch > 89) return best;
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        double horizontal = Math.cos(pitchRad) * 1.5;
        Vec3 shooterMotion = client.player.getKnownMovement();
        Vec3 velocity = new Vec3(-Math.sin(yawRad) * horizontal + shooterMotion.x,
            -Math.sin(pitchRad) * 1.5 + (client.player.onGround() ? 0 : shooterMotion.y),
            Math.cos(yawRad) * horizontal + shooterMotion.z);
        Landing landing = simulate(client, origin, velocity, client.player, null, 90);
        if (landing == null) return best;
        double error = landing.position().distanceTo(destination);
        return best == null || error < best.error() ? new Shot(yaw, pitch, landing, error) : best;
    }

    private static Landing simulate(Minecraft client, Vec3 start, Vec3 initialVelocity,
                                    Entity owner, Entity projectile, int maxTicks) {
        Vec3 position = start;
        Vec3 velocity = initialVelocity;
        for (int tick = 1; tick <= maxTicks; tick++) {
            if (!client.level.hasChunkAt(BlockPos.containing(position))) return null;
            float drag = client.level.getFluidState(BlockPos.containing(position)).is(FluidTags.WATER) ? 0.8F : 0.99F;
            velocity = velocity.add(0, -0.03, 0).scale(drag);
            Vec3 next = position.add(velocity);
            if (!client.level.hasChunkAt(BlockPos.containing(next))) return null;
            BlockHitResult block = client.level.clip(new ClipContext(position, next,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner == null ? client.player : owner));
            double nearest = block.getType() == HitResult.Type.MISS ? Double.POSITIVE_INFINITY
                : position.distanceToSqr(block.getLocation());
            Vec3 hit = block.getType() == HitResult.Type.MISS ? null : block.getLocation();
            for (Entity entity : client.level.entitiesForRendering()) {
                if (entity == owner || entity == projectile || entity instanceof ThrownEnderpearl
                    || !entity.canBeHitByProjectile()) continue;
                var clip = entity.getBoundingBox().inflate(entity.getPickRadius() + 0.1).clip(position, next);
                if (clip.isPresent() && position.distanceToSqr(clip.get()) < nearest) {
                    nearest = position.distanceToSqr(clip.get());
                    hit = clip.get();
                }
            }
            if (hit != null) return new Landing(position, hit, tick);
            position = next;
            if (position.distanceToSqr(start) > 96 * 96) return null;
        }
        return null;
    }
}
