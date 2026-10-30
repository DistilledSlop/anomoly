package dev.anomoly.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.anomoly.modules.FreeCam;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MouseHandler.class)
public abstract class MouseFreeCamMixin {
    @WrapOperation(method = "turnPlayer", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void anomoly$turnCamera(LocalPlayer player, double horizontal, double vertical, Operation<Void> original) {
        FreeCam freeCam = FreeCam.get();
        if (freeCam.active()) freeCam.turn(horizontal, vertical);
        else original.call(player, horizontal, vertical);
    }
}
