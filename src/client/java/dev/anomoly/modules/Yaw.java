package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class Yaw extends Module {
    private final IntSetting step = add(new IntSetting("step", "Snap angle", 45, 5, 90));
    private final BooleanSetting lockPitch = add(new BooleanSetting("lock_pitch", "Lock pitch to level", false));

    public Yaw() {
        super("yaw", "Yaw", "Snap the view yaw to the nearest multiple of a fixed angle for straight-line travel.", Category.MOVEMENT);
    }

    @Override public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (client.gui.screen() != null) return;
        float angle = step.get();
        player.setYRot(Math.round(player.getYRot() / angle) * angle);
        if (lockPitch.get()) player.setXRot(0);
    }
}
