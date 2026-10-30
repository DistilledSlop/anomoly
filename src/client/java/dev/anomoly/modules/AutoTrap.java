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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Builds an obsidian box or applies a web, fire, or lava trap to a nearby opponent. */
public final class AutoTrap extends Module {
    private static final Direction[] SIDES = {
        Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
    };
    private static final Direction[] SUPPORTS = {
        Direction.DOWN, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP
    };
    private final StringSetting mode = add(new StringSetting("mode", "Mode: box, web, ignite, lava", "box"));
    private final StringSetting boxBlock = add(new StringSetting("box_block", "Box block ID", "minecraft:obsidian"));
    private final BooleanSetting targetMobs = add(new BooleanSetting("target_mobs", "Target mobs", false));
    private final BooleanSetting useOffhand = add(new BooleanSetting("use_offhand", "Use offhand item", true));
    private final BooleanSetting restoreSlot = add(new BooleanSetting("restore_slot", "Restore previous hotbar slot", true));
    private final BooleanSetting lineOfSight = add(new BooleanSetting("line_of_sight", "Require line of sight", true));
    private final FloatSetting targetRange = add(new FloatSetting("target_range", "Target range", 5F, 2F, 10F));
    private final FloatSetting placeRange = add(new FloatSetting("place_range", "Place range", 4.5F, 2F, 6F));
    private final FloatSetting interval = add(new FloatSetting("interval_ticks", "Ticks between placements", 3F, 1F, 20F));
    private ClientLevel observedLevel;
    private LocalPlayer observedPlayer;
    private int cooldown;
    private BlockPos pendingLavaFloor;

    public AutoTrap() {
        super("auto_trap", "Auto Trap", "Box nearby opponents or trap them with webs, fire, or lava.", Category.COMBAT);
    }

    @Override public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (client.gameMode == null || client.gui.screen() != null || !player.isAlive()
            || player.isSpectator() || player.isHandsBusy() || player.isUsingItem()
            || client.options.keyUse.isDown() || client.options.keyAttack.isDown()
            || SoupUse.busy(client)) { pendingLavaFloor = null; return; }
        if (observedPlayer != player || observedLevel != client.level) {
            observedPlayer = player;
            observedLevel = client.level;
            cooldown = 0;
            pendingLavaFloor = null;
        }
        if (cooldown > 0) { cooldown--; return; }
        Mode selected = Mode.parse(mode.get());
        if (selected == null) { pendingLavaFloor = null; return; }
        Item item = selected == Mode.BOX ? configuredBoxItem() : selected.item;
        if (item == null || !hasItem(player, item)) { pendingLavaFloor = null; return; }
        List<LivingEntity> targets = CrystalCombat.targets(client, targetRange.get(), targetMobs.get());
        targets.sort(Comparator.comparingDouble(player::distanceToSqr));
        if (targets.isEmpty()) { pendingLavaFloor = null; return; }
        Placement placement = null;
        for (LivingEntity target : targets) {
            placement = findPlacement(client, target, selected);
            if (placement != null) break;
        }
        if (placement == null) { pendingLavaFloor = null; return; }
        if (selected == Mode.LAVA) {
            if (pendingLavaFloor == null || !pendingLavaFloor.equals(placement.hit().getBlockPos())) {
                aimAt(player, placement.hit().getLocation());
                pendingLavaFloor = placement.hit().getBlockPos();
                return;
            }
            if (!lookingAtFloor(client, player, pendingLavaFloor)) {
                pendingLavaFloor = null;
                return;
            }
        } else pendingLavaFloor = null;
        int oldSlot = player.getInventory().getSelectedSlot();
        InteractionHand hand = selectHand(player, item);
        if (hand == null) return;
        try {
            ItemStack stack = player.getItemInHand(hand);
            InteractionResult result = selected == Mode.LAVA
                ? client.gameMode.useItem(player, hand)
                : client.gameMode.useItemOn(player, hand, placement.hit());
            if (result.consumesAction()) {
                player.swing(hand, stack.getInteractAnimation(), false);
                player.itemUsed(hand);
            }
            cooldown = (int)interval.get();
        } finally {
            pendingLavaFloor = null;
            if (hand == InteractionHand.MAIN_HAND && restoreSlot.get()
                && player.getInventory().getSelectedSlot() != oldSlot) {
                player.getInventory().setSelectedSlot(oldSlot);
                player.connection.send(new ServerboundSetCarriedItemPacket(oldSlot));
            }
        }
    }

    private Placement findPlacement(Minecraft client, LivingEntity target, Mode selected) {
        BlockPos feet = target.blockPosition();
        if (selected == Mode.BOX) return boxPlacement(client, feet);
        if (!client.level.isEmptyBlock(feet) || client.player.getBoundingBox().intersects(new AABB(feet))) return null;
        if (selected == Mode.IGNITE
            && !BaseFireBlock.canBePlacedAt(client.level, feet, client.player.getDirection())) return null;
        BlockHitResult floor = support(client, client.player, feet, true);
        return floor == null ? null : new Placement(floor);
    }

    private Placement boxPlacement(Minecraft client, BlockPos feet) {
        for (int height = 0; height <= 2; height++)
            for (Direction side : SIDES) {
                BlockPos block = feet.above(height).relative(side);
                if (!placeable(client, block)) continue;
                BlockHitResult hit = support(client, client.player, block, false);
                if (hit != null) return new Placement(hit);
            }
        BlockPos roof = feet.above(2);
        if (placeable(client, roof)) {
            BlockHitResult hit = support(client, client.player, roof, false);
            if (hit != null) return new Placement(hit);
        }
        return null;
    }

    private static boolean placeable(Minecraft client, BlockPos pos) {
        return client.level.getBlockState(pos).canBeReplaced()
            && client.level.getEntities(null, new AABB(pos)).isEmpty();
    }

    private BlockHitResult support(Minecraft client, LocalPlayer player, BlockPos target, boolean floorOnly) {
        Vec3 eyes = player.getEyePosition();
        for (Direction direction : SUPPORTS) {
            if (floorOnly && direction != Direction.DOWN) break;
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

    private static void aimAt(LocalPlayer player, Vec3 point) {
        Vec3 delta = point.subtract(player.getEyePosition());
        player.setYRot((float)Math.toDegrees(Math.atan2(-delta.x, delta.z)));
        player.setXRot((float)-Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z))));
    }

    private boolean lookingAtFloor(Minecraft client, LocalPlayer player, BlockPos floor) {
        Vec3 eyes = player.getEyePosition();
        Vec3 end = eyes.add(player.getViewVector(1F).scale(placeRange.get()));
        HitResult ray = client.level.clip(new ClipContext(eyes, end,
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return ray instanceof BlockHitResult hit && hit.getBlockPos().equals(floor);
    }

    private Item configuredBoxItem() {
        Identifier id = Identifier.tryParse(boxBlock.get().trim());
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
        pendingLavaFloor = null;
    }

    private enum Mode {
        BOX(null), WEB(Items.COBWEB), IGNITE(Items.FLINT_AND_STEEL), LAVA(Items.LAVA_BUCKET);
        final Item item;
        Mode(Item item) { this.item = item; }
        static Mode parse(String value) {
            try { return valueOf(value.trim().toUpperCase(java.util.Locale.ROOT)); }
            catch (IllegalArgumentException exception) { return null; }
        }
    }

    private record Placement(BlockHitResult hit) { }
}
