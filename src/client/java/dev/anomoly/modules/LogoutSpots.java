package dev.anomoly.modules;

import dev.anomoly.Module;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

public final class LogoutSpots extends Module {
    private record Spot(String name, AABB box) { }

    private final Map<UUID, Spot> lastSeen = new HashMap<>();
    private final Map<UUID, Spot> spots = new HashMap<>();

    public LogoutSpots() {
        super("logout_spots", "LogoutSpots", "Mark where other players were standing when they left the server (also covers LogoffSpot).", Category.RENDER);
    }

    @Override protected void onEnable(Minecraft client) { lastSeen.clear(); spots.clear(); }

    @Override public void tick(Minecraft client) {
        for (Player other : client.level.players()) {
            if (other == client.player) continue;
            lastSeen.put(other.getUUID(), new Spot(other.getName().getString(), other.getBoundingBox()));
            spots.remove(other.getUUID());
        }
        lastSeen.entrySet().removeIf(entry -> {
            if (client.getConnection().getPlayerInfo(entry.getKey()) != null) return false;
            spots.put(entry.getKey(), entry.getValue());
            return true;
        });
        WorldGizmos.draw(client, () -> {
            for (Spot spot : spots.values()) {
                Gizmos.cuboid(spot.box(), GizmoStyle.stroke(0xFFFF8855, 2.0F)).setAlwaysOnTop();
                Gizmos.billboardTextOverBlock(spot.name() + " logged out", BlockPos.containing(spot.box().getCenter()), 2, 0xFFFF8855, 0.6F);
            }
        });
    }
}
