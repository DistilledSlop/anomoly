package dev.anomoly.modules;

import dev.anomoly.AnomolyClient;
import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.FloatSetting;
import java.util.Comparator;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.phys.Vec3;

/** Turns toward nearby endermen or turns away before the player stares at one. */
public final class EndermanLook extends Module {
    private final BooleanSetting lookAt = add(new BooleanSetting("look_at", "Look at Endermen (off: avoid gaze)", false));
    private final FloatSetting range = add(new FloatSetting("range", "Enderman range", 16F, 2F, 64F));
    private final FloatSetting turnSpeed = add(new FloatSetting("turn_speed", "Look turn speed", 20F, 1F, 90F));

    public EndermanLook() {
        super("enderman_look", "Enderman Look", "Look at nearby Endermen or turn away from their gaze.", Category.WORLD);
    }

    @Override public void tick(Minecraft client) {
        if (client.gui.screen() != null || !client.player.isAlive() || client.player.isSpectator()
            || AnomolyClient.MODULES.get(KillAura.class).enabled()
            || AnomolyClient.MODULES.get(Aimbot.class).enabled()
            || AnomolyClient.MODULES.get(AimAssist.class).enabled()
            || AnomolyClient.MODULES.get(AutoShoot.class).active(client)
            || AnomolyClient.MODULES.get(AutoBow.class).active(client)
            || AnomolyClient.MODULES.get(AutoRod.class).active(client)
            || AnomolyClient.MODULES.get(AutoPearl.class).active(client)
            || AnomolyClient.MODULES.get(BowAimbot.class).active(client)
            || AnomolyClient.MODULES.get(ProjectileAimbot.class).active(client)) return;
        var endermen = client.level.getEntitiesOfClass(Enderman.class,
            client.player.getBoundingBox().inflate(range.get()),
            enderman -> enderman.isAlive() && client.player.distanceToSqr(enderman) <= range.get() * range.get()
                && client.player.hasLineOfSight(enderman));
        if (lookAt.get()) {
            Enderman target = endermen.stream().min(Comparator.comparingDouble(client.player::distanceToSqr)).orElse(null);
            if (target == null) return;
            Vec3 direction = target.getEyePosition().subtract(client.player.getEyePosition());
            float yaw = (float) (Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0);
            float pitch = (float) -Math.toDegrees(Math.atan2(direction.y, Math.hypot(direction.x, direction.z)));
            AimTargeting.turn(client, new AimTargeting.Solution(target, yaw, pitch), turnSpeed.get(), 0F);
            return;
        }
        for (Enderman enderman : endermen) {
            if (!enderman.isLookingAtMe(client.player, 0.05, true, false, enderman.getEyeY())) continue;
            float yaw = Mth.wrapDegrees(client.player.getYRot() + 90F);
            client.player.setYRot(yaw);
            client.player.setYHeadRot(yaw);
            return;
        }
    }
}
