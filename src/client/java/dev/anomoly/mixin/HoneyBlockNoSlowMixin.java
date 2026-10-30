package dev.anomoly.mixin;

import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.NoSlow;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.HoneyBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HoneyBlock.class)
public abstract class HoneyBlockNoSlowMixin {
    @Inject(method = "isSlidingDown", at = @At("HEAD"), cancellable = true)
    private void anomoly$skipHoneySlide(BlockPos pos, Entity entity, CallbackInfoReturnable<Boolean> ci) {
        if (entity == Minecraft.getInstance().player
            && AnomolyClient.MODULES.get(NoSlow.class).blockFactor()) ci.setReturnValue(false);
    }
}
