package dev.anomoly.mixin;

import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.AntiAttack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeAntiAttackMixin {
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void anomoly$spareOwnPets(Player player, Entity target, CallbackInfo ci) {
        if (player == Minecraft.getInstance().player && AnomolyClient.MODULES.get(AntiAttack.class).spares(player, target)) ci.cancel();
    }
}
