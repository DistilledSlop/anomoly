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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Builds and repairs the four blast-resistant blocks beside the player's feet. */
public final class Surround extends Module {
    private static final Direction[] SIDES = {
        Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
    };
    private static final Direction[] SUPPORTS = {
        Direction.DOWN, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP
    };
    private final BooleanSetting centerOnEnable = add(new BooleanSetting("center_on_enable", "Center before placing", true));
    private final BooleanSetting allowCryingObsidian = add(new BooleanSetting("allow_crying_obsidian", "Use crying obsidian too", false));
    private final BooleanSetting useOffhand = add(new BooleanSetting("use_offhand", "Use offhand blocks", true));
    private final BooleanSetting restoreSlot = add(new BooleanSetting("restore_slot", "Restore previous hotbar slot", true));
    private final FloatSetting interval = add(new FloatSetting("interval_ticks", "Ticks between placements", 2F, 1F, 20F));
    private final FloatSetting blocksPerTick = add(new FloatSetting("blocks_per_tick", "Blocks per placement tick", 1F, 1F, 4F));
    private ClientLevel observedLevel;
    private LocalPlayer observedPlayer;
    private int cooldown;
    private int nextSide;

    public Surround() {
        super("surround", "Surround", "Place and repair an obsidian shell beside your feet.", Category.COMBAT);
    }

    @Override public void tick(Minecraft client) {
        if (client.gameMode == null || client.gui.screen() != null || !client.player.isAlive()
            || client.player.isSpectator()
            || client.player.isHandsBusy() || client.player.isUsingItem()
            || client.options.keyUse.isDown() || client.options.keyAttack.isDown()) return;
        LocalPlayer player = client.player;
        if (observedPlayer != player || observedLevel != client.level) {
            if (centerOnEnable.get() && !center(player, client)) return;
            observedPlayer = player;
            observedLevel = client.level;
            cooldown = 0;
            nextSide = 0;
        }
        if (cooldown > 0) { cooldown--; return; }
        BlockPos feet = player.blockPosition();
        boolean missingSide = false;
        for (Direction side : SIDES) if (findSupport(client, player, feet.relative(side)) != null) {
            missingSide = true;
            break;
        }
        if (!missingSide) return;
        int oldSlot = player.getInventory().getSelectedSlot();
        InteractionHand hand = selectHand(player);
        if (hand == null) return;
        int placed = 0;
        try {
            for (int offset = 0; offset < SIDES.length && placed < (int)blocksPerTick.get(); offset++) {
                int index = (nextSide + offset) % SIDES.length;
                BlockPos target = feet.relative(SIDES[index]);
                BlockHitResult hit = findSupport(client, player, target);
                if (hit == null) continue;
                ItemStack block = player.getItemInHand(hand);
                InteractionResult result = client.gameMode.useItemOn(player, hand, hit);
                if (!result.consumesAction()) continue;
                player.swing(hand, block.getInteractAnimation(), false);
                player.itemUsed(hand);
                nextSide = (index + 1) % SIDES.length;
                placed++;
                if (player.getItemInHand(hand).isEmpty()) break;
            }
        } finally {
            if (hand == InteractionHand.MAIN_HAND && restoreSlot.get()
                && player.getInventory().getSelectedSlot() != oldSlot) {
                player.getInventory().setSelectedSlot(oldSlot);
                player.connection.send(new ServerboundSetCarriedItemPacket(oldSlot));
            }
        }
        if (placed > 0) cooldown = (int)interval.get() - 1;
    }

    private InteractionHand selectHand(LocalPlayer player) {
        if (useOffhand.get() && usable(player.getOffhandItem())) return InteractionHand.OFF_HAND;
        if (usable(player.getMainHandItem())) return InteractionHand.MAIN_HAND;
        for (int slot = 0; slot < 9; slot++) {
            if (!usable(player.getInventory().getItem(slot))) continue;
            player.getInventory().setSelectedSlot(slot);
            return InteractionHand.MAIN_HAND;
        }
        return null;
    }

    private boolean usable(ItemStack stack) {
        return stack.is(Items.OBSIDIAN) || allowCryingObsidian.get() && stack.is(Items.CRYING_OBSIDIAN);
    }

    private static BlockHitResult findSupport(Minecraft client, LocalPlayer player, BlockPos target) {
        if (!client.level.getBlockState(target).canBeReplaced()
            || player.getBoundingBox().intersects(new AABB(target))) return null;
        for (Direction direction : SUPPORTS) {
            BlockPos support = target.relative(direction);
            BlockState state = client.level.getBlockState(support);
            Direction face = direction.getOpposite();
            if (!state.isFaceSturdy(client.level, support, face)) continue;
            Vec3 hit = Vec3.atCenterOf(support).add(
                face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
            if (player.getEyePosition().distanceToSqr(hit) > 25.0) continue;
            return new BlockHitResult(hit, face, support, false);
        }
        return null;
    }

    private static boolean center(LocalPlayer player, Minecraft client) {
        BlockPos feet = player.blockPosition();
        double dx = feet.getX() + 0.5 - player.getX();
        double dz = feet.getZ() + 0.5 - player.getZ();
        if (Math.abs(dx) < 0.01 && Math.abs(dz) < 0.01) return true;
        if (!player.onGround() || !client.level.noCollision(player, player.getBoundingBox().move(dx, 0, dz))) return false;
        player.setPos(player.getX() + dx, player.getY(), player.getZ() + dz);
        return true;
    }

    @Override protected void onDisable(Minecraft client) {
        observedLevel = null;
        observedPlayer = null;
        cooldown = 0;
        nextSide = 0;
    }
}
