package dev.anomoly.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class HoleEsp extends BlockEspBase {
    public HoleEsp() {
        super("hole_esp", "HoleESP", "Outline one-block holes ringed by bedrock or obsidian.", Category.RENDER, 0xFF55FF55);
    }

    @Override protected boolean matches(Minecraft client, BlockPos pos, BlockState state) {
        return false;
    }

    @Override protected boolean includeAir() { return true; }

    @Override protected boolean matchesAir(Minecraft client, BlockPos pos) { return Holes.safeHole(client.level, pos); }
}
