package dev.anomoly.modules;

import dev.anomoly.Module;
import net.minecraft.client.Minecraft;

public final class AutoRespawn extends Module {
    private int cooldown;
    public AutoRespawn() {
        super("auto_respawn", "Auto Respawn", "Request a respawn after death.", Category.PLAYER);
    }

    @Override public void tick(Minecraft client) {
        if (cooldown > 0) cooldown--;
        if (client.player.isDeadOrDying() && cooldown == 0) {
            client.player.respawn();
            cooldown = 20;
        }
    }
}
