package dev.anomoly.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.AntiWobble;
import dev.anomoly.modules.NoPumpkin;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Hud.class)
public abstract class HudClearViewMixin {
    @ModifyExpressionValue(method = "extractCameraOverlays", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/item/equipment/Equippable;cameraOverlay()Ljava/util/Optional;"))
    private Optional<Identifier> anomoly$hidePumpkin(Optional<Identifier> original) {
        if (AnomolyClient.MODULES.get(NoPumpkin.class).enabled()
            && original.isPresent() && original.get().equals(Identifier.withDefaultNamespace("misc/pumpkinblur"))) {
            return Optional.empty();
        }
        return original;
    }

    @WrapOperation(method = "extractCameraOverlays", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/Hud;extractPortalOverlay(Lnet/minecraft/client/gui/GuiGraphicsExtractor;F)V"))
    private void anomoly$hidePortal(Hud hud, GuiGraphicsExtractor graphics, float intensity, Operation<Void> original) {
        if (!AnomolyClient.MODULES.get(AntiWobble.class).portal()) original.call(hud, graphics, intensity);
    }

    @WrapOperation(method = "extractCameraOverlays", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/Hud;extractConfusionOverlay(Lnet/minecraft/client/gui/GuiGraphicsExtractor;F)V"))
    private void anomoly$hideNausea(Hud hud, GuiGraphicsExtractor graphics, float intensity, Operation<Void> original) {
        if (!AnomolyClient.MODULES.get(AntiWobble.class).nausea()) original.call(hud, graphics, intensity);
    }
}
