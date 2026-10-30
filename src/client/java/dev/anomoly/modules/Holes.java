package dev.anomoly.modules;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Detection of one-block holes whose four sides and floor resist crystal explosions. */
final class Holes {
    private Holes() { }

    static boolean safeHole(ClientLevel level, BlockPos pos) {
        if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir() || !level.getBlockState(pos.above(2)).isAir()) return false;
        if (!resistant(level.getBlockState(pos.below()))) return false;
        for (Direction side : Direction.Plane.HORIZONTAL) if (!resistant(level.getBlockState(pos.relative(side)))) return false;
        return true;
    }

    private static boolean resistant(BlockState state) {
        return state.is(Blocks.BEDROCK) || state.is(Blocks.OBSIDIAN) || state.is(Blocks.CRYING_OBSIDIAN)
            || state.is(Blocks.RESPAWN_ANCHOR) || state.is(Blocks.NETHERITE_BLOCK) || state.is(Blocks.ENDER_CHEST);
    }

    static BlockPos nearest(ClientLevel level, BlockPos origin, int horizontal, int vertical) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-horizontal, -vertical, -horizontal), origin.offset(horizontal, vertical, horizontal))) {
            if (!safeHole(level, pos)) continue;
            double distance = pos.distSqr(origin);
            if (distance < bestDistance) {
                best = pos.immutable();
                bestDistance = distance;
            }
        }
        return best;
    }
}
