package dev.anomoly.modules;

import dev.anomoly.AnomolyClient;
import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.item.component.PiercingWeapon;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public final class SpearAssist extends Module {
    private final FloatSetting fieldOfView = add(new FloatSetting("fov", "FOV", 90F, 20F, 180F));
    private final FloatSetting turnSpeed = add(new FloatSetting("turn_speed", "Turn speed", 25F, 5F, 90F));
    private final BooleanSetting targetMobs = add(new BooleanSetting("target_mobs", "Target mobs", false));
    private int lastTarget = -1;
    private int stableTicks;

    public SpearAssist() {
        super("spear_assist", "Spear Assist", "Aim and stab with 26.3 piercing weapons when the ray is clear.", Category.COMBAT);
    }

    @Override public void tick(Minecraft client) {
        if (client.gui.screen() != null || client.gameMode == null || client.player.isSpectator() || client.player.isHandsBusy()
            || AnomolyClient.MODULES.get(AutoRod.class).active(client)) {
            stableTicks = 0;
            return;
        }
        ItemStack item = client.player.getMainHandItem();
        PiercingWeapon weapon = item.get(DataComponents.PIERCING_WEAPON);
        if (weapon == null || client.player.cannotAttackWithItem(item, 0)) { stableTicks = 0; return; }
        AttackRange attackRange = client.player.getAttackRangeWith(item);
        LivingEntity target = findTarget(client, attackRange);
        if (target == null) { stableTicks = 0; lastTarget = -1; return; }
        Vec3 aim = target.getEyePosition().subtract(client.player.getEyePosition());
        float yaw = (float) (Math.toDegrees(Math.atan2(aim.z, aim.x)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(aim.y, Math.hypot(aim.x, aim.z)));
        float yawDifference = Mth.wrapDegrees(yaw - client.player.getYRot());
        float pitchDifference = pitch - client.player.getXRot();
        float nextYaw = client.player.getYRot() + Mth.clamp(yawDifference, -turnSpeed.get(), turnSpeed.get());
        float nextPitch = Mth.clamp(client.player.getXRot() + Mth.clamp(pitchDifference, -turnSpeed.get(), turnSpeed.get()), -90F, 90F);
        client.player.setYRot(nextYaw);
        client.player.setYHeadRot(nextYaw);
        client.player.setXRot(nextPitch);
        if (target.getId() != lastTarget) stableTicks = 0;
        lastTarget = target.getId();
        if (Math.abs(yawDifference) > 2F || Math.abs(pitchDifference) > 2F) { stableTicks = 0; return; }
        stableTicks++;
        if (stableTicks < 2 || client.player.getAttackStrengthScale(0F) < 1F) return;
        if (!(attackRange.getClosesetHit(client.player, 1F, entity -> PiercingWeapon.canHitEntity(client.player, entity)) instanceof EntityHitResult hit)
            || hit.getEntity() != target) return;
        client.gameMode.piercingAttack(item.getAttackAnimation(), weapon);
        stableTicks = 0;
    }

    private LivingEntity findTarget(Minecraft client, AttackRange attackRange) {
        LivingEntity closest = null;
        double best = Double.POSITIVE_INFINITY;
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || entity == client.player || !PiercingWeapon.canHitEntity(client.player, living)) continue;
            if (!(living instanceof Player) && !targetMobs.get()) continue;
            if (living instanceof Player player && AnomolyClient.MODULES.get(AntiBot.class).reject(client, player)) continue;
            if (!client.player.hasLineOfSight(living) || !attackRange.isInRange(client.player, living.getBoundingBox(), 0.0)) continue;
            Vec3 aim = living.getEyePosition().subtract(client.player.getEyePosition());
            float yaw = (float) (Math.toDegrees(Math.atan2(aim.z, aim.x)) - 90.0);
            if (Math.abs(Mth.wrapDegrees(yaw - client.player.getYRot())) > fieldOfView.get() / 2F) continue;
            double distance = client.player.distanceToSqr(living);
            if (distance < best) { best = distance; closest = living; }
        }
        return closest;
    }
}
