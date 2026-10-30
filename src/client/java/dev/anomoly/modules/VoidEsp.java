package dev.anomoly.modules;

import dev.anomoly.Module;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.Vec3;

public final class VoidEsp extends Module {
    private final List<BlockPos> holes = new ArrayList<>();
    private int ticks;

    public VoidEsp() {
        super("void_esp", "VoidESP", "Mark nearby columns that have no solid block anywhere below, where falling means the void.", Category.RENDER);
    }

    @Override protected void onEnable(Minecraft client) { holes.clear(); ticks = 0; }

    @Override public void tick(Minecraft client) {
        if (ticks++ % 20 == 0) scan(client);
        WorldGizmos.draw(client, () -> {
            GizmoStyle style = GizmoStyle.stroke(0xFFAA00FF, 2.0F);
            for (BlockPos hole : holes) {
                Vec3 base = new Vec3(hole.getX(), client.player.getY() - 1.0 + 0.02, hole.getZ());
                Gizmos.rect(base, base.add(1, 0, 1), Direction.UP, style);
            }
        });
    }

    private void scan(Minecraft client) {
        holes.clear();
        BlockPos origin = client.player.blockPosition();
        int minY = client.level.getMinY();
        for (int dx = -10; dx <= 10; dx++) {
            for (int dz = -10; dz <= 10; dz++) {
                boolean solid = false;
                for (int y = origin.getY(); y >= minY && !solid; y--) {
                    solid = !client.level.getBlockState(new BlockPos(origin.getX() + dx, y, origin.getZ() + dz)).isAir();
                }
                if (!solid && holes.size() < 400) holes.add(new BlockPos(origin.getX() + dx, origin.getY(), origin.getZ() + dz));
            }
        }
    }
}
