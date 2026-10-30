package dev.anomoly.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.Reach;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerReachMixin {
    @ModifyExpressionValue(method = "raycastHitResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;blockInteractionRange()D"))
    private double anomoly$blockRange(double original) {
        Reach reach = AnomolyClient.MODULES.get(Reach.class);
        return reach.enabled() ? Math.max(original, reach.blockRange()) : original;
    }

    @ModifyExpressionValue(method = "raycastHitResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;entityInteractionRange()D"))
    private double anomoly$entityRange(double original) {
        Reach reach = AnomolyClient.MODULES.get(Reach.class);
        return reach.enabled() ? Math.max(original, reach.entityRange()) : original;
    }
}
