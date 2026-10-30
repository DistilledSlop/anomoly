package dev.anomoly.modules;

import dev.anomoly.AnomolyClient;
import dev.anomoly.Module;
import net.minecraft.world.entity.Entity;

/** Fan-out of the local player's attacks to modules that react to a landed hit. */
public final class HitEvents {
    public interface Listener {
        void onHit(Entity target);
    }

    private HitEvents() { }

    public static void attacked(Entity target) {
        for (Module module : AnomolyClient.MODULES.all()) {
            if (module.enabled() && module instanceof Listener listener) listener.onHit(target);
        }
    }
}
