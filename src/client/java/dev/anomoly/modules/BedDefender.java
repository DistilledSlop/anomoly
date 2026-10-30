package dev.anomoly.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class BedDefender extends BlockUseModule {
    public BedDefender() {
        super("bed_defender", "BedDefender", "Place hotbar blocks on top of nearby beds so their tops are covered.", Category.WORLD);
    }

    @Override protected boolean holds(ItemStack stack) {
        return stack.getItem() instanceof BlockItem item && item.getBlock().defaultBlockState().isSolidRender();
    }

    @Override protected boolean suitable(Minecraft client, BlockPos pos, BlockState state) {
        return state.getBlock() instanceof BedBlock && client.level.getBlockState(pos.above()).isAir();
    }
}
