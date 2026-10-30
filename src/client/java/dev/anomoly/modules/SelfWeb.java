package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Places a cobweb at the player's feet when the cell is clear. */
public final class SelfWeb extends Module {
    private final BooleanSetting useOffhand = add(new BooleanSetting("use_offhand", "Use offhand cobweb", true));
    private final BooleanSetting restoreSlot = add(new BooleanSetting("restore_slot", "Restore previous hotbar slot", true));
    private final BooleanSetting lineOfSight = add(new BooleanSetting("line_of_sight", "Require line of sight", true));
    private final FloatSetting placeRange = add(new FloatSetting("place_range", "Place range", 4.5F, 2F, 6F));
    private final FloatSetting interval = add(new FloatSetting("interval_ticks", "Ticks between attempts", 3F, 1F, 20F));
    private ClientLevel observedLevel;
    private LocalPlayer observedPlayer;
    private int cooldown;

    public SelfWeb() {
        super("self_web", "Self Web", "Place a cobweb at your feet and replace it when removed.", Category.COMBAT);
    }

    @Override public void tick(Minecraft client) {
        if (client.gameMode == null || client.gui.screen() != null || !client.player.isAlive()
            || client.player.isSpectator() || client.player.isHandsBusy() || client.player.isUsingItem()
            || client.options.keyUse.isDown() || client.options.keyAttack.isDown()
            || SoupUse.busy(client)) return;
        LocalPlayer player = client.player;
        if (observedPlayer != player || observedLevel != client.level) {
            observedPlayer = player;
            observedLevel = client.level;
            cooldown = 0;
        }
        if (cooldown > 0) { cooldown--; return; }
        if (!hasWeb(player)) return;
        BlockHitResult hit = placement(client, player);
        if (hit == null) return;
        int oldSlot = player.getInventory().getSelectedSlot();
        InteractionHand hand = selectHand(player);
        if (hand == null) return;
        try {
            ItemStack stack = player.getItemInHand(hand);
            InteractionResult result = client.gameMode.useItemOn(player, hand, hit);
            if (result.consumesAction()) {
                player.swing(hand, stack.getInteractAnimation(), false);
                player.itemUsed(hand);
            }
            cooldown = (int)interval.get();
        } finally {
            if (hand == InteractionHand.MAIN_HAND && restoreSlot.get()
                && player.getInventory().getSelectedSlot() != oldSlot) {
                player.getInventory().setSelectedSlot(oldSlot);
                player.connection.send(new ServerboundSetCarriedItemPacket(oldSlot));
            }
        }
    }

    private BlockHitResult placement(Minecraft client, LocalPlayer player) {
        BlockPos feet = player.blockPosition();
        if (!client.level.getBlockState(feet).canBeReplaced()) return null;
        BlockPos floor = feet.below();
        BlockState state = client.level.getBlockState(floor);
        if (!state.isFaceSturdy(client.level, floor, Direction.UP)
            || client.level.getBlockEntity(floor) != null) return null;
        Vec3 eyes = player.getEyePosition();
        Vec3 hit = new Vec3(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
        if (eyes.distanceToSqr(hit) > placeRange.get() * placeRange.get()) return null;
        if (lineOfSight.get()) {
            HitResult ray = client.level.clip(new ClipContext(eyes, hit.add(0, -0.01, 0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (!(ray instanceof BlockHitResult seen) || !seen.getBlockPos().equals(floor)) return null;
        }
        return new BlockHitResult(hit, Direction.UP, floor, false);
    }

    private boolean hasWeb(LocalPlayer player) {
        if (useOffhand.get() && player.getOffhandItem().is(Items.COBWEB)) return true;
        for (int slot = 0; slot < 9; slot++)
            if (player.getInventory().getItem(slot).is(Items.COBWEB)) return true;
        return false;
    }

    private InteractionHand selectHand(LocalPlayer player) {
        if (useOffhand.get() && player.getOffhandItem().is(Items.COBWEB)) return InteractionHand.OFF_HAND;
        if (player.getMainHandItem().is(Items.COBWEB)) return InteractionHand.MAIN_HAND;
        for (int slot = 0; slot < 9; slot++) if (player.getInventory().getItem(slot).is(Items.COBWEB)) {
            player.getInventory().setSelectedSlot(slot);
            return InteractionHand.MAIN_HAND;
        }
        return null;
    }

    @Override protected void onDisable(Minecraft client) {
        observedLevel = null;
        observedPlayer = null;
        cooldown = 0;
    }
}
