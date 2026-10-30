package dev.anomoly.modules;

import dev.anomoly.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;

public final class KillEffect extends Module implements HitEvents.Listener {
    private LivingEntity victim;
    private int age;

    public KillEffect() {
        super("kill_effect", "KillEffect", "Strike a visual-only lightning bolt where an entity you hit dies.", Category.RENDER);
    }

    @Override public void onHit(Entity target) {
        if (target instanceof LivingEntity living) {
            victim = living;
            age = 0;
        }
    }

    @Override public void tick(Minecraft client) {
        if (victim == null) return;
        if (victim.isDeadOrDying()) {
            LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(client.level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            if (bolt != null) {
                bolt.snapTo(victim.getX(), victim.getY(), victim.getZ());
                bolt.setVisualOnly(true);
                client.level.addEntity(bolt);
            }
            victim = null;
        } else if (++age > 100) victim = null;
    }
}
