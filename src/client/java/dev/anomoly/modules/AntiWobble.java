package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;

public final class AntiWobble extends Module {
    private final BooleanSetting portal = add(new BooleanSetting("portal", "Portal effect", true));
    private final BooleanSetting nausea = add(new BooleanSetting("nausea", "Nausea effect", true));

    public AntiWobble() {
        super("anti_wobble", "Anti Wobble", "Remove portal and nausea screen distortion without removing the effects.", Category.RENDER);
    }

    public boolean portal() { return enabled() && portal.get(); }
    public boolean nausea() { return enabled() && nausea.get(); }
}
