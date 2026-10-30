package dev.anomoly.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class BonemealAura extends BlockUseModule {
    public BonemealAura() {
        super("bonemeal_aura", "BonemealAura", "Apply bone meal from the hotbar to nearby plants that can still grow.", Category.WORLD);
    }

    @Override protected boolean holds(ItemStack stack) { return stack.is(Items.BONE_MEAL); }

    @Override protected boolean suitable(Minecraft client, BlockPos pos, BlockState state) {
        return state.getBlock() instanceof BonemealableBlock plant && plant.isValidBonemealTarget(client.level, pos, state, net.minecraft.world.level.block.BonemealSource.INTERACTION);
    }
}
