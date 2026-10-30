package dev.anomoly.modules;

import dev.anomoly.AnomolyClient;
import dev.anomoly.Module;
import net.minecraft.world.entity.Entity;

/** A module that outlines matching entities through walls with the vanilla glow effect. */
public abstract class EntityEsp extends Module {
    protected EntityEsp(String id, String name, String description) {
        super(id, name, description, Category.RENDER);
    }

    protected abstract boolean matches(Entity entity);

    public static boolean glows(Entity entity) {
        for (Module module : AnomolyClient.MODULES.all()) {
            if (module instanceof EntityEsp esp && module.enabled() && esp.matches(entity)) return true;
        }
        return false;
    }
}
