package dev.anomoly.modules;

import dev.anomoly.setting.BooleanSetting;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;

public final class MobEsp extends EntityEsp {
    private final BooleanSetting hostile = add(new BooleanSetting("hostile", "Hostile mobs", true));
    private final BooleanSetting passive = add(new BooleanSetting("passive", "Passive mobs", true));

    public MobEsp() {
        super("mob_esp", "MobEsp", "Outline hostile and passive mobs through walls.");
    }

    @Override protected boolean matches(Entity entity) {
        if (!(entity instanceof Mob)) return false;
        return entity instanceof Enemy ? hostile.get() : passive.get();
    }
}
