package dev.anomoly.mixin;

import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.Hitbox;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityHitboxMixin {
    @Inject(method = "getPickRadius", at = @At("RETURN"), cancellable = true)
    private void anomoly$hitbox(CallbackInfoReturnable<Float> result) {
        if ((Object)this != Minecraft.getInstance().player) {
            result.setReturnValue(result.getReturnValue() + AnomolyClient.MODULES.get(Hitbox.class).expansion());
        }
    }
}
