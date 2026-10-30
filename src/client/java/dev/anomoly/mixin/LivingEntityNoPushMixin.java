package dev.anomoly.mixin;

import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.NoPush;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityNoPushMixin {
    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    private void anomoly$ignoreEntityCollision(CallbackInfoReturnable<Boolean> ci) {
        if ((Object)this == Minecraft.getInstance().player
            && AnomolyClient.MODULES.get(NoPush.class).entities()) ci.setReturnValue(false);
    }
}
