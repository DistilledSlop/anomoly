package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;

public final class NoSlow extends Module {
    private final BooleanSetting itemUse = add(new BooleanSetting("item_use", "Item use", true));
    private final BooleanSetting blockFactor = add(new BooleanSetting("block_factor", "Block speed factor", true));

    public NoSlow() {
        super("no_slow", "No Slow", "Remove item-use slowdown and block speed penalties.", Category.MOVEMENT);
    }

    public boolean itemUse() { return enabled() && itemUse.get(); }
    public boolean blockFactor() { return enabled() && blockFactor.get(); }
}
