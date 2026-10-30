package dev.anomoly.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.NoSlow;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityNoSlowMixin {
    @Shadow protected Vec3 stuckSpeedMultiplier;

    @ModifyReturnValue(method = "getBlockSpeedFactor", at = @At("RETURN"))
    private float anomoly$blockSpeed(float original) {
        return (Object)this instanceof LocalPlayer && AnomolyClient.MODULES.get(NoSlow.class).blockFactor()
            ? Math.max(1.0F, original) : original;
    }

    @Inject(method = "makeStuckInBlock", at = @At("TAIL"))
    private void anomoly$blockCollision(BlockState state, Vec3 speedMultiplier, CallbackInfo ci) {
        if ((Object)this instanceof LocalPlayer && AnomolyClient.MODULES.get(NoSlow.class).blockFactor()) {
            stuckSpeedMultiplier = Vec3.ZERO;
        }
    }
}
