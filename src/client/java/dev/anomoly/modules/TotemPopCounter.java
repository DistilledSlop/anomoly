package dev.anomoly.modules;

import dev.anomoly.Module;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public final class TotemPopCounter extends Module {
    private final Map<UUID, Integer> pops = new HashMap<>();

    public TotemPopCounter() {
        super("totem_pop_counter", "TotemPopCounter", "Count and announce locally how many totems each player has popped.", Category.MISC);
    }

    @Override protected void onEnable(Minecraft client) { pops.clear(); }

    public void popped(Minecraft client, Player player) {
        if (!enabled()) return;
        int count = pops.merge(player.getUUID(), 1, Integer::sum);
        client.player.sendSystemMessage(Component.literal("[Anomoly] " + player.getName().getString() + " popped a totem (" + count + ")."));
    }
}
