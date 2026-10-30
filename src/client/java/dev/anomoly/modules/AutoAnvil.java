package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.FloatSetting;
import dev.anomoly.setting.StringSetting;
import java.util.Comparator;
import java.util.List;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
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

/** Builds a side scaffold and drops an anvil onto a nearby target. */
public final class AutoAnvil extends Module {
    private static final Direction[] SIDES = {
        Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
    };
    private static final Direction[] SUPPORTS = {
        Direction.DOWN, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP
    };
    private final StringSetting supportBlock = add(new StringSetting("support_block", "Support block ID", "minecraft:obsidian"));
    private final BooleanSetting requireHelmet = add(new BooleanSetting("require_helmet", "Require target helmet", true));
    private final BooleanSetting targetMobs = add(new BooleanSetting("target_mobs", "Target mobs", false));
    private final BooleanSetting useOffhand = add(new BooleanSetting("use_offhand", "Use offhand items", true));
    private final BooleanSetting restoreSlot = add(new BooleanSetting("restore_slot", "Restore previous hotbar slot", true));
    private final BooleanSetting lineOfSight = add(new BooleanSetting("line_of_sight", "Require line of sight", true));
    private final FloatSetting targetRange = add(new FloatSetting("target_range", "Target range", 6F, 2F, 10F));
    private final FloatSetting placeRange = add(new FloatSetting("place_range", "Place range", 4.5F, 2F, 6F));
    private final FloatSetting dropHeight = add(new FloatSetting("drop_height", "Drop height above target feet", 4F, 3F, 6F));
    private final FloatSetting interval = add(new FloatSetting("interval_ticks", "Ticks between placements", 2F, 1F, 20F));
    private ClientLevel observedLevel;
    private LocalPlayer observedPlayer;
    private int cooldown;

    public AutoAnvil() {
        super("auto_anvil", "Auto Anvil", "Build a scaffold and drop an anvil onto a helmeted opponent.", Category.COMBAT);
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
        if (!hasAnvil(player)) return;
        Item scaffold = configuredSupportItem();
        List<LivingEntity> targets = CrystalCombat.targets(client, targetRange.get(), targetMobs.get());
        targets.sort(Comparator.comparingDouble(player::distanceToSqr));
        Action action = null;
        for (LivingEntity target : targets) {
            if (requireHelmet.get() && target.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) continue;
            action = nextAction(client, target, scaffold);
            if (action != null) break;
        }
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

    private Action nextAction(Minecraft client, LivingEntity target, Item scaffold) {
        BlockPos feet = target.blockPosition();
        int height = (int)dropHeight.get();
        BlockPos drop = feet.above(height);
        if (!client.level.isEmptyBlock(drop) || !clearDropColumn(client, feet, height)
            || activeAnvil(client, feet, height)) return null;
        BlockHitResult direct = support(client, client.player, drop);
        if (direct != null) return new Action(direct, true);
        if (scaffold == null || !hasItem(client.player, scaffold)) return null;
        BlockHitResult best = null;
        int bestStage = -1;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (Direction side : SIDES) {
            BlockPos base = feet.relative(side);
            if (!canReachFutureDropFace(client.player, base, height, side)) continue;
            for (int stage = 0; stage <= height; stage++) {
                BlockPos block = base.above(stage);
                if (!client.level.getBlockState(block).canBeReplaced()) continue;
                if (!client.level.getEntities(null, new AABB(block)).isEmpty()) break;
                BlockHitResult hit = support(client, client.player, block);
                if (hit != null) {
                    double distance = client.player.getEyePosition().distanceToSqr(hit.getLocation());
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
        for (int step = 2; step < height; step++)
            if (!client.level.isEmptyBlock(feet.above(step))) return false;
        for (int step = 0; step < height; step++)
            if (client.level.getBlockState(feet.above(step)).is(BlockTags.ANVIL)) return false;
        return true;
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
