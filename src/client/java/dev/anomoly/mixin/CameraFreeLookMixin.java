package dev.anomoly.mixin;

import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.FreeLook;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Camera.class)
public abstract class CameraFreeLookMixin {
    @Shadow private Entity entity;

    @ModifyArgs(method = "alignWithEntity", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/Camera;setRotation(FF)V", ordinal = 0))
    private void anomoly$minecartCamera(Args args) { anomoly$adjust(args); }

    @ModifyArgs(method = "alignWithEntity", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/Camera;setRotation(FF)V", ordinal = 1))
    private void anomoly$normalCamera(Args args) { anomoly$adjust(args); }

    private void anomoly$adjust(Args args) {
        Minecraft client = Minecraft.getInstance();
        if (entity != client.player) return;
        FreeLook freeLook = AnomolyClient.MODULES.get(FreeLook.class);
        if (!freeLook.active(client)) return;
        args.set(0, freeLook.yaw());
        args.set(1, freeLook.pitch());
    }
}
