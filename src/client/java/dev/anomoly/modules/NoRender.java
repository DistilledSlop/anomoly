package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;

public final class NoRender extends Module {
    private final BooleanSetting particles = add(new BooleanSetting("particles", "Particles", true));

    public NoRender() {
        super("no_render", "No Render", "Skip rendering selected effects, starting with world particles (also covers Particles).", Category.RENDER);
    }

    public boolean blockParticles() { return enabled() && particles.get(); }
}
