package dev.anomoly.mixin;

import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.SoundBlocker;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundEngine.class)
public abstract class SoundEngineBlockMixin {
    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private void anomoly$blockSound(SoundInstance sound, CallbackInfoReturnable<SoundEngine.PlayResult> result) {
        if (AnomolyClient.MODULES.get(SoundBlocker.class).blocks(sound.getIdentifier())) result.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
    }
}
