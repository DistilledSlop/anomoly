package dev.anomoly.mixin;

import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.NoRender;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleEngine.class)
public abstract class ParticleEngineNoRenderMixin {
    @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
    private void anomoly$noParticles(ParticleOptions options, double x, double y, double z, double dx, double dy, double dz,
                                     CallbackInfoReturnable<Particle> result) {
        if (AnomolyClient.MODULES.get(NoRender.class).blockParticles()) result.setReturnValue(null);
    }
}
