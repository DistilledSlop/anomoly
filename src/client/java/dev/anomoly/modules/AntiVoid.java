package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public final class AntiVoid extends Module {
    private final FloatSetting margin = add(new FloatSetting("margin", "Height above void", 6.0F, 1.0F, 32.0F));
    private final FloatSetting recovery = add(new FloatSetting("recovery", "Recovery lift", 0.6F, 0.1F, 2.0F));

    public AntiVoid() {
        super("anti_void", "AntiVoid", "Bounce back upward before falling out of the world.", Category.MOVEMENT);
    }

    @Override public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        double floor = player.level().getMinY();
        if (player.getY() > floor + margin.get() || player.onGround() || player.getDeltaMovement().y >= 0
            || player.getAbilities().flying || player.isFallFlying()) return;
        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(velocity.x, recovery.get(), velocity.z);
        player.fallDistance = 0;
    }
}
