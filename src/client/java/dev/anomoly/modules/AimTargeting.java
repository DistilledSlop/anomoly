package dev.anomoly.modules;

import dev.anomoly.AnomolyClient;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Shared visible-target selection and bounded view rotation for aiming modules. */
final class AimTargeting {
    record Solution(LivingEntity entity, float yaw, float pitch) { }

    private AimTargeting() { }

    static Solution find(Minecraft client, float range, float fieldOfView, boolean targetMobs, boolean targetInvisible) {
        if (client.player == null || client.level == null) return null;
        Solution best = null;
        double bestError = Double.POSITIVE_INFINITY;
        double bestDistance = Double.POSITIVE_INFINITY;
        double maxDistance = range * range;
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || entity == client.player || !living.isAlive()) continue;
            if (!(living instanceof Player) && !targetMobs) continue;
            if (living instanceof Player player && (!client.player.canHarmPlayer(player)
                || AnomolyClient.MODULES.get(AntiBot.class).reject(client, player))) continue;
            if (!targetInvisible && living.isInvisible()) continue;
            double distance = client.player.distanceToSqr(living);
            if (distance > maxDistance || !client.player.hasLineOfSight(living)) continue;
            Vec3 direction = living.getEyePosition().subtract(client.player.getEyePosition());
            float yaw = (float) (Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0);
            float pitch = (float) -Math.toDegrees(Math.atan2(direction.y, Math.hypot(direction.x, direction.z)));
            float yawError = Mth.wrapDegrees(yaw - client.player.getYRot());
            float pitchError = pitch - client.player.getXRot();
            if (Math.abs(yawError) > fieldOfView / 2F) continue;
            double error = Math.hypot(yawError, pitchError);
            if (error < bestError || error == bestError && distance < bestDistance) {
                best = new Solution(living, yaw, pitch);
                bestError = error;
                bestDistance = distance;
            }
        }
        return best;
    }

    static void turn(Minecraft client, Solution target, float maxDegreesPerTick, float deadZone) {
        if (target == null) return;
        float yawError = Mth.wrapDegrees(target.yaw() - client.player.getYRot());
        float pitchError = target.pitch() - client.player.getXRot();
        if (Math.abs(yawError) <= deadZone && Math.abs(pitchError) <= deadZone) return;
        float yaw = client.player.getYRot() + Mth.clamp(yawError, -maxDegreesPerTick, maxDegreesPerTick);
        float pitch = Mth.clamp(client.player.getXRot() + Mth.clamp(pitchError, -maxDegreesPerTick, maxDegreesPerTick), -90F, 90F);
        client.player.setYRot(yaw);
        client.player.setYHeadRot(yaw);
        client.player.setXRot(pitch);
    }
}
