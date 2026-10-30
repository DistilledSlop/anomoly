package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.StringSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;

public final class NoStatusEffects extends Module {
    private final StringSetting effects = add(new StringSetting("effects", "Effect IDs to remove locally",
        "minecraft:blindness,minecraft:nausea,minecraft:darkness,minecraft:slowness,minecraft:mining_fatigue,minecraft:levitation"));

    public NoStatusEffects() {
        super("no_status_effects", "No Status Effects", "Strip chosen status effects from the local player each tick (also covers AntiBadEffects).", Category.PLAYER);
    }

    @Override public void tick(Minecraft client) {
        String listed = "," + effects.get().replace(" ", "") + ",";
        for (MobEffectInstance effect : client.player.getActiveEffects().toArray(new MobEffectInstance[0])) {
            var key = BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value());
            if (key != null && listed.contains("," + key + ",")) client.player.removeEffect(effect.getEffect());
        }
    }
}
