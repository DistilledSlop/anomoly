package dev.anomoly.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.PortalMenu;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Screen.class)
public abstract class ScreenPortalMixin {
    @ModifyReturnValue(method = "isAllowedInPortal", at = @At("RETURN"))
    private boolean anomoly$allowPortalScreen(boolean original) {
        return original || AnomolyClient.MODULES.get(PortalMenu.class).enabled();
    }
}
