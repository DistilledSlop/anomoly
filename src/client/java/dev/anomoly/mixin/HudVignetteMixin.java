package dev.anomoly.mixin;

import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.NoVignette;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public abstract class HudVignetteMixin {
    @Inject(method = "extractVignette", at = @At("HEAD"), cancellable = true)
    private void anomoly$noVignette(GuiGraphicsExtractor graphics, Entity camera, CallbackInfo ci) {
        if (AnomolyClient.MODULES.get(NoVignette.class).enabled()) ci.cancel();
    }
}
