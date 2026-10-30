package dev.anomoly.modules;

import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class KillStats extends TextHud implements HitEvents.Listener {
    private LivingEntity victim;
    private int age, kills, deaths, streak;
    private boolean wasDead;

    public KillStats() {
        super("kill_stats", "KillStats", "Count your kills, deaths, kill streak and K/D ratio on the HUD (also covers KillFeed's count).");
    }

    @Override public void onHit(Entity target) {
        if (target instanceof LivingEntity living) {
            victim = living;
            age = 0;
        }
    }

    @Override public void tick(Minecraft client) {
        boolean dead = client.player.isDeadOrDying();
        if (dead && !wasDead) {
            deaths++;
            streak = 0;
        }
        wasDead = dead;
        if (victim == null) return;
        if (victim.isDeadOrDying()) {
            kills++;
            streak++;
            victim = null;
        } else if (++age > 100) victim = null;
    }

    @Override protected List<String> lines(Minecraft client) {
        return List.of(String.format(Locale.ROOT, "K %d  D %d  Streak %d  K/D %.2f", kills, deaths, streak, deaths == 0 ? kills : (double)kills / deaths));
    }
}
