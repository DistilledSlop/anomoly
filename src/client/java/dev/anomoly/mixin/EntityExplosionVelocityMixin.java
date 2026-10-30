package dev.anomoly.mixin;

import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.Velocity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Entity.class)
public abstract class EntityExplosionVelocityMixin {
    @ModifyArg(method = "pushFromExplosion", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/Entity;push(Lnet/minecraft/world/phys/Vec3;)V"), index = 0)
    private Vec3 anomoly$scaleExplosionVelocity(Vec3 impulse) {
        if ((Object)this != Minecraft.getInstance().player) return impulse;
        return AnomolyClient.MODULES.get(Velocity.class).scale(impulse);
    }
}
