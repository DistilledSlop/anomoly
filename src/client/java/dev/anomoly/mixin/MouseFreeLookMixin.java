package dev.anomoly.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.FreeLook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MouseHandler.class)
public abstract class MouseFreeLookMixin {
    @WrapOperation(method = "turnPlayer", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void anomoly$turnCamera(LocalPlayer player, double horizontal, double vertical,
        Operation<Void> original) {
        FreeLook freeLook = AnomolyClient.MODULES.get(FreeLook.class);
        if (freeLook.active(Minecraft.getInstance())) freeLook.turn(horizontal, vertical);
        else original.call(player, horizontal, vertical);
    }
}
