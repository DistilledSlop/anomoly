package dev.anomoly.modules;

import dev.anomoly.AnomolyClient;
import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.protocol.game.ServerboundPunchPacket;
import net.minecraft.world.phys.Vec3;

public final class KillAura extends Module {
    private final FloatSetting range = add(new FloatSetting("range", "Range", 3.0F, 2.0F, 6.0F));
    private final FloatSetting fieldOfView = add(new FloatSetting("fov", "FOV", 100F, 20F, 360F));
    private final FloatSetting turnSpeed = add(new FloatSetting("turn_speed", "Turn speed", 30F, 5F, 180F));
    private final BooleanSetting targetMobs = add(new BooleanSetting("target_mobs", "Target mobs", false));
    private final BooleanSetting throughWalls = add(new BooleanSetting("through_walls", "Through walls", false));

    public KillAura() {
        super("kill_aura", "Kill Aura", "Face and attack the nearest eligible target at full cooldown.", Category.COMBAT);
    }

    @Override public void tick(Minecraft client) {
        if (client.gui.screen() != null || client.gameMode == null || client.player.isSpectator()
            || AnomolyClient.MODULES.get(Criticals.class).pending(client)
            || AnomolyClient.MODULES.get(CrystalAura.class).engaged(client)
            || client.player.isHandsBusy() || client.player.getMainHandItem().has(DataComponents.PIERCING_WEAPON)
            || AnomolyClient.MODULES.get(AutoShoot.class).active(client)
            || AnomolyClient.MODULES.get(AutoBow.class).active(client)
            || AnomolyClient.MODULES.get(AutoRod.class).active(client)
            || AnomolyClient.MODULES.get(AutoPearl.class).active(client)
            || SoupUse.busy(client)
            || AnomolyClient.MODULES.get(BowAimbot.class).active(client)
            || AnomolyClient.MODULES.get(ProjectileAimbot.class).active(client)
            || client.player.cannotAttackWithItem(client.player.getMainHandItem(), 0)) return;
        LivingEntity target = findTarget(client);
        if (target == null) return;
        Vec3 eyes = client.player.getEyePosition();
        Vec3 aim = target.getEyePosition().subtract(eyes);
        float yaw = (float) (Math.toDegrees(Math.atan2(aim.z, aim.x)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(aim.y, Math.hypot(aim.x, aim.z)));
        float yawDifference = Mth.wrapDegrees(yaw - client.player.getYRot());
        float pitchDifference = pitch - client.player.getXRot();
        client.player.setYRot(client.player.getYRot() + Mth.clamp(yawDifference, -turnSpeed.get(), turnSpeed.get()));
        client.player.setXRot(Mth.clamp(client.player.getXRot() + Mth.clamp(pitchDifference, -turnSpeed.get(), turnSpeed.get()), -90F, 90F));
        if (Math.abs(yawDifference) <= 3F && Math.abs(pitchDifference) <= 3F
            && client.player.getAttackStrengthScale(0F) >= 1F) {
            client.gameMode.attack(client.player, target);
            // Criticals owns the attack until descent. An early punch packet would
            // reset the server-side cooldown before that delayed hit lands.
            if (AnomolyClient.MODULES.get(Criticals.class).pending(client)) return;
            client.player.swing(InteractionHand.MAIN_HAND, client.player.getMainHandItem().getAttackAnimation(), false);
            client.player.connection.send(ServerboundPunchPacket.INSTANCE);
        }
    }

    private LivingEntity findTarget(Minecraft client) {
        LivingEntity closest = null;
        double best = range.get() * range.get();
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || entity == client.player || !living.isAlive()) continue;
            if (!(living instanceof Player) && !targetMobs.get()) continue;
            if (living instanceof Player player && (!client.player.canHarmPlayer(player)
                || AnomolyClient.MODULES.get(AntiBot.class).reject(client, player))) continue;
            if (!throughWalls.get() && !client.player.hasLineOfSight(living)) continue;
            double distance = client.player.distanceToSqr(living);
            if (distance > best) continue;
            if (!client.player.getAttackRangeWith(client.player.getMainHandItem()).isInRange(client.player, living.getBoundingBox(), 3.0)) continue;
            Vec3 aim = living.getEyePosition().subtract(client.player.getEyePosition());
            float yaw = (float) (Math.toDegrees(Math.atan2(aim.z, aim.x)) - 90.0);
            if (Math.abs(Mth.wrapDegrees(yaw - client.player.getYRot())) > fieldOfView.get() / 2F) continue;
            closest = living;
            best = distance;
        }
        return closest;
    }
}
