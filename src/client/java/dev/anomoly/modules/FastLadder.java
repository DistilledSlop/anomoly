package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public final class FastLadder extends Module {
    private final FloatSetting speed = add(new FloatSetting("speed", "Climb speed", 0.20F, 0.12F, 0.35F));

    public FastLadder() {
        super("fast_ladder", "Fast Ladder", "Climb ladders and vines faster while holding forward.", Category.MOVEMENT);
    }

    @Override public void tick(Minecraft client) {
        var player = client.player;
        if (client.gui.screen() != null || !player.onClimbable() || !client.options.keyUp.isDown()
            || client.options.keyShift.isDown()) return;
        Vec3 velocity = player.getDeltaMovement();
        if (velocity.y < speed.get()) player.setDeltaMovement(velocity.x, speed.get(), velocity.z);
    }
}
