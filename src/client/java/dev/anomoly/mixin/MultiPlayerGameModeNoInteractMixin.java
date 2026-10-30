package dev.anomoly.mixin;

import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.NoInteract;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeNoInteractMixin {
    @Shadow public abstract boolean isDestroying();
    @Shadow public abstract void stopDestroyBlock();

    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void anomoly$blockMineStart(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> result) {
        if (AnomolyClient.MODULES.get(NoInteract.class).blockMine(Minecraft.getInstance(), pos))
            result.setReturnValue(false);
    }

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void anomoly$blockMineContinue(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> result) {
        if (AnomolyClient.MODULES.get(NoInteract.class).blockMine(Minecraft.getInstance(), pos)) {
            if (isDestroying()) stopDestroyBlock();
            result.setReturnValue(false);
        }
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void anomoly$blockUse(LocalPlayer player, InteractionHand hand,
        BlockHitResult hit, CallbackInfoReturnable<InteractionResult> result) {
        if (AnomolyClient.MODULES.get(NoInteract.class).blockUse(Minecraft.getInstance(), hit, hand))
            result.setReturnValue(InteractionResult.PASS);
    }

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void anomoly$entityHit(Player player, Entity entity, CallbackInfo callback) {
        if (AnomolyClient.MODULES.get(NoInteract.class).entityHit(entity)) callback.cancel();
    }

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void anomoly$entityUse(Player player, Entity entity, EntityHitResult hit, InteractionHand hand,
        CallbackInfoReturnable<InteractionResult> result) {
        if (AnomolyClient.MODULES.get(NoInteract.class).entityUse(entity, hand))
            result.setReturnValue(InteractionResult.PASS);
    }
}
