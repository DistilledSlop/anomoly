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

/** Places string in bed-placement cells around the player's feet. */
public final class AntiBed extends Module {
    private static final int[][] OFFSETS = {
        {0, 0}, {0, -1}, {1, 0}, {0, 1}, {-1, 0},
        {1, -1}, {1, 1}, {-1, 1}, {-1, -1}
    };
    private final BooleanSetting diagonals = add(new BooleanSetting("diagonals", "Protect diagonal cells", false));
    private final BooleanSetting useOffhand = add(new BooleanSetting("use_offhand", "Use offhand string", true));
    private final BooleanSetting restoreSlot = add(new BooleanSetting("restore_slot", "Restore previous hotbar slot", true));
    private final BooleanSetting lineOfSight = add(new BooleanSetting("line_of_sight", "Require line of sight", true));
    private final FloatSetting placeRange = add(new FloatSetting("place_range", "Place range", 4.5F, 2F, 6F));
    private final FloatSetting interval = add(new FloatSetting("interval_ticks", "Ticks between placements", 3F, 1F, 20F));
    private ClientLevel observedLevel;
    private LocalPlayer observedPlayer;
    private int cooldown;

    public AntiBed() {
        super("anti_bed", "Anti Bed", "Place string around your feet to deny nearby bed placement.", Category.COMBAT);
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
        if (!hasString(player)) return;
        BlockHitResult hit = nextCell(client, player);
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

    private BlockHitResult nextCell(Minecraft client, LocalPlayer player) {
        BlockPos feet = player.blockPosition();
        Vec3 eyes = player.getEyePosition();
        int limit = diagonals.get() ? OFFSETS.length : 5;
        for (int index = 0; index < limit; index++) {
            BlockPos cell = feet.offset(OFFSETS[index][0], 0, OFFSETS[index][1]);
            if (!client.level.isEmptyBlock(cell)) continue;
            BlockPos floor = cell.below();
            BlockState state = client.level.getBlockState(floor);
            if (!state.isFaceSturdy(client.level, floor, Direction.UP)
                || client.level.getBlockEntity(floor) != null) continue;
            Vec3 hit = new Vec3(cell.getX() + 0.5, cell.getY(), cell.getZ() + 0.5);
            if (eyes.distanceToSqr(hit) > placeRange.get() * placeRange.get()) continue;
            if (lineOfSight.get()) {
                HitResult ray = client.level.clip(new ClipContext(eyes, hit.add(0, -0.01, 0),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
                if (!(ray instanceof BlockHitResult seen) || !seen.getBlockPos().equals(floor)) continue;
            }
            return new BlockHitResult(hit, Direction.UP, floor, false);
        }
        return null;
    }

    private boolean hasString(LocalPlayer player) {
        if (useOffhand.get() && player.getOffhandItem().is(Items.STRING)) return true;
        for (int slot = 0; slot < 9; slot++)
            if (player.getInventory().getItem(slot).is(Items.STRING)) return true;
        return false;
    }

    private InteractionHand selectHand(LocalPlayer player) {
        if (useOffhand.get() && player.getOffhandItem().is(Items.STRING)) return InteractionHand.OFF_HAND;
        if (player.getMainHandItem().is(Items.STRING)) return InteractionHand.MAIN_HAND;
        for (int slot = 0; slot < 9; slot++) if (player.getInventory().getItem(slot).is(Items.STRING)) {
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
