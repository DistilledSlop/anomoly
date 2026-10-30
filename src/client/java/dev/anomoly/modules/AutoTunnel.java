package dev.anomoly.modules;

import dev.anomoly.AnomolyClient;
import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Mines a fixed cardinal corridor, repairs its floor, and walks through it. */
public final class AutoTunnel extends Module {
    private static final Direction[] SUPPORTS = {
        Direction.DOWN, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
    };
    private final FloatSetting width = add(new FloatSetting("width", "Tunnel width", 2F, 1F, 3F));
    private final FloatSetting height = add(new FloatSetting("height", "Tunnel height", 2F, 2F, 4F));
    private final FloatSetting length = add(new FloatSetting("length", "Tunnel length (0 unlimited)", 16F, 0F, 256F));
    private final BooleanSetting fillFloor = add(new BooleanSetting("fill_floor", "Repair floor gaps", true));
    private final BooleanSetting edgeGuard = add(new BooleanSetting("edge_guard", "Stop at unbuilt edges", true));
    private final BooleanSetting avoidFluids = add(new BooleanSetting("avoid_fluids", "Stop near fluids", true));
    private final BooleanSetting avoidFalling = add(new BooleanSetting("avoid_falling_blocks", "Stop below falling blocks", true));
    private final BooleanSetting protectBlockEntities = add(new BooleanSetting("protect_block_entities", "Stop at containers and block entities", true));
    private final BooleanSetting placeTorches = add(new BooleanSetting("place_torches", "Place torches", false));
    private final FloatSetting torchInterval = add(new FloatSetting("torch_interval", "Blocks between torches", 8F, 3F, 32F));
    private final BooleanSetting autoTool = add(new BooleanSetting("auto_tool", "Choose a suitable hotbar tool", true));
    private final BooleanSetting restoreSlot = add(new BooleanSetting("restore_slot", "Restore previous hotbar slot", true));
    private final BooleanSetting rotate = add(new BooleanSetting("rotate", "Face mined blocks", true));
    private ClientLevel observedLevel;
    private LocalPlayer observedPlayer;
    private BlockPos start;
    private Direction forward;
    private Direction left;
    private int segment;
    private int startupDelay;
    private boolean torchDone;
    private BlockPos miningPos;
    private Direction miningFace;
    private int originalSlot = -1;
    private int toolSlot = -1;
    private int miningTicks;
    private String haltedReason;
    private boolean haltedThisTick;

    public AutoTunnel() {
        super("auto_tunnel", "Auto Tunnel", "Mine a corridor, repair its floor, and walk through it.", Category.WORLD);
    }

    public boolean protectEdges() { return enabled() && edgeGuard.get(); }

    @Override public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (observedPlayer != player || observedLevel != client.level) {
            release(client);
            finishMining(client);
            observedPlayer = player;
            observedLevel = client.level;
            start = null;
            segment = 1;
            startupDelay = 20;
            haltedReason = null;
            return;
        }
        if (client.gameMode == null || client.gui.screen() != null || !player.isAlive()
            || player.isSpectator() || player.isHandsBusy() || player.isUsingItem()
            || client.options.keyAttack.isDown() || client.options.keyUse.isDown()) {
            release(client);
            finishMining(client);
            return;
        }
        if (startupDelay > 0) { startupDelay--; release(client); return; }
        if (start == null) {
            if (!player.onGround()) { release(client); return; }
            start = player.blockPosition();
            forward = player.getDirection();
            left = forward.getCounterClockWise();
            segment = 1;
            torchDone = false;
        }
        if (length.get() > 0 && segment > (int)length.get()) {
            client.gui.hud.getChat().addClientSystemMessage(
                Component.literal("[Anomoly] Auto Tunnel completed " + (segment - 1) + " blocks.")
                    .withStyle(ChatFormatting.GREEN));
            setEnabled(false);
            return;
        }
        if (Math.abs(player.getY() - start.getY()) > 0.3
            || Math.abs(lateralOffset(player)) > 0.4) {
            halt(client, "Player left the tunnel line");
            return;
        }
        haltedThisTick = false;
        if (hazard(client)) return;
        BlockPos target = nextSolid(client);
        if (haltedThisTick) return;
        haltedReason = null;
        if (target != null) {
            release(client);
            mine(client, target);
            return;
        }
        finishMining(client);
        BlockPos floorGap = nextFloorGap(client);
        if (floorGap != null) {
            release(client);
            if (!fillFloor.get()) { halt(client, "Tunnel floor has a gap"); return; }
            if (!placeFloor(client, floorGap)) halt(client, "Need solid blocks to repair tunnel floor");
            return;
        }
        if (placeTorches.get() && !torchDone && segment % (int)torchInterval.get() == 0) {
            torchDone = true;
            placeTorch(client);
            release(client);
            return;
        }
        faceForward(player);
        double traveled = (player.getX() - (start.getX() + 0.5)) * forward.getStepX()
            + (player.getZ() - (start.getZ() + 0.5)) * forward.getStepZ();
        if (traveled >= segment - 0.2) {
            segment++;
            torchDone = false;
            release(client);
        } else client.options.keyUp.setDown(true);
    }

    private double lateralOffset(LocalPlayer player) {
        return (player.getX() - (start.getX() + 0.5)) * left.getStepX()
            + (player.getZ() - (start.getZ() + 0.5)) * left.getStepZ();
    }

    private BlockPos cell(int lateral, int y) {
        return start.relative(forward, segment).relative(left, lateral).atY(start.getY() + y);
    }

    private int minLateral() { return -(int)width.get() / 2; }
    private int maxLateral() { return minLateral() + (int)width.get() - 1; }

    private boolean hazard(Minecraft client) {
        if (avoidFalling.get() && !client.level.getEntitiesOfClass(FallingBlockEntity.class,
            client.player.getBoundingBox().inflate(4)).isEmpty()) {
            halt(client, "Waiting for falling blocks");
            return true;
        }
        for (int lateral = minLateral(); lateral <= maxLateral(); lateral++)
            for (int y = 0; y < (int)height.get(); y++) {
                BlockPos pos = cell(lateral, y);
                if (avoidFluids.get()) {
                    for (Direction side : Direction.values()) {
                        BlockPos nearby = pos.relative(side);
                        if (!client.level.getBlockState(nearby).getFluidState().isEmpty()) {
                            halt(client, "Fluid beside the next tunnel segment");
                            return true;
                        }
                    }
                    if (!client.level.getBlockState(pos).getFluidState().isEmpty()) {
                        halt(client, "Fluid in the next tunnel segment");
                        return true;
                    }
                }
                if (avoidFalling.get() && y == (int)height.get() - 1
                    && client.level.getBlockState(pos.above()).getBlock() instanceof FallingBlock) {
                    halt(client, "Falling block above the next tunnel segment");
                    return true;
                }
            }
        return false;
    }

    private BlockPos nextSolid(Minecraft client) {
        for (int y = (int)height.get() - 1; y >= 0; y--)
            for (int lateral = minLateral(); lateral <= maxLateral(); lateral++) {
                BlockPos pos = cell(lateral, y);
                BlockState state = client.level.getBlockState(pos);
                if (state.isAir() || state.getCollisionShape(client.level, pos).isEmpty()) continue;
                if (state.getDestroySpeed(client.level, pos) < 0F
                    || protectBlockEntities.get() && client.level.getBlockEntity(pos) != null) {
                    halt(client, "Unbreakable or protected block in tunnel");
                    return null;
                }
                return pos;
            }
        return null;
    }

    private BlockPos nextFloorGap(Minecraft client) {
        for (int lateral = minLateral(); lateral <= maxLateral(); lateral++) {
            BlockPos pos = cell(lateral, -1);
            if (!client.level.getBlockState(pos).isCollisionShapeFullBlock(client.level, pos)) return pos;
        }
        return null;
    }

    private void mine(Minecraft client, BlockPos pos) {
        LocalPlayer player = client.player;
        if (miningPos == null || !miningPos.equals(pos)) {
            finishMining(client);
            miningPos = pos;
            miningFace = nearestFace(player.getEyePosition(), Vec3.atCenterOf(pos));
            miningTicks = 0;
            originalSlot = player.getInventory().getSelectedSlot();
            toolSlot = autoTool.get() ? selectTool(player, client.level.getBlockState(pos)) : originalSlot;
            player.getInventory().setSelectedSlot(toolSlot);
        }
        if (rotate.get() && faceBlock(player, pos)) return;
        if (miningTicks == 0) client.gameMode.startDestroyBlock(pos, miningFace);
        if (!client.gameMode.continueDestroyBlock(pos, miningFace)) {
            finishMining(client);
            return;
        }
        if (miningTicks++ % 4 == 0)
            player.swing(InteractionHand.MAIN_HAND, player.getMainHandItem().getInteractAnimation(), false);
        if (client.level.getBlockState(pos).isAir()) finishMining(client);
    }

    private static boolean faceBlock(LocalPlayer player, BlockPos pos) {
        Vec3 direction = Vec3.atCenterOf(pos).subtract(player.getEyePosition());
        float yaw = (float)(Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0);
        float pitch = (float)-Math.toDegrees(Math.atan2(direction.y,
            Math.hypot(direction.x, direction.z)));
        boolean changed = Math.abs(net.minecraft.util.Mth.wrapDegrees(yaw - player.getYRot())) > 1F
            || Math.abs(pitch - player.getXRot()) > 1F;
        player.setYRot(yaw);
        player.setYHeadRot(yaw);
        player.setXRot(pitch);
        return changed;
    }

    private static void faceForward(LocalPlayer player, Direction direction) {
        float yaw = switch (direction) {
            case NORTH -> 180F;
            case EAST -> -90F;
            case SOUTH -> 0F;
            case WEST -> 90F;
            default -> 0F;
        };
        player.setYRot(yaw);
        player.setYHeadRot(yaw);
        player.setXRot(0F);
    }

    private void faceForward(LocalPlayer player) { faceForward(player, forward); }

    private static Direction nearestFace(Vec3 eyes, Vec3 center) {
        Vec3 d = eyes.subtract(center);
        double ax = Math.abs(d.x), ay = Math.abs(d.y), az = Math.abs(d.z);
        if (ay >= ax && ay >= az) return d.y >= 0 ? Direction.UP : Direction.DOWN;
        if (ax >= az) return d.x >= 0 ? Direction.EAST : Direction.WEST;
        return d.z >= 0 ? Direction.SOUTH : Direction.NORTH;
    }

    private static int selectTool(LocalPlayer player, BlockState state) {
        var inventory = player.getInventory();
        int best = inventory.getSelectedSlot();
        ItemStack bestStack = inventory.getItem(best);
        boolean drops = bestStack.isCorrectToolForDrops(state);
        float speed = bestStack.getDestroySpeed(state);
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = inventory.getItem(slot);
            boolean candidateDrops = stack.isCorrectToolForDrops(state);
            float candidateSpeed = stack.getDestroySpeed(state);
            if (candidateDrops && !drops || candidateDrops == drops && candidateSpeed > speed + 0.01F) {
                best = slot;
                drops = candidateDrops;
                speed = candidateSpeed;
            }
        }
        return best;
    }

    private boolean placeFloor(Minecraft client, BlockPos target) {
        if (!client.level.getBlockState(target).canBeReplaced()) return false;
        BlockHitResult hit = supportHit(client, target);
        if (hit == null) return false;
        LocalPlayer player = client.player;
        int oldSlot = player.getInventory().getSelectedSlot();
        InteractionHand hand = selectBlockHand(client, player);
        if (hand == null) return false;
        try {
            ItemStack stack = player.getItemInHand(hand);
            InteractionResult result = client.gameMode.useItemOn(player, hand, hit);
            if (!result.consumesAction()) return false;
            player.swing(hand, stack.getInteractAnimation(), false);
            player.itemUsed(hand);
            return true;
        } finally {
            restore(client, player, oldSlot, hand);
        }
    }

    private BlockHitResult supportHit(Minecraft client, BlockPos target) {
        for (Direction side : SUPPORTS) {
            BlockPos support = target.relative(side);
            Direction face = side.getOpposite();
            if (!client.level.getBlockState(support).isFaceSturdy(client.level, support, face)) continue;
            Vec3 hit = Vec3.atCenterOf(support).add(
                face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
            if (client.player.getEyePosition().distanceToSqr(hit) > 20.25) continue;
            return new BlockHitResult(hit, face, support, false);
        }
        return null;
    }

    private InteractionHand selectBlockHand(Minecraft client, LocalPlayer player) {
        if (usableBlock(client, player.getOffhandItem())) return InteractionHand.OFF_HAND;
        if (usableBlock(client, player.getMainHandItem())) return InteractionHand.MAIN_HAND;
        for (int slot = 0; slot < 9; slot++)
            if (usableBlock(client, player.getInventory().getItem(slot))) {
                player.getInventory().setSelectedSlot(slot);
                return InteractionHand.MAIN_HAND;
            }
        return null;
    }

    private static boolean usableBlock(Minecraft client, ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) return false;
        var block = blockItem.getBlock();
        return !(block instanceof FallingBlock) && !(block instanceof EntityBlock)
            && block.defaultBlockState().isCollisionShapeFullBlock(client.level, BlockPos.ZERO);
    }

    private void placeTorch(Minecraft client) {
        BlockPos target = cell(minLateral(), 0);
        if (!client.level.getBlockState(target).canBeReplaced()) return;
        BlockPos floor = target.below();
        if (!client.level.getBlockState(floor).isFaceSturdy(client.level, floor, Direction.UP)) return;
        LocalPlayer player = client.player;
        int oldSlot = player.getInventory().getSelectedSlot();
        InteractionHand hand = null;
        if (player.getOffhandItem().is(Items.TORCH)) hand = InteractionHand.OFF_HAND;
        else if (player.getMainHandItem().is(Items.TORCH)) hand = InteractionHand.MAIN_HAND;
        else for (int slot = 0; slot < 9; slot++)
            if (player.getInventory().getItem(slot).is(Items.TORCH)) {
                player.getInventory().setSelectedSlot(slot);
                hand = InteractionHand.MAIN_HAND;
                break;
            }
        if (hand == null) return;
        try {
            Vec3 hit = new Vec3(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);
            if (player.getEyePosition().distanceToSqr(hit) > 20.25) return;
            ItemStack stack = player.getItemInHand(hand);
            InteractionResult result = client.gameMode.useItemOn(player, hand,
                new BlockHitResult(hit, Direction.UP, floor, false));
            if (result.consumesAction()) {
                player.swing(hand, stack.getInteractAnimation(), false);
                player.itemUsed(hand);
            }
        } finally {
            restore(client, player, oldSlot, hand);
        }
    }

    private void restore(Minecraft client, LocalPlayer player, int oldSlot, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && restoreSlot.get()
            && player.getInventory().getSelectedSlot() != oldSlot) {
            player.getInventory().setSelectedSlot(oldSlot);
            player.connection.send(new ServerboundSetCarriedItemPacket(oldSlot));
        }
    }

    private void finishMining(Minecraft client) {
        if (miningPos != null && client.gameMode != null && client.player != null)
            client.gameMode.stopDestroyBlock();
        if (originalSlot >= 0 && restoreSlot.get() && observedPlayer == client.player
            && client.player != null && client.player.getInventory().getSelectedSlot() == toolSlot) {
            client.player.getInventory().setSelectedSlot(originalSlot);
            client.player.connection.send(new ServerboundSetCarriedItemPacket(originalSlot));
        }
        miningPos = null;
        miningFace = null;
        originalSlot = -1;
        toolSlot = -1;
        miningTicks = 0;
    }

    private void halt(Minecraft client, String reason) {
        release(client);
        finishMining(client);
        haltedThisTick = true;
        if (!reason.equals(haltedReason)) {
            haltedReason = reason;
            client.gui.hud.getChat().addClientSystemMessage(
                Component.literal("[Anomoly] Auto Tunnel paused: " + reason + ".")
                    .withStyle(ChatFormatting.YELLOW));
        }
    }

    private static void release(Minecraft client) {
        client.options.keyUp.setDown(false);
    }

    @Override protected void onDisable(Minecraft client) {
        release(client);
        finishMining(client);
        if (client.player != null) KeyMapping.setAll();
        observedLevel = null;
        observedPlayer = null;
        start = null;
        forward = null;
        left = null;
        segment = 0;
        startupDelay = 0;
        haltedReason = null;
        haltedThisTick = false;
    }
}
