package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import net.minecraft.client.Minecraft;

public final class RotationLock extends Module {
    private final BooleanSetting lockYaw = add(new BooleanSetting("lock_yaw", "Lock yaw", true));
    private final BooleanSetting lockPitch = add(new BooleanSetting("lock_pitch", "Lock pitch", true));
    private float yaw, pitch;

    public RotationLock() {
        super("rotation_lock", "RotationLock", "Hold the view at the angle it had when enabled (also covers ViewLock).", Category.PLAYER);
    }

    @Override protected void onEnable(Minecraft client) {
        if (client.player == null) return;
        yaw = client.player.getYRot();
        pitch = client.player.getXRot();
    }

    @Override public void tick(Minecraft client) {
        if (client.gui.screen() != null) return;
        if (lockYaw.get()) client.player.setYRot(yaw);
        if (lockPitch.get()) client.player.setXRot(pitch);
    }
}
