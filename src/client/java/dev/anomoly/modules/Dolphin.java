package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public final class Dolphin extends Module {
    private final FloatSetting lift = add(new FloatSetting("lift", "Surface lift", 0.1F, 0.02F, 0.5F));

    public Dolphin() {
        super("dolphin", "Dolphin", "Rise to the surface and bob there while in water unless sneaking.", Category.MOVEMENT);
    }

    @Override public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (client.gui.screen() != null || !player.isInWater() || player.isShiftKeyDown() || player.isPassenger()) return;
        Vec3 velocity = player.getDeltaMovement();
        if (velocity.y < lift.get()) player.setDeltaMovement(velocity.x, lift.get(), velocity.z);
    }
}
