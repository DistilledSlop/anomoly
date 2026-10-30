package dev.anomoly.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.Multitask;
import dev.anomoly.modules.NoEntityTrace;
import java.util.function.Predicate;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerInteractionMixin {
    @WrapOperation(method = "pick", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/projectile/ProjectileUtil;getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;"))
    private static EntityHitResult anomoly$skipEntities(Entity source, Vec3 from, Vec3 to, AABB box, Predicate<Entity> filter,
                                                        double maxDistance, Operation<EntityHitResult> original) {
        if (AnomolyClient.MODULES.get(NoEntityTrace.class).ignoring()) return null;
        return original.call(source, from, to, box, filter, maxDistance);
    }

    @Inject(method = "isHandsBusy", at = @At("RETURN"), cancellable = true)
    private void anomoly$multitask(CallbackInfoReturnable<Boolean> result) {
        if (AnomolyClient.MODULES.get(Multitask.class).enabled()) result.setReturnValue(false);
    }
}
