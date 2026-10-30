package dev.anomoly.mixin;

import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.AirPlace;
import dev.anomoly.modules.NoBlockInteract;
import dev.anomoly.modules.NoEntityInteract;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
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
        boolean block = AnomolyClient.MODULES.get(NoBlockInteract.class).bypass(client);
        boolean entity = AnomolyClient.MODULES.get(NoEntityInteract.class).bypass(client);
        if (block || entity) client.hitResult = BlockHitResult.miss(client.hitResult.getLocation(),
            client.player.getDirection(), BlockPos.containing(client.hitResult.getLocation()));
        if (block) return;
        BlockHitResult hit = AnomolyClient.MODULES.get(AirPlace.class).clickTarget(client);
        if (hit != null) client.hitResult = hit;
    }
}
