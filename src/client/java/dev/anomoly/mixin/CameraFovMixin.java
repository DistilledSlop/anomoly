package dev.anomoly.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.FovOverride;
import dev.anomoly.modules.NoFov;
import dev.anomoly.modules.Zoom;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Camera.class)
public abstract class CameraFovMixin {
    @ModifyReturnValue(method = "getFov", at = @At("RETURN"))
    private float anomoly$fov(float original) {
        Zoom zoom = AnomolyClient.MODULES.get(Zoom.class);
        if (zoom.enabled() && AnomolyClient.zoomHeld() && Minecraft.getInstance().gui.screen() == null) return zoom.degrees();
        FovOverride override = AnomolyClient.MODULES.get(FovOverride.class);
        if (override.enabled()) return override.degrees();
        if (AnomolyClient.MODULES.get(NoFov.class).enabled()) return Minecraft.getInstance().options.fov().get();
        return original;
    }
}
