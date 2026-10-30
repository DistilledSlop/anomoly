package dev.anomoly.mixin;

import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.AirPlace;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftAirPlaceMixin {
    @Inject(method = "startUseItem", at = @At("HEAD"))
    private void anomoly$airPlaceOnClick(CallbackInfo callback) {
        Minecraft client = (Minecraft)(Object)this;
        BlockHitResult hit = AnomolyClient.MODULES.get(AirPlace.class).clickTarget(client);
        if (hit != null) client.hitResult = hit;
    }
}
