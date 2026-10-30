package dev.anomoly.mixin;

import dev.anomoly.modules.FreeCam;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraFreeCamMixin {
    @Shadow protected abstract void setPosition(double x, double y, double z);

    @Shadow protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "alignWithEntity", at = @At("TAIL"))
    private void anomoly$freeCam(float partialTick, CallbackInfo ci) {
        FreeCam freeCam = FreeCam.get();
        if (!freeCam.active()) return;
        Vec3 at = freeCam.position();
        setPosition(at.x, at.y, at.z);
        setRotation(freeCam.yaw(), freeCam.pitch());
    }
}
