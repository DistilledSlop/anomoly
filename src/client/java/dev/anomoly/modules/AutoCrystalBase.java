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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Places obsidian beneath a safe, useful end-crystal spawn position. */
public final class AutoCrystalBase extends Module {
    private static final Direction[] SUPPORTS = {
        Direction.DOWN, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP
    };
    private final BooleanSetting targetMobs = add(new BooleanSetting("target_mobs", "Target mobs", false));
    private final BooleanSetting requireCrystals = add(new BooleanSetting("require_crystals", "Require end crystals", true));
    private final BooleanSetting useOffhand = add(new BooleanSetting("use_offhand", "Use offhand obsidian", true));
    private final BooleanSetting restoreSlot = add(new BooleanSetting("restore_slot", "Restore previous hotbar slot", true));
    private final BooleanSetting lineOfSight = add(new BooleanSetting("line_of_sight", "Require line of sight", true));
    private final FloatSetting targetRange = add(new FloatSetting("target_range", "Target range", 8F, 2F, 12F));
    private final FloatSetting placeRange = add(new FloatSetting("place_range", "Base place range", 4.5F, 2F, 6F));
    private final FloatSetting minTargetDamage = add(new FloatSetting("min_target_damage", "Minimum target damage", 6F, 0F, 40F));
    private final FloatSetting maxSelfDamage = add(new FloatSetting("max_self_damage", "Maximum self damage", 8F, 0F, 60F));
    private final FloatSetting minAdvantage = add(new FloatSetting("min_advantage", "Minimum damage advantage", 2F, 0F, 40F));
    private final FloatSetting healthReserve = add(new FloatSetting("health_reserve", "Minimum health afterward", 4F, 0F, 20F));
    private final FloatSetting interval = add(new FloatSetting("interval_ticks", "Ticks between bases", 4F, 1F, 20F));
    private ClientLevel observedLevel;
    private LocalPlayer observedPlayer;
    private int cooldown;
    private boolean placedThisTick;

    public AutoCrystalBase() {
        super("auto_crystal_base", "Auto Crystal Base", "Prepare obsidian bases at useful end-crystal positions.", Category.COMBAT);
    }

    public boolean placedThisTick(Minecraft client) {
        return enabled() && placedThisTick && observedPlayer == client.player && observedLevel == client.level;
    }

    @Override public void tick(Minecraft client) {
        placedThisTick = false;
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
        if (requireCrystals.get() && !hasCrystal(player) || !hasObsidian(player)) return;
        CrystalAura aura = AnomolyClient.MODULES.get(CrystalAura.class);
        List<LivingEntity> targets = aura.enabled()
            ? aura.planningTargets(client)
            : CrystalCombat.targets(client, targetRange.get(), targetMobs.get());
        if (targets.isEmpty()) return;
        if (hasActiveCrystal(client, player) || aura.enabled() && aura.bestBase(client, targets) != null
            || !aura.enabled() && hasStandaloneBase(client, targets)) return;
        Candidate candidate = bestCandidate(client, targets, aura);
        cooldown = (int)interval.get();
        if (candidate == null) return;
        int oldSlot = player.getInventory().getSelectedSlot();
        InteractionHand hand = selectObsidian(player);
        if (hand == null) return;
        try {
            ItemStack block = player.getItemInHand(hand);
            InteractionResult result = client.gameMode.useItemOn(player, hand, candidate.hit());
            if (result.consumesAction()) {
                player.swing(hand, block.getInteractAnimation(), false);
                player.itemUsed(hand);
                placedThisTick = true;
            }
        } finally {
            if (hand == InteractionHand.MAIN_HAND && restoreSlot.get()
                && player.getInventory().getSelectedSlot() != oldSlot) {
                player.getInventory().setSelectedSlot(oldSlot);
                player.connection.send(new ServerboundSetCarriedItemPacket(oldSlot));
            }
        }
    }

    private Candidate bestCandidate(Minecraft client, List<LivingEntity> targets, CrystalAura aura) {
        LocalPlayer player = client.player;
        BlockPos origin = player.blockPosition();
        int radius = (int)Math.ceil(placeRange.get());
        double bestScore = Double.NEGATIVE_INFINITY;
        Candidate best = null;
        for (int x = origin.getX() - radius; x <= origin.getX() + radius; x++)
            for (int y = origin.getY() - 2; y <= origin.getY() + 2; y++)
                for (int z = origin.getZ() - radius; z <= origin.getZ() + radius; z++) {
                    BlockPos base = new BlockPos(x, y, z);
                    if (!client.level.getBlockState(base).canBeReplaced()
                        || !client.level.isEmptyBlock(base.above())
                        || !client.level.isEmptyBlock(base.above(2))
                        || !client.level.getEntities(null, new AABB(x, y, z, x + 1, y + 3, z + 1)).isEmpty()) continue;
                    BlockHitResult hit = support(client, player, base);
                    if (hit == null) continue;
                    Vec3 explosion = new Vec3(x + 0.5, y + 1.0, z + 0.5);
                    double score = aura.enabled()
                        ? aura.projectedScore(client, explosion, targets, base)
                        : projectedScore(client, explosion, targets, base);
                    if (score > bestScore) { bestScore = score; best = new Candidate(base, hit); }
                }
        return best;
    }

    private BlockHitResult support(Minecraft client, LocalPlayer player, BlockPos target) {
        Vec3 eyes = player.getEyePosition();
        for (Direction direction : SUPPORTS) {
            BlockPos block = target.relative(direction);
            Direction face = direction.getOpposite();
            if (!client.level.getBlockState(block).isFaceSturdy(client.level, block, face)
                || client.level.getBlockEntity(block) != null) continue;
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

    private boolean hasStandaloneBase(Minecraft client, List<LivingEntity> targets) {
        BlockPos origin = client.player.blockPosition();
        int radius = (int)Math.ceil(placeRange.get());
        Vec3 eyes = client.player.getEyePosition();
        for (int x = origin.getX() - radius; x <= origin.getX() + radius; x++)
            for (int y = origin.getY() - 2; y <= origin.getY() + 2; y++)
                for (int z = origin.getZ() - radius; z <= origin.getZ() + radius; z++) {
                    BlockPos base = new BlockPos(x, y, z);
                    if (!client.level.getBlockState(base).is(Blocks.OBSIDIAN)
                        && !client.level.getBlockState(base).is(Blocks.BEDROCK)) continue;
                    Vec3 hit = Vec3.atCenterOf(base).add(0, 0.5, 0);
                    if (eyes.distanceToSqr(hit) > placeRange.get() * placeRange.get()
                        || !CrystalCombat.freeCrystalVolume(client, base)) continue;
                    if (lineOfSight.get()) {
                        HitResult ray = client.level.clip(new ClipContext(eyes,
                            new Vec3(x + 0.5, y + 0.99, z + 0.5),
                            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, client.player));
                        if (!(ray instanceof BlockHitResult seen) || !seen.getBlockPos().equals(base)) continue;
                    }
                    if (ownScore(client, new Vec3(x + 0.5, y + 1.0, z + 0.5), targets)
                        > Double.NEGATIVE_INFINITY) return true;
                }
        return false;
    }

    private double ownScore(Minecraft client, Vec3 explosion, List<LivingEntity> targets) {
        return CrystalCombat.score(client, explosion, targets, minTargetDamage.get(),
            maxSelfDamage.get(), minAdvantage.get(), healthReserve.get());
    }

    private double projectedScore(Minecraft client, Vec3 explosion, List<LivingEntity> targets, BlockPos base) {
        return CrystalCombat.projectedScore(client, explosion, targets, minTargetDamage.get(),
            maxSelfDamage.get(), minAdvantage.get(), healthReserve.get(), base);
    }

    private boolean hasActiveCrystal(Minecraft client, LocalPlayer player) {
        double rangeSq = placeRange.get() * placeRange.get();
        for (Entity entity : client.level.entitiesForRendering())
            if (entity instanceof EndCrystal && player.distanceToSqr(entity) <= rangeSq) return true;
        return false;
    }

    private static boolean hasCrystal(LocalPlayer player) {
        if (player.getOffhandItem().is(Items.END_CRYSTAL)) return true;
        for (int slot = 0; slot < 9; slot++)
            if (player.getInventory().getItem(slot).is(Items.END_CRYSTAL)) return true;
        return false;
    }

    private boolean hasObsidian(LocalPlayer player) {
        if (useOffhand.get() && player.getOffhandItem().is(Items.OBSIDIAN)) return true;
        for (int slot = 0; slot < 9; slot++)
            if (player.getInventory().getItem(slot).is(Items.OBSIDIAN)) return true;
        return false;
    }

    private InteractionHand selectObsidian(LocalPlayer player) {
        if (useOffhand.get() && player.getOffhandItem().is(Items.OBSIDIAN)) return InteractionHand.OFF_HAND;
        if (player.getMainHandItem().is(Items.OBSIDIAN)) return InteractionHand.MAIN_HAND;
        for (int slot = 0; slot < 9; slot++) if (player.getInventory().getItem(slot).is(Items.OBSIDIAN)) {
            player.getInventory().setSelectedSlot(slot);
            return InteractionHand.MAIN_HAND;
        }
        return null;
    }

    @Override protected void onDisable(Minecraft client) {
        observedLevel = null;
        observedPlayer = null;
        cooldown = 0;
        placedThisTick = false;
    }

    private record Candidate(BlockPos base, BlockHitResult hit) { }
}
