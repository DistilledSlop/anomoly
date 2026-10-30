package dev.anomoly.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class BarrierEsp extends BlockEspBase {
    public BarrierEsp() {
        super("barrier_esp", "BarrierEsp", "Outline invisible barrier, light and structure-void blocks.", Category.RENDER, 0xFFFF4040);
    }

    @Override protected boolean matches(Minecraft client, BlockPos pos, BlockState state) {
        return state.is(Blocks.BARRIER) || state.is(Blocks.LIGHT) || state.is(Blocks.STRUCTURE_VOID);
    }
}
