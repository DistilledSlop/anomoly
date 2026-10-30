package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Add vertical motion to grounded jumps from springy surfaces. */
public final class BlockBounce extends Module {
    private final FloatSetting motion = add(new FloatSetting("motion", "Extra jump motion", 0.42F, 0.2F, 2.0F));

    public BlockBounce() {
        super("block_bounce", "BlockBounce",
            "Jump higher from slime blocks, beds, and honey blocks.", Category.MOVEMENT);
    }

    public void onJump(LocalPlayer player) {
        if (!enabled() || player != Minecraft.getInstance().player || !player.onGround() || !bouncyBelow(player)) return;
        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(velocity.x, velocity.y + motion.get(), velocity.z);
    }

    private static boolean bouncyBelow(LocalPlayer player) {
        AABB box = player.getBoundingBox();
        int y = Mth.floor(box.minY - 0.01);
        for (int x = Mth.floor(box.minX + 1.0E-5); x <= Mth.floor(box.maxX - 1.0E-5); x++) {
            for (int z = Mth.floor(box.minZ + 1.0E-5); z <= Mth.floor(box.maxZ - 1.0E-5); z++) {
                BlockState state = player.level().getBlockState(new BlockPos(x, y, z));
                if (state.is(Blocks.SLIME_BLOCK) || state.getBlock() instanceof BedBlock
                    || state.is(Blocks.HONEY_BLOCK)) return true;
            }
        }
        return false;
    }
}
