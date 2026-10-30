package dev.anomoly.mixin;

import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.VeinMiner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeVeinMinerMixin {
    @Inject(method = "startDestroyBlock", at = @At("HEAD"))
    private void anomoly$startVein(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> result) {
        AnomolyClient.MODULES.get(VeinMiner.class).onManualStart(Minecraft.getInstance(), pos);
    }
}
