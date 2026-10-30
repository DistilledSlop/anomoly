package dev.anomoly.mixin;

import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.NoSwing;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityNoSwingMixin {
    @Inject(method = "getSwingAnimation", at = @At("HEAD"), cancellable = true)
    private void anomoly$noSwing(float partialTick, CallbackInfoReturnable<Float> result) {
        if ((Object)this instanceof LocalPlayer && AnomolyClient.MODULES.get(NoSwing.class).enabled()) result.setReturnValue(0F);
    }
}
