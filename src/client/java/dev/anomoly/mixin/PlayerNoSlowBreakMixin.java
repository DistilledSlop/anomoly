package dev.anomoly.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.NoSlowBreak;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public abstract class PlayerNoSlowBreakMixin {
    @ModifyExpressionValue(method = "getDestroySpeed", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/player/Player;onGround()Z"))
    private boolean anomoly$groundedMining(boolean onGround) {
        return onGround || (Object)this instanceof LocalPlayer && AnomolyClient.MODULES.get(NoSlowBreak.class).enabled();
    }

    @ModifyExpressionValue(method = "getDestroySpeed", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/player/Player;isEyeInFluid(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean anomoly$dryMining(boolean inFluid) {
        return inFluid && !((Object)this instanceof LocalPlayer && AnomolyClient.MODULES.get(NoSlowBreak.class).enabled());
    }
}
