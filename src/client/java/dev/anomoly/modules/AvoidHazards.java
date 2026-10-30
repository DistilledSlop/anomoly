package dev.anomoly.modules;

import dev.anomoly.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class AvoidHazards extends Module {
    public AvoidHazards() {
        super("avoid_hazards", "AvoidHazards", "Stop horizontal movement before walking into lava, fire, cactus, magma, berry bushes or cobwebs (also covers Anti Hazard).", Category.MOVEMENT);
    }

    @Override public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        Vec3 velocity = player.getDeltaMovement();
        if (Math.abs(velocity.x) + Math.abs(velocity.z) < 0.01) return;
        Vec3 ahead = player.position().add(velocity.x * 4, 0, velocity.z * 4);
        BlockPos feet = BlockPos.containing(ahead);
        if (hazard(client.level.getBlockState(feet)) || hazard(client.level.getBlockState(feet.above()))
            || hazard(client.level.getBlockState(feet.below()))) {
            player.setDeltaMovement(0, velocity.y, 0);
        }
    }

    private static boolean hazard(BlockState state) {
        return state.is(Blocks.LAVA) || state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE) || state.is(Blocks.CACTUS)
            || state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.COBWEB)
            || state.is(Blocks.CAMPFIRE) || state.is(Blocks.WITHER_ROSE);
    }
}
