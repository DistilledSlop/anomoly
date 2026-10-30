package dev.anomoly.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class Tillaura extends BlockUseModule {
    public Tillaura() {
        super("tillaura", "Tillaura", "Till nearby dirt and grass into farmland with a hoe from the hotbar.", Category.WORLD);
    }

    @Override protected boolean holds(ItemStack stack) { return stack.is(ItemTags.HOES); }

    @Override protected boolean suitable(Minecraft client, BlockPos pos, BlockState state) {
        return (state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT_PATH))
            && client.level.getBlockState(pos.above()).isAir();
    }
}
