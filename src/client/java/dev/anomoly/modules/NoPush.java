package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;

public final class NoPush extends Module {
    private final BooleanSetting entities = add(new BooleanSetting("entities", "Entity collisions", true));
    private final BooleanSetting fluids = add(new BooleanSetting("fluids", "Water and lava currents", true));

    public NoPush() {
        super("no_push", "No Push", "Ignore entity collision pushes and fluid currents.", Category.MOVEMENT);
    }

    public boolean entities() { return enabled() && entities.get(); }
    public boolean fluids() { return enabled() && fluids.get(); }
}
