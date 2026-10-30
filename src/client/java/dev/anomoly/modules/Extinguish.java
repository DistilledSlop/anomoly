package dev.anomoly.modules;

import dev.anomoly.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class Extinguish extends Module {
    private BlockPos placed;
    private int previousSlot = -1;

    public Extinguish() {
        super("extinguish", "Extinguish", "Place water at your feet when you catch fire, then scoop it up once the flames are out.", Category.WORLD);
    }

    @Override public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (client.gameMode == null || client.gui.screen() != null) return;
        if (placed != null) {
            if (!player.isOnFire() && client.level.getBlockState(placed).is(Blocks.WATER)) use(client, new BlockHitResult(Vec3.atCenterOf(placed), Direction.UP, placed, false));
            if (!client.level.getBlockState(placed).is(Blocks.WATER) || !player.isOnFire() && player.getMainHandItem().is(Items.WATER_BUCKET)) finish(player);
            return;
        }
        if (!player.isOnFire() || player.isInWater() || player.isInLava() || !player.onGround()) return;
        int slot = -1;
        for (int i = 0; i < 9; i++) if (player.getInventory().getItem(i).is(Items.WATER_BUCKET)) slot = i;
        BlockPos below = player.blockPosition().below();
        if (slot < 0 || !client.level.getBlockState(below).isFaceSturdy(client.level, below, Direction.UP)) return;
        previousSlot = player.getInventory().getSelectedSlot();
        select(player, slot);
        use(client, new BlockHitResult(Vec3.atCenterOf(below).add(0, 0.5, 0), Direction.UP, below, false));
        placed = player.blockPosition();
    }

    private void use(Minecraft client, BlockHitResult hit) {
        client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, hit);
    }

    private void finish(LocalPlayer player) {
        placed = null;
        if (previousSlot >= 0) select(player, previousSlot);
        previousSlot = -1;
    }

    private static void select(LocalPlayer player, int slot) {
        player.getInventory().setSelectedSlot(slot);
        player.connection.send(new ServerboundSetCarriedItemPacket(slot));
    }

    @Override protected void onDisable(Minecraft client) {
        if (client.player != null && previousSlot >= 0) select(client.player, previousSlot);
        placed = null;
        previousSlot = -1;
    }
}
