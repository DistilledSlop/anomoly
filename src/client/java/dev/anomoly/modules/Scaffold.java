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
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Places full blocks under and just ahead of a walking player. */
public final class Scaffold extends Module {
    private static final Direction[] SUPPORTS = {
        Direction.DOWN, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
    };
    private final BooleanSetting edgeGuard = add(new BooleanSetting("edge_guard", "Stop at an unbuilt edge", true));
    private final BooleanSetting useOffhand = add(new BooleanSetting("use_offhand", "Use offhand blocks", true));
    private final BooleanSetting restoreSlot = add(new BooleanSetting("restore_slot", "Restore previous hotbar slot", true));
    private final BooleanSetting lineOfSight = add(new BooleanSetting("line_of_sight", "Require visible support face", false));
    private final BooleanSetting autoTower = add(new BooleanSetting("auto_tower", "Jump and tower while stationary", false));
    private final FloatSetting leadTicks = add(new FloatSetting("lead_ticks", "Movement lookahead (ticks)", 4F, 0F, 8F));
    private final FloatSetting placeRange = add(new FloatSetting("place_range", "Placement reach", 4.5F, 2F, 6F));
    private final FloatSetting interval = add(new FloatSetting("interval_ticks", "Ticks between placements", 1F, 1F, 10F));
    private ClientLevel observedLevel;
    private LocalPlayer observedPlayer;
    private int bridgeY;
    private int cooldown;

    public Scaffold() {
        super("scaffold", "Scaffold", "Build a bridge under your feet as you move.", Category.WORLD);
    }

    public boolean protectEdges() { return enabled() && edgeGuard.get(); }

    @Override public void tick(Minecraft client) {
        if (client.gameMode == null || client.gui.screen() != null || !client.player.isAlive()
            || client.player.isSpectator() || client.player.isHandsBusy() || client.player.isUsingItem()
            || client.options.keyUse.isDown() || client.options.keyAttack.isDown()) return;
        LocalPlayer player = client.player;
        if (observedLevel != client.level || observedPlayer != player) {
            observedLevel = client.level;
            observedPlayer = player;
            bridgeY = player.blockPosition().getY() - 1;
            cooldown = 10;
            return;
        }
        if (player.onGround()) bridgeY = player.blockPosition().getY() - 1;
        boolean tower = autoTower.get() && noMovementInput(client) && hasMaterial(client, player);
        if (tower && player.onGround()) player.jumpFromGround();
        if (player.getY() < bridgeY + 0.25 || player.getY() > bridgeY + 2.5) return;
        if (cooldown > 0) { cooldown--; return; }
        BlockHitResult hit = tower ? towerPlacement(client, player) : nextPlacement(client, player);
        if (hit == null) return;
        int oldSlot = player.getInventory().getSelectedSlot();
        InteractionHand hand = selectHand(client, player);
        if (hand == null) return;
        try {
            ItemStack stack = player.getItemInHand(hand);
            InteractionResult result = client.gameMode.useItemOn(player, hand, hit);
            if (result.consumesAction()) {
                player.swing(hand, stack.getInteractAnimation(), false);
                player.itemUsed(hand);
                cooldown = (int)interval.get() - 1;
            }
        } finally {
            if (hand == InteractionHand.MAIN_HAND && restoreSlot.get()
                && player.getInventory().getSelectedSlot() != oldSlot) {
                player.getInventory().setSelectedSlot(oldSlot);
                player.connection.send(new ServerboundSetCarriedItemPacket(oldSlot));
            }
        }
    }

    private BlockHitResult nextPlacement(Minecraft client, LocalPlayer player) {
        Vec3 velocity = player.getDeltaMovement();
        double endX = player.getX() + velocity.x * leadTicks.get();
        double endZ = player.getZ() + velocity.z * leadTicks.get();
        double distance = Math.hypot(endX - player.getX(), endZ - player.getZ());
        int samples = Math.max(1, (int)Math.ceil(distance / 0.25));
        BlockPos last = null;
        for (int i = 0; i <= samples; i++) {
            double fraction = (double)i / samples;
            BlockPos target = BlockPos.containing(player.getX() + (endX - player.getX()) * fraction,
                bridgeY, player.getZ() + (endZ - player.getZ()) * fraction);
            if (target.equals(last)) continue;
            last = target;
            if (!client.level.getBlockState(target).canBeReplaced()
                || player.getBoundingBox().intersects(new AABB(target))) continue;
            BlockHitResult hit = findSupport(client, player, target);
            if (hit != null) return hit;
        }
        return null;
    }

    private BlockHitResult towerPlacement(Minecraft client, LocalPlayer player) {
        if (player.getY() < bridgeY + 1.95) return null;
        BlockPos target = BlockPos.containing(player.getX(), player.getY() - 1.0, player.getZ());
        if (!client.level.getBlockState(target).canBeReplaced()
            || player.getBoundingBox().intersects(new AABB(target))) return null;
        return findSupport(client, player, target);
    }

    private static boolean noMovementInput(Minecraft client) {
        return !client.options.keyUp.isDown() && !client.options.keyDown.isDown()
            && !client.options.keyLeft.isDown() && !client.options.keyRight.isDown();
    }

    private boolean hasMaterial(Minecraft client, LocalPlayer player) {
        if (useOffhand.get() && usable(client, player.getOffhandItem())) return true;
        for (int slot = 0; slot < 9; slot++)
            if (usable(client, player.getInventory().getItem(slot))) return true;
        return false;
    }

    private BlockHitResult findSupport(Minecraft client, LocalPlayer player, BlockPos target) {
        for (Direction direction : SUPPORTS) {
            BlockPos support = target.relative(direction);
            BlockState state = client.level.getBlockState(support);
            Direction face = direction.getOpposite();
            if (!state.isFaceSturdy(client.level, support, face)) continue;
            Vec3 hit = Vec3.atCenterOf(support).add(
                face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
            if (player.getEyePosition().distanceToSqr(hit) > placeRange.get() * placeRange.get()) continue;
            if (lineOfSight.get()) {
                Vec3 inside = hit.add(-face.getStepX() * 0.01,
                    -face.getStepY() * 0.01, -face.getStepZ() * 0.01);
                HitResult ray = client.level.clip(new ClipContext(player.getEyePosition(), inside,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
                if (!(ray instanceof BlockHitResult seen)
                    || !seen.getBlockPos().equals(support) || seen.getDirection() != face) continue;
            }
            return new BlockHitResult(hit, face, support, false);
        }
        return null;
    }

    private InteractionHand selectHand(Minecraft client, LocalPlayer player) {
        if (useOffhand.get() && usable(client, player.getOffhandItem())) return InteractionHand.OFF_HAND;
        if (usable(client, player.getMainHandItem())) return InteractionHand.MAIN_HAND;
        for (int slot = 0; slot < 9; slot++) {
            if (!usable(client, player.getInventory().getItem(slot))) continue;
            player.getInventory().setSelectedSlot(slot);
            return InteractionHand.MAIN_HAND;
        }
        return null;
    }

    private static boolean usable(Minecraft client, ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) return false;
        var block = blockItem.getBlock();
        if (block instanceof FallingBlock || block instanceof EntityBlock) return false;
        return block.defaultBlockState().isCollisionShapeFullBlock(client.level, BlockPos.ZERO);
    }

    @Override protected void onDisable(Minecraft client) {
        observedLevel = null;
        observedPlayer = null;
        cooldown = 0;
    }
}
