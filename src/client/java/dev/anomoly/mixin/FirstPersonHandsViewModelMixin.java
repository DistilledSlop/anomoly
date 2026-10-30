package dev.anomoly.mixin;

import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.ViewModel;
import net.minecraft.client.player.FirstPersonHandsAndItems;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FirstPersonHandsAndItems.class)
public abstract class FirstPersonHandsViewModelMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void anomoly$viewModel(LocalPlayer player, float partialTick, FirstPersonHandsAndItemsRenderState state, CallbackInfo ci) {
        AnomolyClient.MODULES.get(ViewModel.class).apply(state);
    }
}
