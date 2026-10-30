package dev.anomoly.mixin;

import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.Timer;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DeltaTracker.Timer.class)
public abstract class TimerMixin {
    @Shadow @Final @Mutable private float msPerTick;
    @Unique private float anomoly$baseMsPerTick = -1;

    @Inject(method = "advanceGameTime", at = @At("HEAD"))
    private void anomoly$scaleTickLength(long nowMs, CallbackInfoReturnable<Integer> result) {
        if (anomoly$baseMsPerTick < 0) anomoly$baseMsPerTick = msPerTick;
        msPerTick = anomoly$baseMsPerTick / AnomolyClient.MODULES.get(Timer.class).factor();
    }
}
