package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;

public final class AirJump extends Module {
    private final FloatSetting extraJumps = add(new FloatSetting("extra_jumps", "Extra jumps", 1, 1, 5));
    private int remaining;
    private boolean wasHeld;

    public AirJump() {
        super("air_jump", "Air Jump", "Jump again when the jump key is pressed in midair.", Category.MOVEMENT);
    }

    @Override public void tick(Minecraft client) {
        if (client.gui.screen() != null || client.player.isPassenger() || client.player.getAbilities().flying
            || client.player.isFallFlying() || client.player.isInWater() || client.player.isInLava()) {
            wasHeld = client.options.keyJump.isDown();
            return;
        }
        if (client.player.onGround()) remaining = (int)extraJumps.get();
        boolean held = client.options.keyJump.isDown();
        if (held && !wasHeld && !client.player.onGround() && remaining > 0) {
            client.player.jumpFromGround();
            remaining--;
        }
        wasHeld = held;
    }

    @Override protected void onDisable(Minecraft client) { remaining = 0; wasHeld = false; }
}
