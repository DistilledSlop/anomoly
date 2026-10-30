package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public final class Glide extends Module {
    private final FloatSetting fallSpeed = add(new FloatSetting("fall_speed", "Fall speed", 0.03F, 0.0F, 0.2F));
    private final FloatSetting forward = add(new FloatSetting("forward", "Forward boost", 0.0F, 0.0F, 0.5F));

    public Glide() {
        super("glide", "Glide", "Fall slowly instead of dropping at full speed.", Category.MOVEMENT);
    }

    @Override public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player.onGround() || !MoveUtil.freeMovement(player) || player.isInWater() || player.isInLava()
            || player.onClimbable()) return;
        Vec3 velocity = player.getDeltaMovement();
        if (velocity.y >= -fallSpeed.get()) return;
        Vec3 boost = forward.get() > 0 && MoveUtil.moving(player) ? MoveUtil.direction(player, forward.get()) : Vec3.ZERO;
        player.setDeltaMovement(velocity.x + boost.x * 0.1, -fallSpeed.get(), velocity.z + boost.z * 0.1);
        player.fallDistance = 0;
    }
}
