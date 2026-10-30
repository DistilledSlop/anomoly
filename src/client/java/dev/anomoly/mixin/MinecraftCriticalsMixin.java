package dev.anomoly.mixin;

import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.Criticals;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Suppress the early swing and punch packet while Criticals waits for descent. */
@Mixin(Minecraft.class)
public abstract class MinecraftCriticalsMixin {
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void anomoly$deferCriticalAttack(CallbackInfoReturnable<Boolean> cir) {
        Minecraft client = (Minecraft)(Object)this;
        if (client.hitResult instanceof EntityHitResult hit
            && AnomolyClient.MODULES.get(Criticals.class).intercept(client.player, hit.getEntity()))
            cir.setReturnValue(false);
    }
}
