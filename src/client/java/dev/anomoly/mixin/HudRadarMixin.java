package dev.anomoly.mixin;

import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.PlayerRadar;
import dev.anomoly.modules.SmartEat;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public abstract class HudRadarMixin {
    @Shadow private boolean isHidden;

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void anomoly$drawRadar(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (!isHidden) AnomolyClient.MODULES.get(PlayerRadar.class).drawHud(Minecraft.getInstance(), graphics);
        if (!isHidden) AnomolyClient.MODULES.get(SmartEat.class).drawPreview(Minecraft.getInstance(), graphics);
        if (!isHidden) dev.anomoly.modules.TextHud.drawAll(Minecraft.getInstance(), graphics);
        if (!isHidden) AnomolyClient.MODULES.get(dev.anomoly.modules.Hitmarkers.class).draw(Minecraft.getInstance(), graphics);
    }
}
