package dev.anomoly.mixin;

import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.NoJumpDelay;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityNoJumpDelayMixin {
    @Shadow private int noJumpDelay;

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void anomoly$clearJumpGate(CallbackInfo callback) {
        if ((Object)this instanceof LocalPlayer && AnomolyClient.MODULES.get(NoJumpDelay.class).enabled())
            noJumpDelay = 0;
    }
}
