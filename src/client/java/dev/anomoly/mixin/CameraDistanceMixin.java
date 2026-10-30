package dev.anomoly.mixin;

import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.CameraClip;
import dev.anomoly.modules.CameraTweaks;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraDistanceMixin {
    @ModifyArg(method = "alignWithEntity", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/Camera;getMaxZoom(F)F"), index = 0)
    private float anomoly$cameraDistance(float original) {
        CameraTweaks tweaks = AnomolyClient.MODULES.get(CameraTweaks.class);
        return tweaks.enabled() ? original * tweaks.distanceFactor() : original;
    }

    @Inject(method = "getMaxZoom", at = @At("HEAD"), cancellable = true)
    private void anomoly$cameraClip(float requested, CallbackInfoReturnable<Float> result) {
        if (AnomolyClient.MODULES.get(CameraClip.class).enabled()) result.setReturnValue(requested);
    }
}
