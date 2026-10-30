package dev.anomoly.mixin;

import dev.anomoly.modules.AttackHooks;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeAttackMixin {
    @Unique private AttackHooks.Context anomoly$attackContext;

    @Inject(method = "attack", at = @At("HEAD"))
    private void anomoly$beforeAttack(Player player, Entity entity, CallbackInfo ci) {
        anomoly$attackContext = AttackHooks.before(player, entity);
    }

    @Inject(method = "attack", at = @At("TAIL"))
    private void anomoly$afterAttack(Player player, Entity entity, CallbackInfo ci) {
        AttackHooks.after(anomoly$attackContext, player, entity);
        anomoly$attackContext = null;
    }
}
