package dev.anomoly.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class LiquidFiller extends BlockUseModule {
    public LiquidFiller() {
        super("liquid_filler", "LiquidFiller", "Place hotbar blocks into nearby source water and lava to cover them.", Category.WORLD);
    }

    @Override protected boolean holds(ItemStack stack) {
        return stack.getItem() instanceof BlockItem item && item.getBlock().defaultBlockState().isSolidRender()
            && !(item.getBlock() == Blocks.TNT || item.getBlock() == Blocks.SAND || item.getBlock() == Blocks.GRAVEL);
    }

    @Override protected boolean suitable(Minecraft client, BlockPos pos, BlockState state) {
        return state.getFluidState().isSource() && state.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock;
    }
}
