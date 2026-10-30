package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public final class FastFall extends Module {
    private final FloatSetting extraGravity = add(new FloatSetting("extra_gravity", "Extra gravity", 0.08F, 0.02F, 0.30F));

    public FastFall() {
        super("fast_fall", "Fast Fall", "Increase downward speed while falling freely.", Category.MOVEMENT);
    }

    @Override public void tick(Minecraft client) {
        var player = client.player;
        if (client.gui.screen() != null || player.onGround() || player.isPassenger() || player.getAbilities().flying
            || player.isFallFlying() || player.isInWater() || player.isInLava() || player.onClimbable()
            || player.fallDistance < 0.5) return;
        Vec3 velocity = player.getDeltaMovement();
        if (velocity.y < -0.05) player.setDeltaMovement(velocity.x, Math.max(-3.9, velocity.y - extraGravity.get()), velocity.z);
    }
}
