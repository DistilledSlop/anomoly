package dev.anomoly.modules;

import dev.anomoly.Module;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public final class VisualRange extends Module {
    private final Set<UUID> inRange = new HashSet<>();
    private boolean seeded;

    public VisualRange() {
        super("visual_range", "VisualRange", "Post a local chat note when another player enters or leaves your loaded range.", Category.MISC);
    }

    @Override protected void onEnable(Minecraft client) { inRange.clear(); seeded = false; }

    @Override public void tick(Minecraft client) {
        Set<UUID> now = new HashSet<>();
        for (Player other : client.level.players()) {
            if (other == client.player) continue;
            now.add(other.getUUID());
            if (seeded && inRange.add(other.getUUID())) {
                client.player.sendSystemMessage(Component.literal("[Anomoly] " + other.getName().getString() + " entered visual range."));
            }
        }
        if (seeded) {
            inRange.removeIf(id -> {
                if (now.contains(id)) return false;
                var info = client.getConnection().getPlayerInfo(id);
                client.player.sendSystemMessage(Component.literal("[Anomoly] " + (info == null ? "A player" : info.getProfile().name()) + " left visual range."));
                return true;
            });
        }
        inRange.addAll(now);
        seeded = true;
    }
}
