package dev.anomoly.modules;

import dev.anomoly.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;

public final class JumpCircle extends Module {
    private boolean wasGrounded = true;

    public JumpCircle() {
        super("jump_circle", "JumpCircle", "Leave a fading circle on the ground where you jump (also covers JumpEffect).", Category.RENDER);
    }

    @Override public void tick(Minecraft client) {
        var player = client.player;
        boolean takeoff = wasGrounded && !player.onGround() && player.getDeltaMovement().y > 0;
        wasGrounded = player.onGround();
        if (!takeoff) return;
        WorldGizmos.draw(client, () -> Gizmos.circle(player.position().add(0, 0.05, 0), 0.8F, GizmoStyle.stroke(0xFF55DDBB, 2.0F))
            .persistForMillis(1500).fadeOut());
    }
}
