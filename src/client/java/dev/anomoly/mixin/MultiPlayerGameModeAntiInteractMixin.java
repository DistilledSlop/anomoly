package dev.anomoly.mixin;

import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.AntiInteract;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeAntiInteractMixin {
    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void anomoly$skipProtectedContainer(LocalPlayer player, InteractionHand hand,
        BlockHitResult hit, CallbackInfoReturnable<InteractionResult> callback) {
        if (AnomolyClient.MODULES.get(AntiInteract.class).blocksClick(Minecraft.getInstance(), hit))
            callback.setReturnValue(InteractionResult.FAIL);
    }
}
