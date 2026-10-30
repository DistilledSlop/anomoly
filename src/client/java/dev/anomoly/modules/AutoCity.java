package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class AutoCity extends Module {
    private final FloatSetting range = add(new FloatSetting("range", "Range", 4.5F, 2.0F, 6.0F));
    private BlockPos mining;

    public AutoCity() {
        super("auto_city", "Auto City", "Mine the block beside the nearest player's feet that protects them from crystals.", Category.COMBAT);
    }

    @Override public void tick(Minecraft client) {
        var player = client.player;
        if (client.gameMode == null || client.gui.screen() != null) return;
        if (mining != null && (client.level.getBlockState(mining).isAir() || Vec3.atCenterOf(mining).distanceToSqr(player.getEyePosition()) > range.get() * range.get())) {
            client.gameMode.stopDestroyBlock();
            mining = null;
        }
        if (mining == null) {
            Player target = null;
            for (Player other : client.level.players()) {
                if (other != player && player.distanceToSqr(other) < 36 && (target == null || player.distanceToSqr(other) < player.distanceToSqr(target))) target = other;
            }
            if (target == null) return;
            for (Direction side : Direction.Plane.HORIZONTAL) {
                BlockPos pos = target.blockPosition().relative(side);
                var state = client.level.getBlockState(pos);
                if (state.isAir() || state.is(Blocks.BEDROCK) || state.getDestroySpeed(client.level, pos) < 0
                    || Vec3.atCenterOf(pos).distanceToSqr(player.getEyePosition()) > range.get() * range.get()) continue;
                mining = pos;
                client.gameMode.startDestroyBlock(pos, side.getOpposite());
                break;
            }
            return;
        }
        client.gameMode.continueDestroyBlock(mining, Direction.UP);
        player.swing(InteractionHand.MAIN_HAND, player.getMainHandItem().getAttackAnimation(), false);
    }

    @Override protected void onDisable(Minecraft client) {
        if (client.gameMode != null && mining != null) client.gameMode.stopDestroyBlock();
        mining = null;
    }
}
