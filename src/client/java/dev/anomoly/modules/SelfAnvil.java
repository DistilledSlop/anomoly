package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.FloatSetting;
import dev.anomoly.setting.StringSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Builds a short support and drops an anvil into the player's own hole. */
public final class SelfAnvil extends Module {
    private static final Direction[] SIDES = {
        Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
    };
    private static final Direction[] SUPPORTS = {
        Direction.DOWN, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP
    };
    private final StringSetting supportBlock = add(new StringSetting("support_block", "Support block ID", "minecraft:obsidian"));
    private final BooleanSetting requireHole = add(new BooleanSetting("require_hole", "Only in a one-block hole", true));
    private final BooleanSetting useOffhand = add(new BooleanSetting("use_offhand", "Use offhand items", true));
    private final BooleanSetting restoreSlot = add(new BooleanSetting("restore_slot", "Restore previous hotbar slot", true));
    private final BooleanSetting lineOfSight = add(new BooleanSetting("line_of_sight", "Require line of sight", true));
    private final FloatSetting minHealth = add(new FloatSetting("min_health", "Minimum health", 10F, 1F, 20F));
    private final FloatSetting placeRange = add(new FloatSetting("place_range", "Place range", 4.5F, 2F, 6F));
    private final FloatSetting dropHeight = add(new FloatSetting("drop_height", "Drop height above feet", 2F, 2F, 4F));
    private final FloatSetting interval = add(new FloatSetting("interval_ticks", "Ticks between placements", 2F, 1F, 20F));
    private ClientLevel observedLevel;
    private LocalPlayer observedPlayer;
    private int cooldown;

    public SelfAnvil() {
        super("self_anvil", "Self Anvil", "Drop an anvil into your hole; the fall can hurt you.", Category.COMBAT);
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
        if (player.getHealth() <= minHealth.get() || !hasAnvil(player)) return;
        BlockPos feet = player.blockPosition();
        if (requireHole.get() && !inHole(client, feet)) return;
        Item scaffold = configuredSupportItem();
        Action action = nextAction(client, player, feet, scaffold);
        if (action == null) return;
        int oldSlot = player.getInventory().getSelectedSlot();
        InteractionHand hand = action.anvil() ? selectAnvil(player) : selectItem(player, scaffold);
        if (hand == null) return;
        try {
            ItemStack stack = player.getItemInHand(hand);
            InteractionResult result = client.gameMode.useItemOn(player, hand, action.hit());
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

    private static boolean inHole(Minecraft client, BlockPos feet) {
        BlockPos floor = feet.below();
        if (!client.level.getBlockState(floor).isFaceSturdy(client.level, floor, Direction.UP)) return false;
        for (Direction side : SIDES) {
            BlockPos wall = feet.relative(side);
            if (!client.level.getBlockState(wall).isFaceSturdy(client.level, wall, side.getOpposite())) return false;
        }
        return true;
    }

    private Action nextAction(Minecraft client, LocalPlayer player, BlockPos feet, Item scaffold) {
        int height = (int)dropHeight.get();
        BlockPos drop = feet.above(height);
        if (!client.level.isEmptyBlock(drop) || !clearDropColumn(client, feet, height)
            || activeAnvil(client, feet, height)) return null;
        BlockHitResult direct = support(client, player, drop);
        if (direct != null) return new Action(direct, true);
        if (scaffold == null || !hasItem(player, scaffold)) return null;
        BlockHitResult best = null;
        int bestStage = -1;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (Direction side : SIDES) {
            BlockPos base = feet.relative(side);
            if (!canReachFutureDropFace(player, base, height, side)) continue;
            for (int stage = 0; stage <= height; stage++) {
                BlockPos block = base.above(stage);
                if (!client.level.getBlockState(block).canBeReplaced()) continue;
                if (!client.level.getEntities(null, new AABB(block)).isEmpty()) break;
                BlockHitResult hit = support(client, player, block);
                if (hit != null) {
                    double distance = player.getEyePosition().distanceToSqr(hit.getLocation());
                    if (stage > bestStage || stage == bestStage && distance < bestDistance) {
                        bestStage = stage;
                        bestDistance = distance;
                        best = hit;
                    }
                }
                break;
            }
        }
        return best == null ? null : new Action(best, false);
    }

    private boolean canReachFutureDropFace(LocalPlayer player, BlockPos base, int height, Direction side) {
        BlockPos top = base.above(height);
        Direction face = side.getOpposite();
        Vec3 hit = Vec3.atCenterOf(top).add(
            face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
        Vec3 eyes = player.getEyePosition();
        if (eyes.distanceToSqr(hit) > placeRange.get() * placeRange.get()) return false;
        if (lineOfSight.get()) {
            for (int stage = 0; stage < height; stage++)
                if (new AABB(base.above(stage)).clip(eyes, hit).isPresent()) return false;
        }
        return true;
    }

    private static boolean clearDropColumn(Minecraft client, BlockPos feet, int height) {
        for (int step = 1; step < height; step++)
            if (!client.level.isEmptyBlock(feet.above(step))) return false;
        return client.level.isEmptyBlock(feet);
    }

    private static boolean activeAnvil(Minecraft client, BlockPos feet, int height) {
        for (var entity : client.level.entitiesForRendering())
            if (entity instanceof FallingBlockEntity falling && falling.getBlockState().is(BlockTags.ANVIL)
                && Math.abs(falling.getX() - feet.getX() - 0.5) < 0.7
                && Math.abs(falling.getZ() - feet.getZ() - 0.5) < 0.7
                && falling.getY() >= feet.getY() && falling.getY() <= feet.getY() + height + 1) return true;
        return false;
    }

    private BlockHitResult support(Minecraft client, LocalPlayer player, BlockPos target) {
        Vec3 eyes = player.getEyePosition();
        for (Direction direction : SUPPORTS) {
            BlockPos block = target.relative(direction);
            Direction face = direction.getOpposite();
            BlockState state = client.level.getBlockState(block);
            if (!state.isFaceSturdy(client.level, block, face) || client.level.getBlockEntity(block) != null) continue;
            Vec3 hit = Vec3.atCenterOf(block).add(
                face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
            if (eyes.distanceToSqr(hit) > placeRange.get() * placeRange.get()) continue;
            if (lineOfSight.get()) {
                Vec3 inside = Vec3.atCenterOf(block).add(
                    face.getStepX() * 0.49, face.getStepY() * 0.49, face.getStepZ() * 0.49);
                HitResult ray = client.level.clip(new ClipContext(eyes, inside,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
                if (!(ray instanceof BlockHitResult seen) || !seen.getBlockPos().equals(block)) continue;
            }
            return new BlockHitResult(hit, face, block, false);
        }
        return null;
    }

    private Item configuredSupportItem() {
        Identifier id = Identifier.tryParse(supportBlock.get().trim());
        Item item = id == null ? null : BuiltInRegistries.ITEM.getValue(id);
        return item instanceof BlockItem ? item : null;
    }

    private boolean hasItem(LocalPlayer player, Item item) {
        if (useOffhand.get() && player.getOffhandItem().is(item)) return true;
        for (int slot = 0; slot < 9; slot++)
            if (player.getInventory().getItem(slot).is(item)) return true;
        return false;
    }

    private InteractionHand selectItem(LocalPlayer player, Item item) {
        if (useOffhand.get() && player.getOffhandItem().is(item)) return InteractionHand.OFF_HAND;
        if (player.getMainHandItem().is(item)) return InteractionHand.MAIN_HAND;
        for (int slot = 0; slot < 9; slot++) if (player.getInventory().getItem(slot).is(item)) {
            player.getInventory().setSelectedSlot(slot);
            return InteractionHand.MAIN_HAND;
        }
        return null;
    }

    private static boolean isAnvil(ItemStack stack) {
        return stack.is(Items.ANVIL) || stack.is(Items.CHIPPED_ANVIL) || stack.is(Items.DAMAGED_ANVIL);
    }

    private boolean hasAnvil(LocalPlayer player) {
        if (useOffhand.get() && isAnvil(player.getOffhandItem())) return true;
        for (int slot = 0; slot < 9; slot++)
            if (isAnvil(player.getInventory().getItem(slot))) return true;
        return false;
    }

    private InteractionHand selectAnvil(LocalPlayer player) {
        if (useOffhand.get() && isAnvil(player.getOffhandItem())) return InteractionHand.OFF_HAND;
        if (isAnvil(player.getMainHandItem())) return InteractionHand.MAIN_HAND;
        for (int slot = 0; slot < 9; slot++) if (isAnvil(player.getInventory().getItem(slot))) {
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

    private record Action(BlockHitResult hit, boolean anvil) { }
}
