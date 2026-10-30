package dev.anomoly.mixin;

import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.AntiBlind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import net.minecraft.world.effect.MobEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapRenderStateExtractor.class)
public abstract class LightmapAntiBlindMixin {
    @Inject(method = "extract", at = @At("TAIL"))
    private void anomoly$removeDarkness(LightmapRenderState state, float partialTicks, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || !client.player.hasEffect(MobEffects.DARKNESS)
            || !AnomolyClient.MODULES.get(AntiBlind.class).darkness()) return;
        state.brightness = client.options.gamma().get().floatValue();
        state.darknessEffectScale = 0.0F;
    }
}
