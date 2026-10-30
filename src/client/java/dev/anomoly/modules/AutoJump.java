package dev.anomoly.modules;

import dev.anomoly.Module;
import net.minecraft.client.Minecraft;

public final class AutoJump extends Module {
    public AutoJump() {
        super("auto_jump", "Auto Jump", "Jump when walking on the ground.", Category.MOVEMENT);
    }

    @Override public void tick(Minecraft client) {
        if (client.gui.screen() == null && client.player.onGround()
            && client.player.input.hasForwardImpulse() && !client.player.isShiftKeyDown()) {
            client.player.jumpFromGround();
        }
    }
}
