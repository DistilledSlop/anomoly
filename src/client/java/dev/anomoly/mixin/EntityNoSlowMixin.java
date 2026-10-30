package dev.anomoly.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.NoSlow;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public abstract class EntityNoSlowMixin {
    @ModifyReturnValue(method = "getBlockSpeedFactor", at = @At("RETURN"))
    private float anomoly$blockSpeed(float original) {
        return (Object)this instanceof LocalPlayer && AnomolyClient.MODULES.get(NoSlow.class).blockFactor()
            ? Math.max(1.0F, original) : original;
    }
}
