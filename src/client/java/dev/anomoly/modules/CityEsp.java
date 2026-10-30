package dev.anomoly.modules;

import dev.anomoly.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class CityEsp extends Module {
    public CityEsp() {
        super("city_esp", "City Esp", "Outline the minable blocks that wall in nearby players' feet, the ones to break for a city attack.", Category.RENDER);
    }

    @Override public void tick(Minecraft client) {
        WorldGizmos.draw(client, () -> {
            GizmoStyle style = GizmoStyle.stroke(0xFFFF5555, 2.0F);
            for (Player other : client.level.players()) {
                if (other == client.player || client.player.distanceToSqr(other) > 24 * 24) continue;
                BlockPos feet = other.blockPosition();
                for (Direction side : Direction.Plane.HORIZONTAL) {
                    BlockPos pos = feet.relative(side);
                    BlockState state = client.level.getBlockState(pos);
                    if (!state.isAir() && !state.is(Blocks.BEDROCK)) Gizmos.cuboid(pos, style).setAlwaysOnTop();
                }
            }
        });
    }
}
