package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.FloatSetting;
import dev.anomoly.setting.StringSetting;
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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Fills empty, blast-resistant one-block holes near opponents. */
public final class HoleFiller extends Module {
    private static final Direction[] WALLS = {
        Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
    };
    private final StringSetting fillBlock = add(new StringSetting("fill_block", "Fill block ID", "minecraft:obsidian"));
    private final BooleanSetting targetMobs = add(new BooleanSetting("target_mobs", "Target mobs", false));
    private final BooleanSetting useOffhand = add(new BooleanSetting("use_offhand", "Use offhand block", true));
    private final BooleanSetting restoreSlot = add(new BooleanSetting("restore_slot", "Restore previous hotbar slot", true));
    private final BooleanSetting lineOfSight = add(new BooleanSetting("line_of_sight", "Require line of sight", true));
    private final FloatSetting targetRange = add(new FloatSetting("target_range", "Target range", 8F, 2F, 12F));
    private final FloatSetting enemyDistance = add(new FloatSetting("enemy_distance", "Maximum hole distance from target", 3F, 1F, 6F));
    private final FloatSetting placeRange = add(new FloatSetting("place_range", "Place range", 4.5F, 2F, 6F));
    private final FloatSetting interval = add(new FloatSetting("interval_ticks", "Ticks between placements", 3F, 1F, 20F));
    private ClientLevel observedLevel;
    private LocalPlayer observedPlayer;
    private int cooldown;

    public HoleFiller() {
        super("hole_filler", "Hole Filler", "Fill nearby obsidian or bedrock holes before opponents enter them.", Category.COMBAT);
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
        List<LivingEntity> targets = CrystalCombat.targets(client, targetRange.get(), targetMobs.get());
        if (targets.isEmpty()) return;
        BlockHitResult hit = bestHole(client, targets);
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

    private BlockHitResult bestHole(Minecraft client, List<LivingEntity> targets) {
        LocalPlayer player = client.player;
        BlockPos origin = player.blockPosition();
        int radius = (int)Math.ceil(placeRange.get());
        double bestScore = Double.POSITIVE_INFINITY;
        BlockHitResult best = null;
        for (int x = origin.getX() - radius; x <= origin.getX() + radius; x++)
            for (int y = origin.getY() - 2; y <= origin.getY() + 2; y++)
                for (int z = origin.getZ() - radius; z <= origin.getZ() + radius; z++) {
                    BlockPos hole = new BlockPos(x, y, z);
                    if (!safeHole(client, hole)) continue;
                    Vec3 center = Vec3.atCenterOf(hole);
                    double nearestTarget = Double.POSITIVE_INFINITY;
                    for (LivingEntity target : targets)
                        nearestTarget = Math.min(nearestTarget, target.distanceToSqr(center));
                    if (nearestTarget > enemyDistance.get() * enemyDistance.get()) continue;
                    BlockHitResult hit = floorHit(client, player, hole);
                    if (hit == null) continue;
                    double score = nearestTarget + player.distanceToSqr(center) * 0.1;
                    if (score < bestScore) { bestScore = score; best = hit; }
                }
        return best;
    }

    private boolean safeHole(Minecraft client, BlockPos hole) {
        if (!client.level.isEmptyBlock(hole) || !client.level.isEmptyBlock(hole.above())
            || !blastWall(client.level.getBlockState(hole.below()))
            || !client.level.getEntities(null, new AABB(hole.getX(), hole.getY(), hole.getZ(),
                hole.getX() + 1, hole.getY() + 2, hole.getZ() + 1)).isEmpty()) return false;
        for (Direction side : WALLS)
            if (!blastWall(client.level.getBlockState(hole.relative(side)))) return false;
        return true;
    }

    private static boolean blastWall(BlockState state) {
        return state.is(Blocks.OBSIDIAN) || state.is(Blocks.BEDROCK) || state.is(Blocks.CRYING_OBSIDIAN);
    }

    private BlockHitResult floorHit(Minecraft client, LocalPlayer player, BlockPos hole) {
        BlockPos floor = hole.below();
        Vec3 hit = new Vec3(hole.getX() + 0.5, hole.getY(), hole.getZ() + 0.5);
        Vec3 eyes = player.getEyePosition();
        if (eyes.distanceToSqr(hit) > placeRange.get() * placeRange.get()) return null;
        if (lineOfSight.get()) {
            Vec3 inside = hit.add(0, -0.01, 0);
            HitResult ray = client.level.clip(new ClipContext(eyes, inside,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (!(ray instanceof BlockHitResult seen) || !seen.getBlockPos().equals(floor)) return null;
        }
        return new BlockHitResult(hit, Direction.UP, floor, false);
    }

    private Item configuredItem() {
        Identifier id = Identifier.tryParse(fillBlock.get().trim());
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
