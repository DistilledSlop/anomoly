package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;

public final class Reach extends Module {
    private final FloatSetting entityRange = add(new FloatSetting("entity_range", "Entity range", 3.0F, 3.0F, 6.0F));
    private final FloatSetting blockRange = add(new FloatSetting("block_range", "Block range", 4.5F, 4.5F, 6.0F));

    public Reach() {
        super("reach", "Reach", "Extend local target selection; server checks still apply.", Category.COMBAT);
    }

    public float entityRange() { return entityRange.get(); }
    public float blockRange() { return blockRange.get(); }
}
