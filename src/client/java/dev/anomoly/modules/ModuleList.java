package dev.anomoly.modules;

import dev.anomoly.AnomolyClient;
import dev.anomoly.Module;
import java.util.List;
import net.minecraft.client.Minecraft;

public final class ModuleList extends TextHud {
    public ModuleList() {
        super("module_list", "ModuleList", "List the currently enabled Anomoly modules on the HUD.");
    }

    @Override protected List<String> lines(Minecraft client) {
        return AnomolyClient.MODULES.all().stream().filter(m -> m.enabled() && m != this && !(m instanceof TextHud))
            .map(Module::name).toList();
    }
}
