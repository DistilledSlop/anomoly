package dev.anomoly.modules;

import dev.anomoly.Module;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.properties.BedPart;

public final class BedPlates extends Module {
    private record Plate(BlockPos pos, String text) { }

    private final List<Plate> plates = new ArrayList<>();
    private int ticks;

    public BedPlates() {
        super("bed_plates", "BedPlates", "Label nearby beds with how many of their open sides are covered by blocks.", Category.RENDER);
    }

    @Override protected void onEnable(Minecraft client) { plates.clear(); ticks = 0; }

    @Override public void tick(Minecraft client) {
        if (ticks++ % 20 == 0) scan(client);
        WorldGizmos.draw(client, () -> {
            for (Plate plate : plates) Gizmos.billboardTextOverBlock(plate.text(), plate.pos(), 1, 0xFFFF5577, 0.7F).setAlwaysOnTop();
        });
    }

    private void scan(Minecraft client) {
        plates.clear();
        BlockPos origin = client.player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-24, -12, -24), origin.offset(24, 12, 24))) {
            var state = client.level.getBlockState(pos);
            if (!(state.getBlock() instanceof BedBlock) || state.getValue(BedBlock.PART) != BedPart.HEAD) continue;
            int covered = 0;
            for (Direction side : Direction.values()) {
                if (side == Direction.DOWN) continue;
                BlockPos next = pos.relative(side);
                var neighbor = client.level.getBlockState(next);
                if (neighbor.getBlock() instanceof BedBlock) continue;
                if (!neighbor.isAir()) covered++;
            }
            plates.add(new Plate(pos.immutable(), covered + " covered sides"));
        }
    }
}
