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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Builds a short support beside the player, then caps the space above their head. */
public class SelfTrap extends Module {
    private static final Direction[] SIDES = {
        Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
    };
    private static final Direction[] SUPPORTS = {
        Direction.DOWN, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP
    };
    private final StringSetting blockId = add(new StringSetting("block_id", "Block ID", "minecraft:obsidian"));
    private final BooleanSetting useOffhand = add(new BooleanSetting("use_offhand", "Use offhand block", true));
    private final BooleanSetting restoreSlot = add(new BooleanSetting("restore_slot", "Restore previous hotbar slot", true));
    private final BooleanSetting lineOfSight = add(new BooleanSetting("line_of_sight", "Require line of sight", true));
    private final FloatSetting placeRange = add(new FloatSetting("place_range", "Place range", 4.5F, 2F, 6F));
    private final FloatSetting interval = add(new FloatSetting("interval_ticks", "Ticks between placements", 2F, 1F, 20F));
    private ClientLevel observedLevel;
    private LocalPlayer observedPlayer;
    private int cooldown;

    public SelfTrap() {
        super("self_trap", "Self Trap", "Build an overhead block and repair it when removed.", Category.COMBAT);
    }

    protected SelfTrap(String id, String name, String description) {
        super(id, name, description, Category.COMBAT);
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
        Item item = configuredItem();
        if (item == null || !hasItem(player, item)) return;
        BlockHitResult hit = nextPlacement(client, player);
        if (hit == null) return;
        int oldSlot = player.getInventory().getSelectedSlot();
        InteractionHand hand = selectHand(player, item);
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

    private BlockHitResult nextPlacement(Minecraft client, LocalPlayer player) {
        BlockPos feet = player.blockPosition();
        BlockPos roof = feet.above(2);
        if (!placeable(client, roof)) return null;
        BlockHitResult direct = support(client, player, roof);
        if (direct != null) return direct;
        BlockHitResult best = null;
        int bestStage = -1;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (Direction side : SIDES) {
            BlockPos base = feet.relative(side);
            for (int stage = 0; stage < 3; stage++) {
                BlockPos target = base.above(stage);
                if (!client.level.getBlockState(target).canBeReplaced()) continue;
                if (!placeable(client, target)) break;
                BlockHitResult hit = support(client, player, target);
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
        return best;
    }

    private static boolean placeable(Minecraft client, BlockPos target) {
        return client.level.getBlockState(target).canBeReplaced()
            && client.level.getEntities(null, new AABB(target)).isEmpty();
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

    private Item configuredItem() {
        Identifier id = Identifier.tryParse(blockId.get().trim());
        Item item = id == null ? null : BuiltInRegistries.ITEM.getValue(id);
        return item instanceof BlockItem ? item : null;
    }

    private boolean hasItem(LocalPlayer player, Item item) {
        if (useOffhand.get() && player.getOffhandItem().is(item)) return true;
        for (int slot = 0; slot < 9; slot++)
            if (player.getInventory().getItem(slot).is(item)) return true;
        return false;
    }

    private InteractionHand selectHand(LocalPlayer player, Item item) {
        if (useOffhand.get() && player.getOffhandItem().is(item)) return InteractionHand.OFF_HAND;
        if (player.getMainHandItem().is(item)) return InteractionHand.MAIN_HAND;
        for (int slot = 0; slot < 9; slot++) if (player.getInventory().getItem(slot).is(item)) {
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
