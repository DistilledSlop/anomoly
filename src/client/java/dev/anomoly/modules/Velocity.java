package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.world.phys.Vec3;

public final class Velocity extends Module {
    private final FloatSetting horizontal = add(new FloatSetting("horizontal", "Horizontal percent", 0, 0, 100));
    private final FloatSetting vertical = add(new FloatSetting("vertical", "Vertical percent", 0, 0, 100));

    public Velocity() {
        super("velocity", "Velocity", "Scale incoming player knockback and explosion impulses.", Category.MOVEMENT);
    }

    public Vec3 scale(Vec3 motion) {
        if (!enabled()) return motion;
        double xz = horizontal.get() / 100.0;
        double y = vertical.get() / 100.0;
        return new Vec3(motion.x * xz, motion.y * y, motion.z * xz);
    }
}
