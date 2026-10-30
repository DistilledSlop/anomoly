package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public final class Spider extends Module {
    private final FloatSetting speed = add(new FloatSetting("speed", "Climb speed", 0.2F, 0.05F, 1.0F));

    public Spider() {
        super("spider", "Spider", "Climb walls by pushing into them.", Category.MOVEMENT);
    }

    @Override public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (client.gui.screen() != null || !player.horizontalCollision || !MoveUtil.moving(player)
            || player.isShiftKeyDown() || !MoveUtil.freeMovement(player)) return;
        Vec3 velocity = player.getDeltaMovement();
        if (velocity.y < speed.get()) player.setDeltaMovement(velocity.x, speed.get(), velocity.z);
        player.fallDistance = 0;
    }
}
