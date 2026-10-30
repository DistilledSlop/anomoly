package dev.anomoly.mixin;

import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.BlockBounce;
import dev.anomoly.modules.HighJump;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityJumpModulesMixin {
    @Inject(method = "jumpFromGround", at = @At("TAIL"))
    private void anomoly$adjustJump(CallbackInfo callback) {
        if ((Object)this instanceof LocalPlayer player) {
            AnomolyClient.MODULES.get(HighJump.class).onJump(player);
            AnomolyClient.MODULES.get(BlockBounce.class).onJump(player);
        }
    }
}
