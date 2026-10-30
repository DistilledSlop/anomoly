package dev.anomoly.modules;

import dev.anomoly.Module;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.player.Player;

public final class Nametags extends Module {
    public Nametags() {
        super("nametags", "Nametags", "Label players with name, health and distance in an always-visible tag.", Category.RENDER);
    }

    @Override public void tick(Minecraft client) {
        WorldGizmos.draw(client, () -> {
            for (Player other : client.level.players()) {
                if (other == client.player) continue;
                String tag = String.format(Locale.ROOT, "%s  %.1f HP  %.0fm", other.getName().getString(),
                    other.getHealth() + other.getAbsorptionAmount(), client.player.distanceTo(other));
                Gizmos.billboardTextOverMob(other, 2, tag, 0xFFFFFFFF, 0.8F).setAlwaysOnTop();
            }
        });
    }
}
