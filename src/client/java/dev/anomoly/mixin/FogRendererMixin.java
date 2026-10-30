package dev.anomoly.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.NoFog;
import dev.anomoly.modules.AntiBlind;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.material.FogType;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
    @ModifyReturnValue(method = "setupFog", at = @At("RETURN"))
    private FogData anomoly$removeFog(FogData fog) {
        boolean noFog = AnomolyClient.MODULES.get(NoFog.class).enabled();
        boolean clearLiquid = AnomolyClient.MODULES.get(AntiBlind.class).liquidFog()
            && Minecraft.getInstance().gameRenderer.mainCamera().getFluidInCamera() != FogType.NONE;
        if (!noFog && !clearLiquid) return fog;
        fog.environmentalStart = 100000.0F;
        fog.environmentalEnd = 100001.0F;
        fog.renderDistanceStart = 100000.0F;
        fog.renderDistanceEnd = 100001.0F;
        fog.skyEnd = 100001.0F;
        fog.cloudEnd = 100001.0F;
        return fog;
    }
}
