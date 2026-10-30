package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;

public final class AntiBlind extends Module {
    private final BooleanSetting blindness = add(new BooleanSetting("blindness", "Blindness", true));
    private final BooleanSetting darkness = add(new BooleanSetting("darkness", "Darkness", true));
    private final BooleanSetting portal = add(new BooleanSetting("portal", "Portal distortion", true));
    private final BooleanSetting nausea = add(new BooleanSetting("nausea", "Nausea distortion", true));
    private final BooleanSetting pumpkin = add(new BooleanSetting("pumpkin", "Pumpkin overlay", true));
    private final BooleanSetting water = add(new BooleanSetting("water", "Water overlay", true));
    private final BooleanSetting fire = add(new BooleanSetting("fire", "Fire overlay", true));
    private final BooleanSetting block = add(new BooleanSetting("block", "Block overlay", true));
    private final BooleanSetting powderSnow = add(new BooleanSetting("powder_snow", "Powder snow overlay", true));
    private final BooleanSetting liquidFog = add(new BooleanSetting("liquid_fog", "Liquid and snow fog", true));

    public AntiBlind() {
        super("anti_blind", "Anti Blind", "Hide obstructive visual effects while retaining their server-side status.", Category.RENDER);
    }

    public boolean blindness() { return enabled() && blindness.get(); }
    public boolean darkness() { return enabled() && darkness.get(); }
    public boolean portal() { return enabled() && portal.get(); }
    public boolean nausea() { return enabled() && nausea.get(); }
    public boolean pumpkin() { return enabled() && pumpkin.get(); }
    public boolean water() { return enabled() && water.get(); }
    public boolean fire() { return enabled() && fire.get(); }
    public boolean block() { return enabled() && block.get(); }
    public boolean powderSnow() { return enabled() && powderSnow.get(); }
    public boolean liquidFog() { return enabled() && liquidFog.get(); }
}
