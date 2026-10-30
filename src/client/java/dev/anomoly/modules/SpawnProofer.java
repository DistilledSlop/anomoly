package dev.anomoly.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;

public final class SpawnProofer extends BlockUseModule {
    public SpawnProofer() {
        super("spawn_proofer", "Spawn Proofer", "Place torches on dark floors nearby so hostile mobs cannot spawn there.", Category.WORLD);
    }

    @Override protected boolean holds(ItemStack stack) { return stack.is(Items.TORCH) || stack.is(Items.SOUL_TORCH); }

    @Override protected boolean suitable(Minecraft client, BlockPos pos, BlockState state) {
        BlockPos above = pos.above();
        return state.isFaceSturdy(client.level, pos, Direction.UP) && client.level.getBlockState(above).isAir()
            && client.level.getBrightness(LightLayer.BLOCK, above) == 0;
    }
}
