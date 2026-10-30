package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import net.minecraft.client.Minecraft;

public final class NoEntityTrace extends Module {
    private final BooleanSetting onlyMining = add(new BooleanSetting("only_while_mining", "Only while holding attack", false));

    public NoEntityTrace() {
        super("no_entity_trace", "NoEntityTrace", "Ignore entity hitboxes when picking a target so you can mine and interact through them (also covers No Mining Trace).", Category.PLAYER);
    }

    public boolean ignoring() {
        return enabled() && (!onlyMining.get() || Minecraft.getInstance().options.keyAttack.isDown());
    }
}
