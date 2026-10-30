package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.StringSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;

public final class PotionSpoof extends Module {
    private final StringSetting effects = add(new StringSetting("effects", "Effects (id:level, comma separated)", "minecraft:night_vision:1"));

    public PotionSpoof() {
        super("potion_spoof", "PotionSpoof", "Apply chosen status effects to your local player every tick; the server does not see them.", Category.PLAYER);
    }

    @Override public void tick(Minecraft client) {
        for (String entry : effects.get().split(",")) {
            String[] parts = entry.trim().split(":");
            if (parts.length < 2) continue;
            Identifier id = Identifier.tryParse(parts[0] + ":" + parts[1]);
            int level = 1;
            try {
                if (parts.length > 2) level = Math.max(1, Integer.parseInt(parts[2]));
            } catch (NumberFormatException ignored) {
                // Fall back to level 1 for a malformed level.
            }
            if (id == null) continue;
            int amplifier = level - 1;
            BuiltInRegistries.MOB_EFFECT.get(id).ifPresent(holder -> {
                MobEffectInstance current = client.player.getEffect(holder);
                if (current == null || current.getDuration() < 220 || current.getAmplifier() < amplifier) {
                    client.player.addEffect(new MobEffectInstance(holder, 400, amplifier, false, false));
                }
            });
        }
    }
}
