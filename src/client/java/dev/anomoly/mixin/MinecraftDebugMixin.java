package dev.anomoly.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.AntiReducedDebugInfo;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Minecraft.class)
public abstract class MinecraftDebugMixin {
    @ModifyReturnValue(method = "showOnlyReducedInfo", at = @At("RETURN"))
    private boolean anomoly$fullDebug(boolean original) {
        return original && !AnomolyClient.MODULES.get(AntiReducedDebugInfo.class).enabled();
    }
}
