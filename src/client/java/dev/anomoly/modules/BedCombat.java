package dev.anomoly.modules;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Scores a 26.3 bed explosion after both bed halves are removed by the server. */
final class BedCombat {
    private BedCombat() { }

    static double score(Minecraft client, BlockPos foot, BlockPos head, List<LivingEntity> targets,
                        float minTargetDamage, float maxSelfDamage,
                        float minAdvantage, float healthReserve) {
        Vec3 center = Vec3.atCenterOf(head);
        float targetDamage = 0F;
        for (LivingEntity target : targets)
            targetDamage = Math.max(targetDamage, rawDamage(center, foot, head, target));
        if (targetDamage < minTargetDamage) return Double.NEGATIVE_INFINITY;
        boolean invulnerable = client.player.getAbilities().invulnerable;
        float self = invulnerable ? 0F : rawDamage(center, foot, head, client.player);
        if (!invulnerable && (self > maxSelfDamage
            || client.player.getHealth() + client.player.getAbsorptionAmount() - self <= healthReserve)
            || targetDamage - self < minAdvantage) return Double.NEGATIVE_INFINITY;
        return targetDamage - self * 1.25;
    }

    private static float rawDamage(Vec3 center, BlockPos foot, BlockPos head, LivingEntity entity) {
        double distance = Math.sqrt(entity.distanceToSqr(center)) / 10.0;
        if (distance > 1.0) return 0F;
        double power = (1.0 - distance) * seenPercent(center, foot, head, entity);
        return (float)((power * power + power) / 2.0 * 70.0 + 1.0);
    }

    private static float seenPercent(Vec3 center, BlockPos foot, BlockPos head, LivingEntity entity) {
        AABB box = entity.getBoundingBox();
        double xs = 1.0 / ((box.maxX - box.minX) * 2.0 + 1.0);
        double ys = 1.0 / ((box.maxY - box.minY) * 2.0 + 1.0);
        double zs = 1.0 / ((box.maxZ - box.minZ) * 2.0 + 1.0);
        double xOffset = (1.0 - Math.floor(1.0 / xs) * xs) / 2.0;
        double zOffset = (1.0 - Math.floor(1.0 / zs) * zs) / 2.0;
        int seen = 0;
        int samples = 0;
        for (double xx = 0.0; xx <= 1.0; xx += xs)
            for (double yy = 0.0; yy <= 1.0; yy += ys)
                for (double zz = 0.0; zz <= 1.0; zz += zs) {
                    Vec3 from = new Vec3(Mth.lerp(xx, box.minX, box.maxX) + xOffset,
                        Mth.lerp(yy, box.minY, box.maxY), Mth.lerp(zz, box.minZ, box.maxZ) + zOffset);
                    if (visible(entity, from, center, foot, head)) seen++;
                    samples++;
                }
        return samples == 0 ? 0F : (float)seen / samples;
    }

    private static boolean visible(LivingEntity entity, Vec3 from, Vec3 center,
                                   BlockPos foot, BlockPos head) {
        Vec3 start = from;
        for (int skipped = 0; skipped <= 2; skipped++) {
            HitResult ray = entity.level().clip(new ClipContext(start, center,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
            if (ray.getType() == HitResult.Type.MISS) return true;
            if (!(ray instanceof BlockHitResult block)) return false;
            BlockPos pos = block.getBlockPos();
            if (!pos.equals(foot) && !pos.equals(head)) return false;
            Vec3 direction = center.subtract(start).normalize();
            Vec3 inside = ray.getLocation().add(direction.scale(1.0E-5));
            double exit = distanceToExit(inside, direction, pos);
            start = inside.add(direction.scale(exit + 1.0E-4));
            if (start.distanceToSqr(from) >= center.distanceToSqr(from)) return true;
        }
        return false;
    }

    private static double distanceToExit(Vec3 inside, Vec3 direction, BlockPos pos) {
        double distance = Double.POSITIVE_INFINITY;
        if (direction.x > 0) distance = Math.min(distance, (pos.getX() + 1.0 - inside.x) / direction.x);
        else if (direction.x < 0) distance = Math.min(distance, (pos.getX() - inside.x) / direction.x);
        if (direction.y > 0) distance = Math.min(distance, (pos.getY() + 1.0 - inside.y) / direction.y);
        else if (direction.y < 0) distance = Math.min(distance, (pos.getY() - inside.y) / direction.y);
        if (direction.z > 0) distance = Math.min(distance, (pos.getZ() + 1.0 - inside.z) / direction.z);
        else if (direction.z < 0) distance = Math.min(distance, (pos.getZ() - inside.z) / direction.z);
        return Math.max(0.0, distance);
    }
}
