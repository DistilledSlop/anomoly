package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.ChoiceSetting;
import dev.anomoly.setting.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;

public final class HitParticles extends Module implements HitEvents.Listener {
    private final ChoiceSetting kind = add(new ChoiceSetting("kind", "Particle", "Crit", "Crit", "Heart", "Flame", "Totem", "Smoke"));
    private final IntSetting count = add(new IntSetting("count", "Count", 12, 1, 60));

    public HitParticles() {
        super("hit_particles", "HitParticles", "Spray local particles from entities you hit (also covers HitFX, HitBubbles and DamageParticles).", Category.RENDER);
    }

    @Override public void onHit(Entity target) {
        Minecraft client = Minecraft.getInstance();
        ParticleOptions particle = switch (kind.get()) {
            case "Heart" -> ParticleTypes.HEART;
            case "Flame" -> ParticleTypes.FLAME;
            case "Totem" -> ParticleTypes.TOTEM_OF_UNDYING;
            case "Smoke" -> ParticleTypes.LARGE_SMOKE;
            default -> ParticleTypes.CRIT;
        };
        for (int i = 0; i < count.get(); i++) {
            client.level.addParticle(particle, target.getRandomX(0.5), target.getRandomY(), target.getRandomZ(0.5), 0, 0.1, 0);
        }
    }
}
