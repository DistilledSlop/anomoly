package dev.anomoly.mixin;

import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.ClientSpoof;
import net.minecraft.client.ClientBrandRetriever;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientBrandRetriever.class)
public abstract class ClientBrandSpoofMixin {
    @Inject(method = "getClientModName", at = @At("HEAD"), cancellable = true)
    private static void anomoly$spoofBrand(CallbackInfoReturnable<String> result) {
        ClientSpoof spoof = AnomolyClient.MODULES.get(ClientSpoof.class);
        if (spoof.enabled()) result.setReturnValue(spoof.brand());
    }
}
