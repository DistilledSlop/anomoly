package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.ChoiceSetting;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public final class Speed extends Module {
    private final ChoiceSetting mode = add(new ChoiceSetting("mode", "Mode", "Strafe", "Strafe", "Multiply"));
    private final FloatSetting speed = add(new FloatSetting("speed", "Speed", 0.35F, 0.1F, 2.0F));
    private final FloatSetting multiplier = add(new FloatSetting("multiplier", "Multiplier", 1.15F, 1.0F, 3.0F));

    public Speed() {
        super("speed", "Speed", "Move faster than normal walking or sprinting speed.", Category.MOVEMENT);
    }

    @Override public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (client.gui.screen() != null || !MoveUtil.moving(player) || !MoveUtil.freeMovement(player)
            || player.isInWater() || player.isInLava() || player.onClimbable()) return;
        if (mode.get().equals("Strafe")) {
            MoveUtil.setHorizontal(player, MoveUtil.direction(player, speed.get()));
        } else {
            Vec3 velocity = player.getDeltaMovement();
            double limit = speed.get() * 2;
            double factor = Math.min(multiplier.get(), limit / Math.max(MoveUtil.horizontalSpeed(player), 1.0E-4));
            player.setDeltaMovement(velocity.x * factor, velocity.y, velocity.z * factor);
        }
    }
}
