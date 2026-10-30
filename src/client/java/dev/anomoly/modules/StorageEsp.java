package dev.anomoly.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class StorageEsp extends BlockEspBase {
    public StorageEsp() {
        super("storage_esp", "StorageESP", "Outline chests, barrels, ender chests and shulker boxes through walls.", Category.RENDER, 0xFFFFAA00);
    }

    @Override protected boolean matches(Minecraft client, BlockPos pos, BlockState state) {
        var block = state.getBlock();
        return block instanceof ChestBlock || block instanceof BarrelBlock || block instanceof EnderChestBlock
            || block instanceof ShulkerBoxBlock;
    }
}
