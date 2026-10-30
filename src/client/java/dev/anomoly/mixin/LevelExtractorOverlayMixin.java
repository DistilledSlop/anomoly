package dev.anomoly.mixin;

import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.AntiOverlay;
import dev.anomoly.modules.AntiBlind;
import dev.anomoly.modules.AntiWobble;
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
        boolean noOverlay = AnomolyClient.MODULES.get(NoOverlay.class).enabled();
        AntiBlind antiBlind = AnomolyClient.MODULES.get(AntiBlind.class);
        if (all || noOverlay || antiBlind.block()) state.blockOverlay = null;
        if (all || noOverlay || antiBlind.water()) state.waterOverlay = null;
        if (all || AnomolyClient.MODULES.get(NoFireOverlay.class).enabled() || antiBlind.fire()) state.isOnFire = false;
        AntiWobble antiWobble = AnomolyClient.MODULES.get(AntiWobble.class);
        if (antiWobble.portal() || antiBlind.portal()) state.portalEffectIntensity = 0.0F;
        if (antiWobble.nausea() || antiBlind.nausea()) state.nauseaEffectIntensity = 0.0F;
    }
}
