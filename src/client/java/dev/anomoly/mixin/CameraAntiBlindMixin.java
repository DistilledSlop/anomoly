package dev.anomoly.mixin;

import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.AntiBlind;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraAntiBlindMixin {
    @Shadow private Entity entity;

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void anomoly$restoreSky(CameraRenderState state, DeltaTracker deltaTracker, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (entity != client.player || client.player == null) return;
        AntiBlind antiBlind = AnomolyClient.MODULES.get(AntiBlind.class);
        state.entityRenderState.doesMobEffectBlockSky =
            (client.player.hasEffect(MobEffects.BLINDNESS) && !antiBlind.blindness())
                || (client.player.hasEffect(MobEffects.DARKNESS) && !antiBlind.darkness());
    }
}
