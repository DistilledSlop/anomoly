package dev.anomoly.modules;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class KillFeed extends TextHud implements HitEvents.Listener {
    private record Pending(LivingEntity target, int age) { }

    private final Deque<String> feed = new ArrayDeque<>();
    private LivingEntity victim;
    private int age;

    public KillFeed() {
        super("kill_feed", "KillFeed", "List the last few entities you killed on the HUD.");
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
            feed.addFirst("You killed " + victim.getName().getString());
            while (feed.size() > 5) feed.removeLast();
            victim = null;
        } else if (++age > 100) victim = null;
    }

    @Override protected List<String> lines(Minecraft client) { return List.copyOf(feed); }
}
