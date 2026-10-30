package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;

public final class Hitbox extends Module {
    private final FloatSetting expand = add(new FloatSetting("expand", "Expansion (blocks)", 0.2F, 0.0F, 1.0F));

    public Hitbox() {
        super("hitbox", "Hitbox", "Enlarge other entities' targeting boxes so attacks and picks land more easily.", Category.COMBAT);
    }

    public float expansion() { return enabled() ? expand.get() : 0F; }
}
