package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.Vec3;

public final class Tracers extends Module {
    private final BooleanSetting players = add(new BooleanSetting("players", "Players", true));
    private final BooleanSetting hostile = add(new BooleanSetting("hostile", "Hostile mobs", false));
    private final BooleanSetting items = add(new BooleanSetting("items", "Items", false));

    public Tracers() {
        super("tracers", "Tracers", "Draw lines from your crosshair to selected nearby entities.", Category.RENDER);
    }

    @Override public void tick(Minecraft client) {
        var player = client.player;
        Vec3 origin = player.getEyePosition().add(player.getLookAngle().scale(0.6));
        WorldGizmos.draw(client, () -> {
            for (Entity entity : client.level.entitiesForRendering()) {
                if (entity == player || player.distanceToSqr(entity) > 128 * 128) continue;
                int color;
                if (entity instanceof Player) { if (!players.get()) continue; color = 0xFFFFFF55; }
                else if (entity instanceof Enemy) { if (!hostile.get()) continue; color = 0xFFFF5555; }
                else if (entity instanceof ItemEntity) { if (!items.get()) continue; color = 0xFF55FF55; }
                else continue;
                Gizmos.line(origin, entity.getBoundingBox().getCenter(), color).setAlwaysOnTop();
            }
        });
    }
}
