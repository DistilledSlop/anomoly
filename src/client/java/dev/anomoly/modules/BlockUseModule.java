package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Uses a particular hotbar item on the nearest suitable block in reach, one block per interval. */
abstract class BlockUseModule extends Module {
    private final FloatSetting range = add(new FloatSetting("range", "Range", 4.0F, 1.5F, 6.0F));
    private final FloatSetting interval = add(new FloatSetting("interval_ticks", "Ticks between uses", 4.0F, 1.0F, 40.0F));
    private int cooldown;

    protected BlockUseModule(String id, String name, String description, Category category) {
        super(id, name, description, category);
    }

    protected abstract boolean holds(ItemStack stack);

    protected abstract boolean suitable(Minecraft client, BlockPos pos, BlockState state);

    protected Direction face() { return Direction.UP; }

    @Override public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (client.gameMode == null || client.gui.screen() != null || player.isUsingItem() || --cooldown > 0) return;
        int slot = -1;
        for (int i = 0; i < 9 && slot < 0; i++) if (holds(player.getInventory().getItem(i))) slot = i;
        if (slot < 0) return;
        BlockPos target = nearest(client);
        if (target == null) return;
        int previous = player.getInventory().getSelectedSlot();
        if (slot != previous) {
            player.getInventory().setSelectedSlot(slot);
            player.connection.send(new ServerboundSetCarriedItemPacket(slot));
        }
        Direction face = face();
        Vec3 click = Vec3.atCenterOf(target).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
        client.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, new BlockHitResult(click, face, target, false));
        player.swing(InteractionHand.MAIN_HAND, player.getMainHandItem().getInteractAnimation(), false);
        if (slot != previous) {
            player.getInventory().setSelectedSlot(previous);
            player.connection.send(new ServerboundSetCarriedItemPacket(previous));
        }
        cooldown = (int)interval.get();
    }

    private BlockPos nearest(Minecraft client) {
        LocalPlayer player = client.player;
        Vec3 eyes = player.getEyePosition();
        int reach = (int)Math.ceil(range.get());
        BlockPos origin = player.blockPosition();
        BlockPos best = null;
        double bestDistance = range.get() * range.get();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-reach, -reach, -reach), origin.offset(reach, reach, reach))) {
            double distance = Vec3.atCenterOf(pos).distanceToSqr(eyes);
            if (distance > bestDistance || !suitable(client, pos, client.level.getBlockState(pos))) continue;
            best = pos.immutable();
            bestDistance = distance;
        }
        return best;
    }
}
