package dev.anomoly.modules;

import dev.anomoly.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public final class Fish extends Module {
    public Fish() {
        super("fish", "Fish", "Remove underwater gravity so you hold your depth unless jumping or sneaking.", Category.MOVEMENT);
    }

    @Override public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (!player.isInWater() || client.options.keyJump.isDown() || client.options.keyShift.isDown() || player.isPassenger()) return;
        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(velocity.x, 0, velocity.z);
    }
}
