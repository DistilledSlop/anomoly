package dev.anomoly.modules;

import dev.anomoly.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.player.Player;

public final class PopChams extends Module {
    public PopChams() {
        super("pop_chams", "Pop Chams", "Leave a fading ghost box where a player popped a totem.", Category.RENDER);
    }

    public void popped(Minecraft client, Player player) {
        if (!enabled()) return;
        WorldGizmos.draw(client, () -> Gizmos.cuboid(player.getBoundingBox(), GizmoStyle.strokeAndFill(0xFFFFD955, 2.0F, 0x44FFD955))
            .persistForMillis(2000).fadeOut().setAlwaysOnTop());
    }
}
