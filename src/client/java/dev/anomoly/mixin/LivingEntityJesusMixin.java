package dev.anomoly.mixin;

import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.Jesus;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityJesusMixin {
    @Inject(method = "canStandOnFluid", at = @At("HEAD"), cancellable = true)
    private void anomoly$jesus(FluidState fluid, CallbackInfoReturnable<Boolean> result) {
        if ((Object)this instanceof LocalPlayer player && AnomolyClient.MODULES.get(Jesus.class).canStandOn(player, fluid)) {
            result.setReturnValue(true);
        }
    }
}
