package dev.anomoly;

import dev.anomoly.modules.AutoJump;
import dev.anomoly.modules.AutoShoot;
import dev.anomoly.modules.AutoBow;
import dev.anomoly.modules.AutoExp;
import dev.anomoly.modules.AutoPotion;
import dev.anomoly.modules.ThrowPot;
import dev.anomoly.modules.BowAimbot;
import dev.anomoly.modules.BowSpam;
import dev.anomoly.modules.ProjectileAimbot;
import dev.anomoly.modules.AimAssist;
import dev.anomoly.modules.Aimbot;
import dev.anomoly.modules.AntiBot;
import dev.anomoly.modules.AntiReducedDebugInfo;
import dev.anomoly.modules.AntiWeb;
import dev.anomoly.modules.AntiOverlay;
import dev.anomoly.modules.AntiBlind;
import dev.anomoly.modules.AntiWobble;
import dev.anomoly.modules.AutoArmor;
import dev.anomoly.modules.AutoF5;
import dev.anomoly.modules.AutoBowRelease;
import dev.anomoly.modules.AutoGApple;
import dev.anomoly.modules.AntiAFK;
import dev.anomoly.modules.AutoEat;
import dev.anomoly.modules.AutoFish;
import dev.anomoly.modules.AutoRespawn;
import dev.anomoly.modules.AutoReconnect;
import dev.anomoly.modules.AutoSneak;
import dev.anomoly.modules.AutoSprint;
import dev.anomoly.modules.AutoWalk;
import dev.anomoly.modules.AutoTool;
import dev.anomoly.modules.AutoTotem;
import dev.anomoly.modules.AutoReplenish;
import dev.anomoly.modules.AutoDeposit;
import dev.anomoly.modules.AutoDrop;
import dev.anomoly.modules.AutoWeapon;
import dev.anomoly.modules.AutoSword;
import dev.anomoly.modules.AutoClicker;
import dev.anomoly.modules.AutoLeave;
import dev.anomoly.modules.AutoBreak;
import dev.anomoly.modules.AirJump;
import dev.anomoly.modules.AutoSwitch;
import dev.anomoly.modules.CameraClip;
import dev.anomoly.modules.CameraTweaks;
import dev.anomoly.modules.ChestStealer;
import dev.anomoly.modules.ChestCleaner;
import dev.anomoly.modules.FastBreak;
import dev.anomoly.modules.FastLadder;
import dev.anomoly.modules.FastPlace;
import dev.anomoly.modules.FastFall;
import dev.anomoly.modules.FovOverride;
import dev.anomoly.modules.Fullbright;
import dev.anomoly.modules.FreeLook;
import dev.anomoly.modules.KillAura;
import dev.anomoly.modules.MaxBrightness;
import dev.anomoly.modules.NoHurtCam;
import dev.anomoly.modules.NoFall;
import dev.anomoly.modules.NoSlow;
import dev.anomoly.modules.NoBob;
import dev.anomoly.modules.NoFov;
import dev.anomoly.modules.NoFog;
import dev.anomoly.modules.NoFireOverlay;
import dev.anomoly.modules.NoOverlay;
import dev.anomoly.modules.NoRotate;
import dev.anomoly.modules.NoPush;
import dev.anomoly.modules.NoPumpkin;
import dev.anomoly.modules.PlayerRadar;
import dev.anomoly.modules.PortalMenu;
import dev.anomoly.modules.PluginInspector;
import dev.anomoly.modules.Reach;
import dev.anomoly.modules.SpearAssist;
import dev.anomoly.modules.SafeWalk;
import dev.anomoly.modules.Step;
import dev.anomoly.modules.TriggerBot;
import dev.anomoly.modules.Zoom;
import dev.anomoly.modules.Twerk;
import dev.anomoly.modules.TimeChanger;
import dev.anomoly.modules.Velocity;
import dev.anomoly.modules.WeatherChanger;
import java.util.List;
import net.minecraft.client.Minecraft;

public final class ModuleManager {
    private final List<Module> modules = List.of(
        new AutoWeapon(), new AutoSword(), new AutoClicker(), new TriggerBot(), new AntiBot(), new KillAura(), new Aimbot(), new AimAssist(), new SpearAssist(), new Reach(), new AutoLeave(),
        new AutoArmor(), new BowAimbot(), new ProjectileAimbot(), new AutoShoot(), new AutoBow(), new BowSpam(), new AutoBowRelease(), new ThrowPot(), new AutoPotion(), new AutoExp(), new AutoGApple(), new AutoTotem(),
        new AutoSprint(), new AutoWalk(), new AutoJump(), new AirJump(),
        new AutoSneak(), new AutoRespawn(), new AutoEat(), new AutoFish(), new AntiAFK(), new AutoBreak(), new AutoSwitch(), new AutoReconnect(), new NoRotate(),
        new AutoTool(), new AutoReplenish(),
        new AutoDeposit(), new AutoDrop(),
        new ChestStealer(), new ChestCleaner(),
        new FastPlace(), new FastBreak(), new FastLadder(),
        new NoFall(), new NoSlow(), new SafeWalk(), new Step(), new AntiWeb(), new FastFall(), new Velocity(), new NoPush(), new MaxBrightness(), new NoHurtCam(), new NoBob(), new AutoF5(),
        new FovOverride(), new NoFov(), new Zoom(), new CameraClip(), new CameraTweaks(), new FreeLook(),
        new Fullbright(), new NoFog(), new NoOverlay(), new NoFireOverlay(), new AntiOverlay(), new AntiWobble(), new NoPumpkin(), new AntiBlind(), new PlayerRadar(), new WeatherChanger(), new TimeChanger(),
        new AntiReducedDebugInfo(), new PortalMenu(), new PluginInspector(), new Twerk()
    );

    public List<Module> all() { return modules; }

    public <T extends Module> T get(Class<T> type) {
        for (Module module : modules) if (type.isInstance(module)) return type.cast(module);
        throw new IllegalArgumentException("Unregistered module " + type.getName());
    }

    public void tick(Minecraft client) {
        if (client.player == null || client.level == null) return;
        for (Module module : modules) {
            if (client.player == null || client.level == null) break;
            if (module.enabled()) module.tick(client);
        }
    }

    public void disableAll() {
        AnomolyClient.batchConfigChanges(() -> {
            for (Module module : modules) module.setEnabled(false);
        });
    }
}
