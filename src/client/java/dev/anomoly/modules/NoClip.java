package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public final class NoClip extends Module {
    private final FloatSetting speed = add(new FloatSetting("speed", "Speed", 0.4F, 0.05F, 2.0F));

    public NoClip() {
        super("no_clip", "NoClip", "Move through solid blocks as if they were not there; jump rises and sneak sinks.", Category.MOVEMENT);
    }

    @Override public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (client.gui.screen() != null) return;
        player.noPhysics = true;
        player.fallDistance = 0;
        double y = (client.options.keyJump.isDown() ? 1 : 0) - (client.options.keyShift.isDown() ? 1 : 0);
        Vec3 horizontal = MoveUtil.direction(player, speed.get());
        player.setDeltaMovement(horizontal.x, y * speed.get(), horizontal.z);
    }

    @Override protected void onDisable(Minecraft client) {
        if (client.player != null) client.player.noPhysics = false;
    }
}
