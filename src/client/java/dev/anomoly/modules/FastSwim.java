package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public final class FastSwim extends Module {
    private final FloatSetting multiplier = add(new FloatSetting("multiplier", "Speed multiplier", 1.15F, 1.0F, 2.0F));
    private final FloatSetting maxSpeed = add(new FloatSetting("max_speed", "Max speed", 0.5F, 0.1F, 1.5F));

    public FastSwim() {
        super("fast_swim", "FastSwim", "Swim faster in water, scaling motion up to a speed cap (also covers WaterSpeed).", Category.MOVEMENT);
    }

    @Override public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (client.gui.screen() != null || !player.isInWater() || !MoveUtil.moving(player) || player.isPassenger()) return;
        Vec3 velocity = player.getDeltaMovement();
        double speed = MoveUtil.horizontalSpeed(player);
        if (speed < 1.0E-3 || speed >= maxSpeed.get()) return;
        double factor = Math.min(multiplier.get(), maxSpeed.get() / speed);
        player.setDeltaMovement(velocity.x * factor, velocity.y, velocity.z * factor);
    }
}
