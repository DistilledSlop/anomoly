package dev.anomoly.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.AntiBlind;
import net.minecraft.client.renderer.fog.environment.MobEffectFogEnvironment;
import net.minecraft.world.effect.MobEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MobEffectFogEnvironment.class)
public abstract class MobEffectFogAntiBlindMixin {
    @ModifyReturnValue(method = "isApplicable", at = @At("RETURN"))
    private boolean anomoly$ignoreBlindnessFog(boolean original) {
        if (!original) return false;
        AntiBlind antiBlind = AnomolyClient.MODULES.get(AntiBlind.class);
        MobEffectFogEnvironment environment = (MobEffectFogEnvironment)(Object)this;
        if (environment.getMobEffect() == MobEffects.BLINDNESS && antiBlind.blindness()) return false;
        return environment.getMobEffect() != MobEffects.DARKNESS || !antiBlind.darkness();
    }
}
