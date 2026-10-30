package dev.anomoly.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MultiPlayerGameMode.class)
public interface MultiPlayerGameModeAccessor {
    @Accessor("destroyDelay") int anomoly$getDestroyDelay();
    @Accessor("destroyDelay") void anomoly$setDestroyDelay(int ticks);
}
