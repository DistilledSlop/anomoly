package dev.anomoly.modules;

import dev.anomoly.AnomolyClient;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Shared 26.3 crystal target selection, spawn clearance, and blast scoring. */
final class CrystalCombat {
    private CrystalCombat() { }

    static List<LivingEntity> targets(Minecraft client, float range, boolean targetMobs) {
        List<LivingEntity> result = new ArrayList<>();
        double rangeSq = range * range;
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || living == client.player || !living.isAlive()
                || living.damageCooldownTime > 10 || client.player.distanceToSqr(living) > rangeSq) continue;
            if (living instanceof Player player) {
                if (!client.player.canHarmPlayer(player)
                    || AnomolyClient.MODULES.get(AntiBot.class).reject(client, player)) continue;
            } else if (!targetMobs) continue;
            result.add(living);
        }
        return result;
    }

    static boolean freeCrystalVolume(Minecraft client, BlockPos base) {
        BlockPos above = base.above();
        return client.level.isEmptyBlock(above)
            && client.level.getEntities(null, new AABB(base.getX(), above.getY(), base.getZ(),
                base.getX() + 1, above.getY() + 2, base.getZ() + 1)).isEmpty();
    }

    static double score(Minecraft client, Vec3 explosion, List<LivingEntity> targets,
                        float minTargetDamage, float maxSelfDamage, float minAdvantage, float healthReserve) {
        return score(client, explosion, targets, minTargetDamage, maxSelfDamage,
            minAdvantage, healthReserve, null);
    }

    static double projectedScore(Minecraft client, Vec3 explosion, List<LivingEntity> targets,
                                 float minTargetDamage, float maxSelfDamage, float minAdvantage,
                                 float healthReserve, BlockPos futureBase) {
        return score(client, explosion, targets, minTargetDamage, maxSelfDamage,
            minAdvantage, healthReserve, futureBase);
    }

    private static double score(Minecraft client, Vec3 explosion, List<LivingEntity> targets,
                                float minTargetDamage, float maxSelfDamage, float minAdvantage,
                                float healthReserve, BlockPos futureBase) {
        float targetDamage = 0F;
        for (LivingEntity target : targets) targetDamage = Math.max(targetDamage,
            futureBase == null ? rawDamage(explosion, target) : projectedDamage(explosion, target, futureBase));
        if (targetDamage < minTargetDamage) return Double.NEGATIVE_INFINITY;
        boolean invulnerable = client.player.getAbilities().invulnerable;
        float self = invulnerable ? 0F : rawDamage(explosion, client.player);
        if (!invulnerable && (self > maxSelfDamage
            || client.player.getHealth() + client.player.getAbsorptionAmount() - self <= healthReserve)
            || targetDamage - self < minAdvantage)
            return Double.NEGATIVE_INFINITY;
        return targetDamage - self * 1.25;
    }

    static float rawDamage(Vec3 explosion, LivingEntity entity) {
        double distance = Math.sqrt(entity.distanceToSqr(explosion)) / 12.0;
        if (distance > 1.0) return 0F;
        double power = (1.0 - distance) * ServerExplosion.getSeenPercent(explosion, entity);
        return (float)((power * power + power) / 2.0 * 84.0 + 1.0);
    }

    private static float projectedDamage(Vec3 explosion, LivingEntity entity, BlockPos futureBase) {
        double distance = Math.sqrt(entity.distanceToSqr(explosion)) / 12.0;
        if (distance > 1.0) return 0F;
        AABB bb = entity.getBoundingBox();
        AABB obstruction = new AABB(futureBase).inflate(-1.0E-5);
        double xs = 1.0 / ((bb.maxX - bb.minX) * 2.0 + 1.0);
        double ys = 1.0 / ((bb.maxY - bb.minY) * 2.0 + 1.0);
        double zs = 1.0 / ((bb.maxZ - bb.minZ) * 2.0 + 1.0);
        double xOffset = (1.0 - Math.floor(1.0 / xs) * xs) / 2.0;
        double zOffset = (1.0 - Math.floor(1.0 / zs) * zs) / 2.0;
        int hits = 0;
        int count = 0;
        for (double xx = 0.0; xx <= 1.0; xx += xs)
            for (double yy = 0.0; yy <= 1.0; yy += ys)
                for (double zz = 0.0; zz <= 1.0; zz += zs) {
                    Vec3 from = new Vec3(Mth.lerp(xx, bb.minX, bb.maxX) + xOffset,
                        Mth.lerp(yy, bb.minY, bb.maxY), Mth.lerp(zz, bb.minZ, bb.maxZ) + zOffset);
                    if (obstruction.clip(from, explosion).isEmpty()
                        && entity.level().clip(new ClipContext(from, explosion,
                            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity)).getType()
                            == HitResult.Type.MISS) hits++;
                    count++;
                }
        float exposure = count == 0 ? 0F : (float)hits / count;
        double power = (1.0 - distance) * exposure;
        return (float)((power * power + power) / 2.0 * 84.0 + 1.0);
    }
}
