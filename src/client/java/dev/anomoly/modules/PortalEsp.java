package dev.anomoly.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class PortalEsp extends BlockEspBase {
    public PortalEsp() {
        super("portal_esp", "PortalEsp", "Outline Nether portals and end portal frames and gateways through walls.", Category.RENDER, 0xFFB050FF);
    }

    @Override protected boolean matches(Minecraft client, BlockPos pos, BlockState state) {
        return state.is(Blocks.NETHER_PORTAL) || state.is(Blocks.END_PORTAL) || state.is(Blocks.END_PORTAL_FRAME)
            || state.is(Blocks.END_GATEWAY);
    }
}
