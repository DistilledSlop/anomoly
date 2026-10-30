package dev.anomoly.modules;

import dev.anomoly.Module;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.player.Player;

public final class HealthTags extends Module {
    public HealthTags() {
        super("health_tags", "HealthTags", "Show other players' health and absorption above their heads.", Category.RENDER);
    }

    @Override public void tick(Minecraft client) {
        WorldGizmos.draw(client, () -> {
            for (Player other : client.level.players()) {
                if (other == client.player) continue;
                float hp = other.getHealth() + other.getAbsorptionAmount();
                int color = hp > 14 ? 0xFF55FF55 : hp > 7 ? 0xFFFFFF55 : 0xFFFF5555;
                Gizmos.billboardTextOverMob(other, 1, String.format(Locale.ROOT, "%.1f HP", hp), color, 0.7F);
            }
        });
    }
}
