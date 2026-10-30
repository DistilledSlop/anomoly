package dev.anomoly.mixin;

import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.NoBackground;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenNoBackgroundMixin {
    @Inject(method = "extractTransparentBackground", at = @At("HEAD"), cancellable = true)
    private void anomoly$noDarkening(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (AnomolyClient.MODULES.get(NoBackground.class).enabled()) ci.cancel();
    }

    @Inject(method = "extractBlurredBackground", at = @At("HEAD"), cancellable = true)
    private void anomoly$noBlur(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (AnomolyClient.MODULES.get(NoBackground.class).enabled()) ci.cancel();
    }
}
