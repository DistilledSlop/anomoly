package dev.anomoly.modules;

import dev.anomoly.AnomolyClient;
import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.FloatSetting;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Places, charges, and detonates respawn anchors using a blast-damage score. */
public final class AnchorAura extends Module {
    private static final Direction[] FACES = {
        Direction.UP, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.DOWN
    };
    private static final Direction[] SUPPORTS = {
        Direction.DOWN, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP
    };
    private final BooleanSetting place = add(new BooleanSetting("place", "Place anchors", true));
    private final BooleanSetting charge = add(new BooleanSetting("charge", "Charge anchors", true));
    private final BooleanSetting detonate = add(new BooleanSetting("detonate", "Detonate anchors", true));
    private final BooleanSetting targetMobs = add(new BooleanSetting("target_mobs", "Target mobs", false));
    private final BooleanSetting useOffhand = add(new BooleanSetting("use_offhand", "Use offhand items", true));
    private final BooleanSetting restoreSlot = add(new BooleanSetting("restore_slot", "Restore previous hotbar slot", true));
    private final BooleanSetting lineOfSight = add(new BooleanSetting("line_of_sight", "Require line of sight", true));
    private final FloatSetting targetRange = add(new FloatSetting("target_range", "Target range", 8F, 2F, 12F));
    private final FloatSetting placeRange = add(new FloatSetting("place_range", "Place range", 4.5F, 2F, 6F));
    private final FloatSetting minTargetDamage = add(new FloatSetting("min_target_damage", "Minimum target damage", 6F, 0F, 50F));
    private final FloatSetting maxSelfDamage = add(new FloatSetting("max_self_damage", "Maximum self damage", 8F, 0F, 60F));
    private final FloatSetting minAdvantage = add(new FloatSetting("min_advantage", "Minimum damage advantage", 2F, 0F, 50F));
    private final FloatSetting healthReserve = add(new FloatSetting("health_reserve", "Minimum health afterward", 4F, 0F, 20F));
    private final FloatSetting interval = add(new FloatSetting("interval_ticks", "Ticks between actions", 3F, 1F, 20F));
    private ClientLevel observedLevel;
    private LocalPlayer observedPlayer;
    private int cooldown;

    public AnchorAura() {
        super("anchor_aura", "Anchor Aura", "Place, charge, and detonate safe respawn anchors near targets.", Category.COMBAT);
    }

    @Override public void tick(Minecraft client) {
        if (client.gameMode == null || client.gui.screen() != null || !client.player.isAlive()
            || client.player.isSpectator() || client.player.isHandsBusy() || client.player.isUsingItem()
            || client.options.keyUse.isDown() || client.options.keyAttack.isDown()
            || SoupUse.busy(client)
            || AnomolyClient.MODULES.get(CrystalAura.class).engaged(client)
            || AnomolyClient.MODULES.get(AutoCrystalBase.class).placedThisTick(client)) return;
        LocalPlayer player = client.player;
        if (observedPlayer != player || observedLevel != client.level) {
            observedPlayer = player;
            observedLevel = client.level;
            cooldown = 20;
            return;
        }
        if (cooldown > 0) { cooldown--; return; }
        if (client.level.environmentAttributes().getValue(
            EnvironmentAttributes.RESPAWN_ANCHOR_WORKS, player.blockPosition())) return;
        List<LivingEntity> targets = CrystalCombat.targets(client, targetRange.get(), targetMobs.get());
        if (targets.isEmpty()) return;
        Action action = bestExisting(client, targets);
        if (action == null && place.get() && charge.get() && hasItem(player, Items.RESPAWN_ANCHOR)
            && hasItem(player, Items.GLOWSTONE)) action = bestPlacement(client, targets);
        if (action == null) return;
        int oldSlot = player.getInventory().getSelectedSlot();
        InteractionHand hand = switch (action.stage()) {
            case PLACE -> selectItem(player, Items.RESPAWN_ANCHOR);
            case CHARGE -> selectItem(player, Items.GLOWSTONE);
            case DETONATE -> selectDetonationHand(player);
        };
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

    private Action bestExisting(Minecraft client, List<LivingEntity> targets) {
        LocalPlayer player = client.player;
        BlockPos origin = player.blockPosition();
        int radius = (int)Math.ceil(placeRange.get());
        Action best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int x = origin.getX() - radius; x <= origin.getX() + radius; x++)
            for (int y = origin.getY() - 2; y <= origin.getY() + 2; y++)
                for (int z = origin.getZ() - radius; z <= origin.getZ() + radius; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = client.level.getBlockState(pos);
                    if (!state.is(Blocks.RESPAWN_ANCHOR)) continue;
                    int charges = state.getValue(RespawnAnchorBlock.CHARGE);
                    Stage stage;
                    if (charges > 0 && detonate.get()
                        && (!player.getOffhandItem().is(Items.GLOWSTONE) || charges == 4)) {
                        stage = Stage.DETONATE;
                    } else if (charge.get() && charges < 4 && hasItem(player, Items.GLOWSTONE)) {
                        stage = Stage.CHARGE;
                    } else continue;
                    BlockHitResult hit = anchorHit(client, player, pos);
                    if (hit == null) continue;
                    double score = score(client, pos, targets);
                    if (score > bestScore) { bestScore = score; best = new Action(hit, stage); }
                }
        return best;
    }

    private Action bestPlacement(Minecraft client, List<LivingEntity> targets) {
        LocalPlayer player = client.player;
        BlockPos origin = player.blockPosition();
        int radius = (int)Math.ceil(placeRange.get());
        Action best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int x = origin.getX() - radius; x <= origin.getX() + radius; x++)
            for (int y = origin.getY() - 1; y <= origin.getY() + 1; y++)
                for (int z = origin.getZ() - radius; z <= origin.getZ() + radius; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!client.level.getBlockState(pos).canBeReplaced()
                        || !client.level.getEntities(null, new AABB(pos)).isEmpty()) continue;
                    BlockHitResult hit = support(client, player, pos);
                    if (hit == null) continue;
                    double score = score(client, pos, targets);
                    if (score > bestScore) { bestScore = score; best = new Action(hit, Stage.PLACE); }
                }
        return best;
    }

    private double score(Minecraft client, BlockPos pos, List<LivingEntity> targets) {
        return AnchorCombat.score(client, pos, targets, minTargetDamage.get(),
            maxSelfDamage.get(), minAdvantage.get(), healthReserve.get());
    }

    private BlockHitResult anchorHit(Minecraft client, LocalPlayer player, BlockPos anchor) {
        Vec3 eyes = player.getEyePosition();
        for (Direction face : FACES) {
            Vec3 hit = Vec3.atCenterOf(anchor).add(
                face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
            if (eyes.distanceToSqr(hit) > square(placeRange.get())) continue;
            if (lineOfSight.get()) {
                Vec3 inside = Vec3.atCenterOf(anchor).add(
                    face.getStepX() * 0.49, face.getStepY() * 0.49, face.getStepZ() * 0.49);
                HitResult ray = client.level.clip(new ClipContext(eyes, inside,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
                if (!(ray instanceof BlockHitResult seen) || !seen.getBlockPos().equals(anchor)) continue;
            }
            return new BlockHitResult(hit, face, anchor, false);
        }
        return null;
    }

    private BlockHitResult support(Minecraft client, LocalPlayer player, BlockPos target) {
        Vec3 eyes = player.getEyePosition();
        for (Direction direction : SUPPORTS) {
            BlockPos block = target.relative(direction);
            Direction face = direction.getOpposite();
            BlockState state = client.level.getBlockState(block);
            if (!state.isFaceSturdy(client.level, block, face)
                || client.level.getBlockEntity(block) != null) continue;
            Vec3 hit = Vec3.atCenterOf(block).add(
                face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
            if (eyes.distanceToSqr(hit) > square(placeRange.get())) continue;
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

    private static InteractionHand selectDetonationHand(LocalPlayer player) {
        int selected = player.getInventory().getSelectedSlot();
        if (player.getInventory().getItem(selected).isEmpty()) return InteractionHand.MAIN_HAND;
        for (int slot = 0; slot < 9; slot++) if (player.getInventory().getItem(slot).isEmpty()) {
            player.getInventory().setSelectedSlot(slot);
            return InteractionHand.MAIN_HAND;
        }
        if (!player.getMainHandItem().is(Items.GLOWSTONE)) return InteractionHand.MAIN_HAND;
        for (int slot = 0; slot < 9; slot++) if (!player.getInventory().getItem(slot).is(Items.GLOWSTONE)) {
            player.getInventory().setSelectedSlot(slot);
            return InteractionHand.MAIN_HAND;
        }
        return InteractionHand.MAIN_HAND;
    }

    private static double square(double value) { return value * value; }

    @Override protected void onDisable(Minecraft client) {
        observedLevel = null;
        observedPlayer = null;
        cooldown = 0;
    }

    private enum Stage { PLACE, CHARGE, DETONATE }
    private record Action(BlockHitResult hit, Stage stage) { }
}
