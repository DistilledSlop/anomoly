package dev.anomoly.modules;

import dev.anomoly.AnomolyClient;
import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import net.minecraft.client.Minecraft;

public final class AutoDisable extends Module {
    private final BooleanSetting onDeath = add(new BooleanSetting("on_death", "Disable all modules on death", true));
    private final BooleanSetting onCorrection = add(new BooleanSetting("on_dimension_change", "Disable all modules on dimension change", false));
    private Object lastDimension;
    private boolean wasDead;

    public AutoDisable() {
        super("auto_disable", "AutoDisable", "Turn every other module off when you die or change dimension.", Category.WORLD);
    }

    @Override protected void onEnable(Minecraft client) { lastDimension = null; wasDead = false; }

    @Override public void tick(Minecraft client) {
        boolean dead = client.player.isDeadOrDying();
        Object dimension = client.level.dimension();
        boolean trigger = onDeath.get() && dead && !wasDead || onCorrection.get() && lastDimension != null && !dimension.equals(lastDimension);
        wasDead = dead;
        lastDimension = dimension;
        if (!trigger) return;
        AnomolyClient.batchConfigChanges(() -> {
            for (Module module : AnomolyClient.MODULES.all()) if (module != this) module.setEnabled(false);
        });
    }
}
