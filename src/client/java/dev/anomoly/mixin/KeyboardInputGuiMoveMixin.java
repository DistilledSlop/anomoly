package dev.anomoly.mixin;

import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.GuiMove;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputGuiMoveMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void anomoly$moveInGui(CallbackInfo ci) {
        AnomolyClient.MODULES.get(GuiMove.class).apply((KeyboardInput)(Object)this);
    }
}
