package dev.anomoly.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.NoSlow;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerNoSlowMixin {
    @ModifyReturnValue(method = "itemUseSpeedMultiplier", at = @At("RETURN"))
    private float anomoly$itemSpeed(float original) {
        return AnomolyClient.MODULES.get(NoSlow.class).itemUse() ? 1.0F : original;
    }

    @ModifyReturnValue(method = "isSlowDueToUsingItem", at = @At("RETURN"))
    private boolean anomoly$allowSprint(boolean original) {
        return original && !AnomolyClient.MODULES.get(NoSlow.class).itemUse();
    }
}
