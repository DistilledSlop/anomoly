package dev.anomoly.modules;

import dev.anomoly.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class SelfFill extends Module {
    private static final double[] JUMP = {0.42, 0.75, 1.0, 1.16};

    public SelfFill() {
        super("self_fill", "SelfFill", "One-shot: fake a jump, place obsidian in the block you stood in, then switch off (also covers Burrow).", Category.COMBAT);
    }

    @Override protected void onEnable(Minecraft client) {
        try {
            var player = client.player;
            if (player == null || client.gameMode == null || !player.onGround()) return;
            int slot = -1;
            for (int i = 0; i < 9; i++) if (player.getInventory().getItem(i).is(Items.OBSIDIAN)) slot = i;
            if (slot < 0) return;
            BlockPos feet = player.blockPosition();
            Vec3 start = player.position();
            for (double dy : JUMP) PacketInterceptor.replay(client, new ServerboundMovePlayerPacket.Pos(start.add(0, dy, 0), false, false));
            int previous = player.getInventory().getSelectedSlot();
            player.getInventory().setSelectedSlot(slot);
            player.connection.send(new ServerboundSetCarriedItemPacket(slot));
            Vec3 click = Vec3.atCenterOf(feet.below()).add(0, 0.5, 0);
            client.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, new BlockHitResult(click, Direction.UP, feet.below(), false));
            player.getInventory().setSelectedSlot(previous);
            player.connection.send(new ServerboundSetCarriedItemPacket(previous));
            player.setPos(start.x, start.y + 1.0, start.z);
        } finally {
            setEnabled(false);
        }
    }
}
