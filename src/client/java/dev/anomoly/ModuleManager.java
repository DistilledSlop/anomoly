package dev.anomoly;

import dev.anomoly.modules.AutoJump;
import dev.anomoly.modules.AutoEat;
import dev.anomoly.modules.AutoRespawn;
import dev.anomoly.modules.AutoSneak;
import dev.anomoly.modules.AutoSprint;
import dev.anomoly.modules.AutoWalk;
import dev.anomoly.modules.FastBreak;
import dev.anomoly.modules.FastPlace;
import dev.anomoly.modules.KillAura;
import dev.anomoly.modules.MaxBrightness;
import dev.anomoly.modules.NoHurtCam;
import dev.anomoly.modules.PlayerRadar;
import dev.anomoly.modules.Reach;
import dev.anomoly.modules.SpearAssist;
import dev.anomoly.modules.TriggerBot;
import java.util.List;
import net.minecraft.client.Minecraft;

public final class ModuleManager {
    private final List<Module> modules = List.of(
        new TriggerBot(), new KillAura(), new SpearAssist(), new Reach(), new AutoSprint(), new AutoWalk(), new AutoJump(),
        new AutoSneak(), new AutoRespawn(), new AutoEat(), new FastPlace(), new FastBreak(),
        new MaxBrightness(), new NoHurtCam(), new PlayerRadar()
    );

    public List<Module> all() { return modules; }

    public <T extends Module> T get(Class<T> type) {
        for (Module module : modules) if (type.isInstance(module)) return type.cast(module);
        throw new IllegalArgumentException("Unregistered module " + type.getName());
    }

    public void tick(Minecraft client) {
        if (client.player == null || client.level == null) return;
        for (Module module : modules) {
            if (module.enabled()) module.tick(client);
        }
    }

    public void disableAll() {
        for (Module module : modules) module.setEnabled(false);
    }
}
