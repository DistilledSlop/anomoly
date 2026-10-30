package dev.anomoly.mixin;

import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.AntiBounce;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityAntiBounceMixin {
    @Inject(method = "isSuppressingBounce", at = @At("HEAD"), cancellable = true)
    private void anomoly$landWithoutRebound(CallbackInfoReturnable<Boolean> result) {
        Minecraft client = Minecraft.getInstance();
        if (client != null && (Object)this == client.player
            && AnomolyClient.MODULES.get(AntiBounce.class).enabled()) result.setReturnValue(true);
    }
}
