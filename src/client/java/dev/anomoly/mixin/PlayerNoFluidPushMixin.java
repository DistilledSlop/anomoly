package dev.anomoly.mixin;

import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.NoPush;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerNoFluidPushMixin {
    @Inject(method = "isPushedByFluid", at = @At("HEAD"), cancellable = true)
    private void anomoly$ignoreCurrent(CallbackInfoReturnable<Boolean> ci) {
        if ((Object)this == Minecraft.getInstance().player
            && AnomolyClient.MODULES.get(NoPush.class).fluids()) ci.setReturnValue(false);
    }
}
