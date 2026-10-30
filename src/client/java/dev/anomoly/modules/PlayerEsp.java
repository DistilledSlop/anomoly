package dev.anomoly.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class PlayerEsp extends EntityEsp {
    public PlayerEsp() {
        super("player_esp", "PlayerEsp", "Outline other players through walls.");
    }

    @Override protected boolean matches(Entity entity) {
        return entity instanceof Player && entity != Minecraft.getInstance().player;
    }
}
