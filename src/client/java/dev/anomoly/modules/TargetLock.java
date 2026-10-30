package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.StringSetting;
import net.minecraft.world.entity.player.Player;

public final class TargetLock extends Module {
    private final StringSetting target = add(new StringSetting("target", "Only attack this player", ""));

    public TargetLock() {
        super("target_lock", "TargetLock", "Limit Kill Aura, Multi Aura, Click Aura and similar modules to a single named player.", Category.MISC);
    }

    public boolean excludes(Player other) {
        return enabled() && !target.get().isBlank() && !target.get().trim().equalsIgnoreCase(other.getName().getString());
    }
}
