package dev.anomoly.modules;

import dev.anomoly.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.player.Player;

public final class Hats extends Module {
    public Hats() {
        super("hats", "Hats", "Float a glowing ring above every player's head, including your own in third person.", Category.RENDER);
    }

    @Override public void tick(Minecraft client) {
        WorldGizmos.draw(client, () -> {
            for (Player player : client.level.players()) {
                Gizmos.circle(player.position().add(0, player.getBbHeight() + 0.25, 0), 0.35F, GizmoStyle.stroke(0xFFFFD955, 3.0F));
            }
        });
    }
}
