package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public final class Jetpack extends Module {
    private final FloatSetting thrust = add(new FloatSetting("thrust", "Thrust", 0.25F, 0.05F, 1.0F));

    public Jetpack() {
        super("jetpack", "Jetpack", "Hold jump in midair to keep accelerating upward.", Category.MOVEMENT);
    }

    @Override public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (client.gui.screen() != null || !client.options.keyJump.isDown() || player.onGround()
            || !MoveUtil.freeMovement(player)) return;
        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(velocity.x, Math.min(thrust.get(), velocity.y + thrust.get() * 0.5), velocity.z);
    }
}
