package dev.anomoly.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Solves the low arc using Minecraft's per-tick drag/gravity order and a linear target lead. */
final class ProjectileAimSolver {
    record Solution(float yaw, float pitch, double flightTicks) { }

    private ProjectileAimSolver() { }

    static Solution solve(Minecraft client, LivingEntity target, ProjectilePhysics.Profile physics, float leadScale) {
        Vec3 origin = client.player.getEyePosition().add(0, -physics.spawnBelowEye(), 0);
        Vec3 destination = target.getBoundingBox().getCenter();
        Vec3 movement = target.getDeltaMovement();
        Vec3 targetVelocity = new Vec3(movement.x, target.onGround() ? 0 : movement.y, movement.z).scale(leadScale);
        Vec3 shooterVelocity = physics.inheritsShooterMotion() ? client.player.getKnownMovement() : Vec3.ZERO;
        if (client.player.onGround()) shooterVelocity = new Vec3(shooterVelocity.x, 0, shooterVelocity.z);

        double previousTime = 0.01;
        double previousError = requiredVelocity(origin, destination, targetVelocity, shooterVelocity, physics, previousTime).length() - physics.speed();
        for (double time = 0.26; time <= 90; time += 0.25) {
            Vec3 required = requiredVelocity(origin, destination, targetVelocity, shooterVelocity, physics, time);
            double error = required.length() - physics.speed();
            if (previousError > 0 && error <= 0) {
                double low = previousTime;
                double high = time;
                for (int iteration = 0; iteration < 12; iteration++) {
                    double middle = (low + high) / 2;
                    if (requiredVelocity(origin, destination, targetVelocity, shooterVelocity, physics, middle).length() > physics.speed()) low = middle;
                    else high = middle;
                }
                double flight = (low + high) / 2;
                Vec3 shot = requiredVelocity(origin, destination, targetVelocity, shooterVelocity, physics, flight);
                if (!Double.isFinite(shot.length()) || shot.lengthSqr() < 0.01) return null;
                if (!trajectoryClear(client, target, origin, targetVelocity, shooterVelocity, shot.normalize().scale(physics.speed()), physics, flight)) return null;
                float yaw = (float)(Math.toDegrees(Math.atan2(shot.z, shot.x)) - 90.0);
                float pitch = (float)-Math.toDegrees(Math.atan2(shot.y, Math.hypot(shot.x, shot.z))) + physics.aimPitchOffset();
                return new Solution(yaw, pitch, flight);
            }
            previousTime = time;
            previousError = error;
        }
        return null;
    }

    private static Vec3 requiredVelocity(Vec3 origin, Vec3 destination, Vec3 targetVelocity, Vec3 shooterVelocity,
                                         ProjectilePhysics.Profile physics, double ticks) {
        double drag = physics.drag();
        double distanceFactor;
        double gravityDrop;
        if (Math.abs(1.0 - drag) < 1E-6) {
            distanceFactor = ticks;
            gravityDrop = physics.gravity() * ticks * (ticks + (physics.gravityBeforeMove() ? 1 : -1)) / 2;
        } else {
            double series = -Math.expm1(ticks * Math.log(drag)) / (1.0 - drag);
            distanceFactor = physics.dragBeforeMove() ? drag * series : series;
            gravityDrop = physics.gravity() * (physics.dragBeforeMove() ? drag : 1.0)
                / (1.0 - drag) * (ticks - (physics.gravityBeforeMove() ? drag * series : series));
        }
        Vec3 delta = destination.add(targetVelocity.scale(ticks)).subtract(origin);
        return delta.add(0, gravityDrop, 0).scale(1.0 / distanceFactor).subtract(shooterVelocity);
    }

    private static boolean trajectoryClear(Minecraft client, LivingEntity target, Vec3 origin, Vec3 targetVelocity,
                                           Vec3 shooterVelocity, Vec3 launchVelocity, ProjectilePhysics.Profile physics,
                                           double flightTicks) {
        Vec3 position = origin;
        Vec3 velocity = launchVelocity.add(shooterVelocity);
        int steps = (int)Math.ceil(flightTicks);
        for (int step = 1; step <= steps; step++) {
            if (physics.gravityBeforeMove()) velocity = velocity.add(0, -physics.gravity(), 0);
            if (physics.dragBeforeMove()) velocity = velocity.scale(physics.drag());
            Vec3 next = position.add(velocity);
            var targetHit = target.getBoundingBox().move(targetVelocity.scale(step)).inflate(0.2).clip(position, next);
            BlockHitResult blockHit = client.level.clip(new ClipContext(position, next,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, client.player));
            if (targetHit.isPresent() && (blockHit.getType() == net.minecraft.world.phys.HitResult.Type.MISS
                || position.distanceToSqr(targetHit.get()) <= position.distanceToSqr(blockHit.getLocation()))) return true;
            if (blockHit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) return false;
            position = next;
            if (!physics.dragBeforeMove()) velocity = velocity.scale(physics.drag());
            if (!physics.gravityBeforeMove()) velocity = velocity.add(0, -physics.gravity(), 0);
        }
        return true;
    }
}
