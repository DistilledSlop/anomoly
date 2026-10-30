package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;

/** Shared settings and target selection for projectile aiming modules. */
abstract class RangedAim extends Module {
    private final FloatSetting range;
    private final FloatSetting fieldOfView = add(new FloatSetting("fov", "FOV", 140F, 30F, 360F));
    private final FloatSetting turnSpeed = add(new FloatSetting("turn_speed", "Turn speed", 20F, 1F, 90F));
    private final FloatSetting lead = add(new FloatSetting("target_lead", "Target movement lead", 1F, 0F, 1.5F));
    private final BooleanSetting targetMobs = add(new BooleanSetting("target_mobs", "Target mobs", false));
    private final BooleanSetting targetInvisible = add(new BooleanSetting("target_invisible", "Target invisible entities", false));
    private final boolean bowsOnly;

    protected RangedAim(String id, String name, String description, boolean bowsOnly) {
        super(id, name, description, Category.COMBAT);
        this.bowsOnly = bowsOnly;
        range = add(new FloatSetting("range", "Range", bowsOnly ? 48F : 32F, 4F, 64F));
    }

    public boolean active(Minecraft client) {
        return enabled() && ProjectilePhysics.held(client, bowsOnly) != null;
    }

    @Override public void tick(Minecraft client) {
        if (client.gui.screen() != null || client.player.isSpectator()) return;
        ProjectilePhysics.Profile physics = ProjectilePhysics.held(client, bowsOnly);
        if (physics == null) return;
        for (AimTargeting.Solution candidate : AimTargeting.candidates(client, range.get(), fieldOfView.get(), targetMobs.get(), targetInvisible.get())) {
            ProjectileAimSolver.Solution solution = ProjectileAimSolver.solve(client, candidate.entity(), physics, lead.get());
            if (solution == null) continue;
            AimTargeting.turn(client, new AimTargeting.Solution(candidate.entity(), solution.yaw(), solution.pitch()), turnSpeed.get(), 0.1F);
            return;
        }
    }
}
