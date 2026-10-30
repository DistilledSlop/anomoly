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

/** Vanilla 26.3 respawn-anchor blast scoring, including removal of the detonated block. */
final class AnchorCombat {
    private AnchorCombat() { }

    static double score(Minecraft client, BlockPos anchor, List<LivingEntity> targets,
                        float minTargetDamage, float maxSelfDamage,
                        float minAdvantage, float healthReserve) {
        Vec3 center = Vec3.atCenterOf(anchor);
        float targetDamage = 0F;
        for (LivingEntity target : targets)
            targetDamage = Math.max(targetDamage, rawDamage(center, anchor, target));
        if (targetDamage < minTargetDamage) return Double.NEGATIVE_INFINITY;
        boolean invulnerable = client.player.getAbilities().invulnerable;
        float self = invulnerable ? 0F : rawDamage(center, anchor, client.player);
        if (!invulnerable && (self > maxSelfDamage
            || client.player.getHealth() + client.player.getAbsorptionAmount() - self <= healthReserve)
            || targetDamage - self < minAdvantage) return Double.NEGATIVE_INFINITY;
        return targetDamage - self * 1.25;
    }

    private static float rawDamage(Vec3 center, BlockPos anchor, LivingEntity entity) {
        double distance = Math.sqrt(entity.distanceToSqr(center)) / 10.0;
        if (distance > 1.0) return 0F;
        double power = (1.0 - distance) * seenPercent(center, anchor, entity);
        return (float)((power * power + power) / 2.0 * 70.0 + 1.0);
    }

    private static float seenPercent(Vec3 center, BlockPos anchor, LivingEntity entity) {
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
                    HitResult ray = entity.level().clip(new ClipContext(from, center,
                        ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
                    if (ray.getType() == HitResult.Type.MISS
                        || ray instanceof BlockHitResult block && block.getBlockPos().equals(anchor)) seen++;
                    samples++;
                }
        return samples == 0 ? 0F : (float)seen / samples;
    }
}
