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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractBedBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Places and detonates beds only where the dimension's bed rule makes them explode. */
public final class BedAura extends Module {
    private static final Direction[] HORIZONTALS = {
        Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
    };
    private static final Direction[] FACES = {
        Direction.UP, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.DOWN
    };
    private final BooleanSetting place = add(new BooleanSetting("place", "Place beds", true));
    private final BooleanSetting detonate = add(new BooleanSetting("detonate", "Detonate beds", true));
    private final BooleanSetting targetMobs = add(new BooleanSetting("target_mobs", "Target mobs", false));
    private final BooleanSetting useOffhand = add(new BooleanSetting("use_offhand", "Use offhand beds", true));
    private final BooleanSetting restoreSlot = add(new BooleanSetting("restore_slot", "Restore previous hotbar slot", true));
    private final BooleanSetting rotate = add(new BooleanSetting("rotate", "Face bed placement direction", true));
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
    private boolean actedThisTick;

    public BedAura() {
        super("bed_aura", "Bed Aura", "Place and explode beds near targets in dangerous dimensions.", Category.COMBAT);
    }

    public boolean actedThisTick(Minecraft client) {
        return enabled() && actedThisTick && observedPlayer == client.player && observedLevel == client.level;
    }

    @Override public void tick(Minecraft client) {
        actedThisTick = false;
        if (client.gameMode == null || client.gui.screen() != null || !client.player.isAlive()
            || client.player.isSpectator() || client.player.isHandsBusy() || client.player.isUsingItem()
            || client.options.keyUse.isDown() || client.options.keyAttack.isDown()
            || SoupUse.busy(client)
            || AnomolyClient.MODULES.get(CrystalAura.class).engaged(client)
            || AnomolyClient.MODULES.get(AutoCrystalBase.class).placedThisTick(client)
            || AnomolyClient.MODULES.get(AnchorAura.class).actedThisTick(client)) return;
        LocalPlayer player = client.player;
        if (observedPlayer != player || observedLevel != client.level) {
            observedPlayer = player;
            observedLevel = client.level;
            cooldown = 20;
            return;
        }
        if (cooldown > 0) { cooldown--; return; }
        List<LivingEntity> targets = CrystalCombat.targets(client, targetRange.get(), targetMobs.get());
        if (targets.isEmpty()) return;
        Action action = detonate.get() ? bestExisting(client, targets) : null;
        if (action == null && place.get() && detonate.get() && hasBed(player))
            action = bestPlacement(client, targets);
        if (action == null) return;
        if (action.place() && player.getDirection() != action.facing()) {
            if (!rotate.get()) return;
            float yaw = yawFor(action.facing());
            player.setYRot(yaw);
            player.setYHeadRot(yaw);
            cooldown = 2;
            return;
        }
        int oldSlot = player.getInventory().getSelectedSlot();
        InteractionHand hand = action.place() ? selectBedHand(player) : selectDetonationHand(player);
        if (hand == null) return;
        try {
            ItemStack stack = player.getItemInHand(hand);
            InteractionResult result = client.gameMode.useItemOn(player, hand, action.hit());
            if (result.consumesAction()) {
                player.swing(hand, stack.getInteractAnimation(), false);
                player.itemUsed(hand);
                actedThisTick = true;
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
                    BlockPos head = new BlockPos(x, y, z);
                    BlockState state = client.level.getBlockState(head);
                    if (!(state.getBlock() instanceof BedBlock bed)
                        || state.getValue(AbstractBedBlock.PART) != BedPart.HEAD
                        || !bedExplodes(client)) continue;
                    Direction facing = state.getValue(AbstractBedBlock.FACING);
                    BlockPos foot = head.relative(facing.getOpposite());
                    BlockState other = client.level.getBlockState(foot);
                    if (other.getBlock() != bed || other.getValue(AbstractBedBlock.PART) != BedPart.FOOT
                        || other.getValue(AbstractBedBlock.FACING) != facing) continue;
                    BlockHitResult hit = bedHit(client, player, head);
                    if (hit == null) hit = bedHit(client, player, foot);
                    if (hit == null) continue;
                    double score = score(client, foot, head, targets);
                    if (score > bestScore) { bestScore = score; best = new Action(hit, false, facing); }
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
                    BlockPos foot = new BlockPos(x, y, z);
                    if (!emptyPlacement(client, foot)) continue;
                    BlockHitResult hit = floorHit(client, player, foot);
                    if (hit == null) continue;
                    for (Direction facing : HORIZONTALS) {
                        if (!rotate.get() && player.getDirection() != facing) continue;
                        BlockPos head = foot.relative(facing);
                        if (!emptyPlacement(client, head) || !bedExplodes(client)) continue;
                        double score = score(client, foot, head, targets);
                        if (score > bestScore) {
                            bestScore = score;
                            best = new Action(hit, true, facing);
                        }
                    }
                }
        return best;
    }

    private static boolean emptyPlacement(Minecraft client, BlockPos pos) {
        BlockPos floor = pos.below();
        return client.level.getBlockState(pos).canBeReplaced()
            && client.level.getEntities(null, new AABB(pos)).isEmpty()
            && client.level.getBlockState(floor).isFaceSturdy(client.level, floor, Direction.UP)
            && client.level.getBlockEntity(floor) == null;
    }

    private static boolean bedExplodes(Minecraft client) {
        return client.level.dimension().equals(Level.NETHER) || client.level.dimension().equals(Level.END);
    }

    private double score(Minecraft client, BlockPos foot, BlockPos head, List<LivingEntity> targets) {
        return BedCombat.score(client, foot, head, targets, minTargetDamage.get(),
            maxSelfDamage.get(), minAdvantage.get(), healthReserve.get());
    }

    private BlockHitResult floorHit(Minecraft client, LocalPlayer player, BlockPos foot) {
        BlockPos floor = foot.below();
        Vec3 eyes = player.getEyePosition();
        Vec3 hit = new Vec3(foot.getX() + 0.5, foot.getY(), foot.getZ() + 0.5);
        if (eyes.distanceToSqr(hit) > square(placeRange.get())) return null;
        if (lineOfSight.get()) {
            HitResult ray = client.level.clip(new ClipContext(eyes, hit.add(0, -0.01, 0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (!(ray instanceof BlockHitResult seen) || !seen.getBlockPos().equals(floor)) return null;
        }
        return new BlockHitResult(hit, Direction.UP, floor, false);
    }

    private BlockHitResult bedHit(Minecraft client, LocalPlayer player, BlockPos head) {
        Vec3 eyes = player.getEyePosition();
        for (Direction face : FACES) {
            Vec3 hit = Vec3.atCenterOf(head).add(
                face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
            if (eyes.distanceToSqr(hit) > square(placeRange.get())) continue;
            if (lineOfSight.get()) {
                Vec3 inside = Vec3.atCenterOf(head).add(
                    face.getStepX() * 0.49, face.getStepY() * 0.49, face.getStepZ() * 0.49);
                HitResult ray = client.level.clip(new ClipContext(eyes, inside,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
                if (!(ray instanceof BlockHitResult seen) || !seen.getBlockPos().equals(head)) continue;
            }
            return new BlockHitResult(hit, face, head, false);
        }
        return null;
    }

    private static boolean isBed(ItemStack stack) {
        return stack.getItem() instanceof BlockItem block && block.getBlock() instanceof BedBlock;
    }

    private boolean hasBed(LocalPlayer player) {
        if (useOffhand.get() && isBed(player.getOffhandItem())) return true;
        for (int slot = 0; slot < 9; slot++) if (isBed(player.getInventory().getItem(slot))) return true;
        return false;
    }

    private InteractionHand selectBedHand(LocalPlayer player) {
        if (useOffhand.get() && isBed(player.getOffhandItem())) return InteractionHand.OFF_HAND;
        if (isBed(player.getMainHandItem())) return InteractionHand.MAIN_HAND;
        for (int slot = 0; slot < 9; slot++) if (isBed(player.getInventory().getItem(slot))) {
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
        if (!isBed(player.getMainHandItem())) return InteractionHand.MAIN_HAND;
        for (int slot = 0; slot < 9; slot++) if (!isBed(player.getInventory().getItem(slot))) {
            player.getInventory().setSelectedSlot(slot);
            return InteractionHand.MAIN_HAND;
        }
        return InteractionHand.MAIN_HAND;
    }

    private static float yawFor(Direction direction) {
        return switch (direction) {
            case NORTH -> 180F;
            case EAST -> -90F;
            case SOUTH -> 0F;
            case WEST -> 90F;
            default -> 0F;
        };
    }

    private static double square(double value) { return value * value; }

    @Override protected void onDisable(Minecraft client) {
        observedLevel = null;
        observedPlayer = null;
        cooldown = 0;
        actedThisTick = false;
    }

    private record Action(BlockHitResult hit, boolean place, Direction facing) { }
}
