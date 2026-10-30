package dev.anomoly.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.Step;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class LivingEntityStepMixin {
    @ModifyReturnValue(method = "maxUpStep", at = @At("RETURN"))
    private float anomoly$stepHeight(float original) {
        if (!((Object)this instanceof LocalPlayer)) return original;
        Step step = AnomolyClient.MODULES.get(Step.class);
        return step.enabled() ? Math.max(original, step.height()) : original;
    }
}
