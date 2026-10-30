package dev.anomoly.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.GpuTexture;
import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.Fullbright;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Lightmap.class)
public abstract class LightmapFullbrightMixin {
    private static final Vector4f ANOMOLY_WHITE = new Vector4f(1F, 1F, 1F, 1F);

    @Shadow @Final private GpuTexture texture;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void anomoly$renderFullbright(LightmapRenderState state, CallbackInfo ci) {
        if (!AnomolyClient.MODULES.get(Fullbright.class).enabled()) return;
        if (state.needsUpdate) RenderSystem.getDevice().createCommandEncoder().clearColorTexture(texture, ANOMOLY_WHITE);
        ci.cancel();
    }
}
