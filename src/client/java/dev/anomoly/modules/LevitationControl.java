package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

public final class LevitationControl extends Module {
    private final FloatSetting speed = add(new FloatSetting("speed", "Vertical speed", 0.2F, 0.05F, 1.0F));

    public LevitationControl() {
        super("levitation_control", "Levitation Control", "While levitating, jump rises, sneak sinks, and neither hovers in place (also covers LevitCtrl).", Category.MOVEMENT);
    }

    @Override public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (!player.hasEffect(MobEffects.LEVITATION) || client.gui.screen() != null) return;
        double y = client.options.keyJump.isDown() ? speed.get() : client.options.keyShift.isDown() ? -speed.get() : 0;
        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(velocity.x, y, velocity.z);
    }
}
