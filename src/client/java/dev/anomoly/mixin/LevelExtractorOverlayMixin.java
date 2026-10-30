package dev.anomoly.mixin;

import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.AntiOverlay;
import dev.anomoly.modules.NoFireOverlay;
import dev.anomoly.modules.NoOverlay;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelExtractor.class)
public abstract class LevelExtractorOverlayMixin {
    @Inject(method = "extractPlayerState", at = @At("TAIL"))
    private void anomoly$screenOverlays(Camera camera, DeltaTracker deltaTracker, float partialTicks,
                                         PlayerRenderState state, CallbackInfo ci) {
        boolean all = AnomolyClient.MODULES.get(AntiOverlay.class).enabled();
        if (all || AnomolyClient.MODULES.get(NoOverlay.class).enabled()) {
            state.blockOverlay = null;
            state.waterOverlay = null;
        }
        if (all || AnomolyClient.MODULES.get(NoFireOverlay.class).enabled()) state.isOnFire = false;
    }
}
