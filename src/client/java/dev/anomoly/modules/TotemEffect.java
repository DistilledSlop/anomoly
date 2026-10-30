package dev.anomoly.modules;

import dev.anomoly.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;

public final class TotemEffect extends Module {
    public TotemEffect() {
        super("totem_effect", "TotemEffect", "Burst extra totem particles around any player who pops a totem.", Category.RENDER);
    }

    public void popped(Minecraft client, Player player) {
        if (!enabled()) return;
        for (int i = 0; i < 40; i++) {
            client.level.addParticle(ParticleTypes.TOTEM_OF_UNDYING, player.getRandomX(1.0), player.getRandomY(), player.getRandomZ(1.0), 0, 0.2, 0);
        }
    }
}
