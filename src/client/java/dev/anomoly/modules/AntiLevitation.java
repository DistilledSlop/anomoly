package dev.anomoly.modules;

import dev.anomoly.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

public final class AntiLevitation extends Module {
    public AntiLevitation() {
        super("anti_levitation", "AntiLevitation", "Cancel levitation lift and shulker-bullet floating by applying normal gravity.", Category.MOVEMENT);
    }

    @Override public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (!player.hasEffect(MobEffects.LEVITATION) || player.onGround()) return;
        Vec3 velocity = player.getDeltaMovement();
        if (velocity.y > -0.08) player.setDeltaMovement(velocity.x, -0.08, velocity.z);
    }
}
