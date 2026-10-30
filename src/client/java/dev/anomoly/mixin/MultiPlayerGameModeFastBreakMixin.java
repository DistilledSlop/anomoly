package dev.anomoly.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.FastBreak;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeFastBreakMixin {
    @ModifyExpressionValue(method = "continueDestroyBlock", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/block/state/BlockState;getDestroyProgress(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)F"))
    private float anomoly$miningProgress(float original) {
        FastBreak module = AnomolyClient.MODULES.get(FastBreak.class);
        return module.enabled() ? original * module.multiplier() : original;
    }
}
