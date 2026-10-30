package dev.anomoly;

import dev.anomoly.modules.AutoJump;
import dev.anomoly.modules.AntiReducedDebugInfo;
import dev.anomoly.modules.AutoArmor;
import dev.anomoly.modules.AutoF5;
import dev.anomoly.modules.AntiAFK;
import dev.anomoly.modules.AutoEat;
import dev.anomoly.modules.AutoFish;
import dev.anomoly.modules.AutoRespawn;
import dev.anomoly.modules.AutoSneak;
import dev.anomoly.modules.AutoSprint;
import dev.anomoly.modules.AutoWalk;
import dev.anomoly.modules.AutoTool;
import dev.anomoly.modules.FastBreak;
import dev.anomoly.modules.FastLadder;
import dev.anomoly.modules.FastPlace;
import dev.anomoly.modules.FovOverride;
import dev.anomoly.modules.KillAura;
import dev.anomoly.modules.MaxBrightness;
import dev.anomoly.modules.NoHurtCam;
import dev.anomoly.modules.NoFall;
import dev.anomoly.modules.NoSlow;
import dev.anomoly.modules.NoBob;
import dev.anomoly.modules.NoFov;
import dev.anomoly.modules.PlayerRadar;
import dev.anomoly.modules.PortalMenu;
import dev.anomoly.modules.Reach;
import dev.anomoly.modules.SpearAssist;
import dev.anomoly.modules.SafeWalk;
import dev.anomoly.modules.Step;
import dev.anomoly.modules.TriggerBot;
import dev.anomoly.modules.Twerk;
import java.util.List;
import net.minecraft.client.Minecraft;

public final class ModuleManager {
    private final List<Module> modules = List.of(
        new TriggerBot(), new KillAura(), new SpearAssist(), new Reach(), new AutoArmor(), new AutoSprint(), new AutoWalk(), new AutoJump(),
        new AutoSneak(), new AutoRespawn(), new AutoEat(), new AutoFish(), new AntiAFK(), new AutoTool(), new FastPlace(), new FastBreak(), new FastLadder(),
        new NoFall(), new NoSlow(), new SafeWalk(), new Step(), new MaxBrightness(), new NoHurtCam(), new NoBob(), new AutoF5(),
        new FovOverride(), new NoFov(), new PlayerRadar(), new AntiReducedDebugInfo(), new PortalMenu(), new Twerk()
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
