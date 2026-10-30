package dev.anomoly.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.TridentFly;
import net.minecraft.world.item.TridentItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TridentItem.class)
public abstract class TridentItemDryRiptideMixin {
    @ModifyExpressionValue(method = "use", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/player/Player;isInWaterOrRain()Z"))
    private boolean anomoly$dryUse(boolean wet) {
        return wet || AnomolyClient.MODULES.get(TridentFly.class).enabled();
    }

    @ModifyExpressionValue(method = "releaseUsing", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/player/Player;isInWaterOrRain()Z"))
    private boolean anomoly$dryRelease(boolean wet) {
        return wet || AnomolyClient.MODULES.get(TridentFly.class).enabled();
    }
}
