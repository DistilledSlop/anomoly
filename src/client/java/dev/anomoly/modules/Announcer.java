package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class Announcer extends Module {
    private final BooleanSetting movement = add(new BooleanSetting("distance", "Distance traveled", true));
    private final FloatSetting interval = add(new FloatSetting("blocks", "Announce every N blocks", 100, 10, 5000));
    private double travelled, lastX, lastZ;
    private boolean seeded;

    public Announcer() {
        super("announcer", "Announcer", "Post a local chat note each time you travel another set distance.", Category.MISC);
    }

    @Override protected void onEnable(Minecraft client) { travelled = 0; seeded = false; }

    @Override public void tick(Minecraft client) {
        var player = client.player;
        if (seeded && movement.get()) {
            double step = Math.hypot(player.getX() - lastX, player.getZ() - lastZ);
            if (step < 10) travelled += step;
            if (travelled >= interval.get()) {
                travelled = 0;
                player.sendSystemMessage(Component.literal("[Anomoly] Traveled another " + (int)(double)interval.get() + " blocks."));
            }
        }
        lastX = player.getX();
        lastZ = player.getZ();
        seeded = true;
    }
}
