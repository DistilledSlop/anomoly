package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;
import dev.anomoly.setting.StringSetting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;

public final class Follow extends Module {
    private final StringSetting name = add(new StringSetting("target", "Player name (blank for nearest)", ""));
    private final FloatSetting distance = add(new FloatSetting("distance", "Stop distance", 2.5F, 1.0F, 10.0F));

    public Follow() {
        super("follow", "Follow", "Walk toward a chosen or the nearest player and jump over obstacles.", Category.MOVEMENT);
    }

    @Override public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        Player target = null;
        for (Player other : client.level.players()) {
            if (other == player || !name.get().isBlank() && !other.getName().getString().equalsIgnoreCase(name.get().trim())) continue;
            if (target == null || player.distanceToSqr(other) < player.distanceToSqr(target)) target = other;
        }
        boolean walk = target != null && client.gui.screen() == null && player.distanceTo(target) > distance.get();
        if (walk) {
            double dx = target.getX() - player.getX();
            double dz = target.getZ() - player.getZ();
            player.setYRot((float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0));
        }
        client.options.keyUp.setDown(walk);
        client.options.keyJump.setDown(walk && player.horizontalCollision);
    }

    @Override protected void onDisable(Minecraft client) { KeyMapping.setAll(); }
}
