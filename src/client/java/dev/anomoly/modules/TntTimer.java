package dev.anomoly.modules;

import dev.anomoly.Module;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;

public final class TntTimer extends Module {
    public TntTimer() {
        super("tnt_timer", "TNTTimer", "Show the remaining fuse time above primed TNT.", Category.RENDER);
    }

    @Override public void tick(Minecraft client) {
        WorldGizmos.draw(client, () -> {
            for (Entity entity : client.level.entitiesForRendering()) {
                if (entity instanceof PrimedTnt tnt) {
                    Gizmos.billboardTextOverMob(tnt, 1, String.format(Locale.ROOT, "%.1fs", tnt.getFuse() / 20.0), 0xFFFF5555, 0.8F);
                }
            }
        });
    }
}
