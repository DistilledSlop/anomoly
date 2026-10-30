package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.IntSetting;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.PlayerModelPart;

public final class SkinBlinker extends Module {
    private final BooleanSetting random = add(new BooleanSetting("random", "Randomize all layers (SkinDerp)", false));
    private final IntSetting interval = add(new IntSetting("interval_ticks", "Ticks between changes", 4, 1, 40));
    private final Random rng = new Random();
    private int ticks;

    public SkinBlinker() {
        super("skin_blinker", "Skin Blinker", "Flicker your skin layers on and off so other players see them blink (also covers SkinDerp).", Category.FUN);
    }

    @Override public void tick(Minecraft client) {
        if (++ticks % interval.get() != 0) return;
        if (random.get()) {
            for (PlayerModelPart part : PlayerModelPart.values()) client.options.setModelPart(part, rng.nextBoolean());
        } else {
            client.options.setModelPart(PlayerModelPart.JACKET, !client.options.isModelPartEnabled(PlayerModelPart.JACKET));
        }
    }

    @Override protected void onDisable(Minecraft client) {
        for (PlayerModelPart part : PlayerModelPart.values()) client.options.setModelPart(part, true);
    }
}
