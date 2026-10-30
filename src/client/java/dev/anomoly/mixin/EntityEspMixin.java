package dev.anomoly.mixin;

import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.EntityEsp;
import dev.anomoly.modules.TrueSight;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityEspMixin {
    @Inject(method = "isCurrentlyGlowing", at = @At("RETURN"), cancellable = true)
    private void anomoly$esp(CallbackInfoReturnable<Boolean> result) {
        Entity self = (Entity)(Object)this;
        if (!result.getReturnValue() && self.level().isClientSide() && EntityEsp.glows(self)) result.setReturnValue(true);
    }

    @Inject(method = "isInvisibleTo", at = @At("HEAD"), cancellable = true)
    private void anomoly$trueSight(Player viewer, CallbackInfoReturnable<Boolean> result) {
        if (AnomolyClient.MODULES.get(TrueSight.class).enabled()) result.setReturnValue(false);
    }
}
