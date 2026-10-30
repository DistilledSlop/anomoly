package dev.anomoly.modules;

import dev.anomoly.AnomolyClient;
import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;

public final class Aimbot extends Module {
    private final FloatSetting range = add(new FloatSetting("range", "Range", 6F, 2F, 12F));
    private final FloatSetting fieldOfView = add(new FloatSetting("fov", "FOV", 120F, 20F, 360F));
    private final FloatSetting turnSpeed = add(new FloatSetting("turn_speed", "Turn speed", 20F, 1F, 90F));
    private final BooleanSetting targetMobs = add(new BooleanSetting("target_mobs", "Target mobs", false));
    private final BooleanSetting targetInvisible = add(new BooleanSetting("target_invisible", "Target invisible entities", false));

    public Aimbot() {
        super("aimbot", "Aimbot", "Track the nearest visible target to your crosshair without attacking.", Category.COMBAT);
    }

    @Override public void tick(Minecraft client) {
        if (client.gui.screen() != null || client.player.isSpectator()
            || AnomolyClient.MODULES.get(KillAura.class).enabled()
            || AnomolyClient.MODULES.get(AutoShoot.class).active(client)
            || AnomolyClient.MODULES.get(BowAimbot.class).active(client)
            || AnomolyClient.MODULES.get(ProjectileAimbot.class).active(client)) return;
        AimTargeting.turn(client, AimTargeting.find(client, range.get(), fieldOfView.get(), targetMobs.get(), targetInvisible.get()),
            turnSpeed.get(), 0F);
    }
}
