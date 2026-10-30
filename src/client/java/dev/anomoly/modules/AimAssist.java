package dev.anomoly.modules;

import dev.anomoly.AnomolyClient;
import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;

public final class AimAssist extends Module {
    private final FloatSetting range = add(new FloatSetting("range", "Range", 4.5F, 2F, 8F));
    private final FloatSetting fieldOfView = add(new FloatSetting("fov", "FOV", 50F, 10F, 180F));
    private final FloatSetting turnSpeed = add(new FloatSetting("turn_speed", "Turn speed", 2.5F, 0.5F, 12F));
    private final BooleanSetting targetMobs = add(new BooleanSetting("target_mobs", "Target mobs", false));
    private final BooleanSetting targetInvisible = add(new BooleanSetting("target_invisible", "Target invisible entities", false));

    public AimAssist() {
        super("aim_assist", "Aim Assist", "Gently correct your aim toward a visible target while attack is held; never attacks for you.", Category.COMBAT);
    }

    @Override public void tick(Minecraft client) {
        if (client.gui.screen() != null || client.player.isSpectator() || !client.options.keyAttack.isDown()
            || AnomolyClient.MODULES.get(KillAura.class).enabled() || AnomolyClient.MODULES.get(Aimbot.class).enabled()
            || AnomolyClient.MODULES.get(AutoShoot.class).active(client)
            || AnomolyClient.MODULES.get(BowAimbot.class).active(client)
            || AnomolyClient.MODULES.get(ProjectileAimbot.class).active(client)) return;
        AimTargeting.turn(client, AimTargeting.find(client, range.get(), fieldOfView.get(), targetMobs.get(), targetInvisible.get()),
            turnSpeed.get(), 0.25F);
    }
}
