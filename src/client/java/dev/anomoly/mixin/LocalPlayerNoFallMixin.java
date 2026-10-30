package dev.anomoly.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.NoFall;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerNoFallMixin {
    @ModifyExpressionValue(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;onGround()Z"))
    private boolean anomoly$reportedOnGround(boolean original) {
        return original || AnomolyClient.MODULES.get(NoFall.class).spoofGround((LocalPlayer)(Object)this);
    }
}
