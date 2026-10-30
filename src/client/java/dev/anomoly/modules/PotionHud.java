package dev.anomoly.modules;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffectInstance;

public final class PotionHud extends TextHud {
    public PotionHud() {
        super("potion_hud", "PotionHud", "List active status effects with level and remaining time on the HUD.");
    }

    @Override protected List<String> lines(Minecraft client) {
        return client.player.getActiveEffects().stream().map(PotionHud::describe).toList();
    }

    private static String describe(MobEffectInstance effect) {
        int seconds = effect.isInfiniteDuration() ? -1 : effect.getDuration() / 20;
        String time = seconds < 0 ? "∞" : String.format("%d:%02d", seconds / 60, seconds % 60);
        return effect.getEffect().value().getDisplayName().getString() + " " + (effect.getAmplifier() + 1) + "  " + time;
    }
}
