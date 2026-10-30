package dev.anomoly.mixin;

import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.AntiLegitMiss;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftAntiMissMixin {
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void anomoly$antiMiss(CallbackInfoReturnable<Boolean> result) {
        Minecraft client = (Minecraft)(Object)this;
        if (AnomolyClient.MODULES.get(AntiLegitMiss.class).enabled()
            && (client.hitResult == null || client.hitResult.getType() == HitResult.Type.MISS)) {
            result.setReturnValue(false);
        }
    }
}
