package dev.anomoly.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.AutoClicker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Minecraft.class)
public abstract class MinecraftAutoClickerMixin {
    @WrapOperation(method = "handleKeybinds", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/KeyMapping;isDown()Z", ordinal = 3))
    private boolean anomoly$rateLimitHeldUse(KeyMapping key, Operation<Boolean> original) {
        boolean down = original.call(key);
        if (!down || key != ((Minecraft)(Object)this).options.keyUse) return down;
        return AnomolyClient.MODULES.get(AutoClicker.class).allowHeldUse();
    }
}
