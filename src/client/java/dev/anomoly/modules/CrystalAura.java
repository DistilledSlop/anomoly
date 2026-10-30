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
import net.minecraft.network.protocol.game.ServerboundPunchPacket;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Selects crystal placements and detonations by target damage and self exposure. */
public final class CrystalAura extends Module {
    private final BooleanSetting place = add(new BooleanSetting("place", "Place crystals", true));
    private final BooleanSetting breakCrystals = add(new BooleanSetting("break", "Break crystals", true));
    private final BooleanSetting targetMobs = add(new BooleanSetting("target_mobs", "Target mobs", false));
    private final BooleanSetting useOffhand = add(new BooleanSetting("use_offhand", "Use offhand crystals", true));
    private final BooleanSetting selectHotbar = add(new BooleanSetting("select_hotbar", "Select hotbar crystals", true));
    private final BooleanSetting restoreSlot = add(new BooleanSetting("restore_slot", "Restore previous hotbar slot", true));
    private final BooleanSetting lineOfSight = add(new BooleanSetting("line_of_sight", "Require line of sight", true));
    private final FloatSetting targetRange = add(new FloatSetting("target_range", "Target range", 8F, 2F, 12F));
    private final FloatSetting placeRange = add(new FloatSetting("place_range", "Place range", 4.5F, 2F, 6F));
    private final FloatSetting breakRange = add(new FloatSetting("break_range", "Break range", 4.5F, 2F, 6F));
    private final FloatSetting minTargetDamage = add(new FloatSetting("min_target_damage", "Minimum target damage", 6F, 0F, 40F));
    private final FloatSetting maxSelfDamage = add(new FloatSetting("max_self_damage", "Maximum self damage", 8F, 0F, 60F));
    private final FloatSetting minAdvantage = add(new FloatSetting("min_advantage", "Minimum damage advantage", 2F, 0F, 40F));
    private final FloatSetting healthReserve = add(new FloatSetting("health_reserve", "Minimum health afterward", 4F, 0F, 20F));
    private final FloatSetting placeDelay = add(new FloatSetting("place_delay_ticks", "Ticks between placements", 4F, 1F, 20F));
    private final FloatSetting breakDelay = add(new FloatSetting("break_delay_ticks", "Ticks between detonations", 4F, 1F, 20F));
    private ClientLevel observedLevel;
    private LocalPlayer observedPlayer;
    private int placeCooldown;
    private int breakCooldown;
    private boolean engaged;

    public CrystalAura() {
        super("crystal_aura", "Crystal Aura", "Place and detonate end crystals when target damage outweighs self damage.", Category.COMBAT);
    }

    public boolean engaged(Minecraft client) {
        return enabled() && engaged && observedPlayer == client.player && observedLevel == client.level;
    }

    @Override public void tick(Minecraft client) {
        engaged = false;
        if (AnomolyClient.MODULES.get(AutoCrystalBase.class).placedThisTick(client)) return;
        if (client.gameMode == null || client.gui.screen() != null || !client.player.isAlive()
            || client.player.isSpectator() || client.player.isHandsBusy() || client.player.isUsingItem()
            || client.options.keyUse.isDown() || client.options.keyAttack.isDown()
            || SoupUse.busy(client)
            || AnomolyClient.MODULES.get(AutoBow.class).active(client)
            || AnomolyClient.MODULES.get(AutoShoot.class).active(client)
            || AnomolyClient.MODULES.get(AutoRod.class).active(client)) return;
        LocalPlayer player = client.player;
        if (observedPlayer != player || observedLevel != client.level) {
            observedPlayer = player;
            observedLevel = client.level;
            placeCooldown = 0;
            breakCooldown = 0;
        }
        if (placeCooldown > 0) placeCooldown--;
        if (breakCooldown > 0) breakCooldown--;
        List<LivingEntity> targets = planningTargets(client);
        if (targets.isEmpty()) return;

        EndCrystal bestCrystal = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        if (breakCrystals.get()) for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof EndCrystal crystal) || !crystal.isAlive()
                || !inAttackRange(player, crystal)
                || player.getEyePosition().distanceToSqr(crystal.position()) > square(breakRange.get())
                || lineOfSight.get() && !player.hasLineOfSight(crystal)) continue;
            double score = scorePlacement(client, crystal.position(), targets);
            if (score > bestScore) { bestScore = score; bestCrystal = crystal; }
        }
        if (bestCrystal != null) {
            engaged = true;
            if (breakCooldown == 0) {
                client.gameMode.attack(player, bestCrystal);
                player.swing(InteractionHand.MAIN_HAND, player.getMainHandItem().getAttackAnimation(), false);
                player.connection.send(ServerboundPunchPacket.INSTANCE);
                breakCooldown = (int)breakDelay.get();
            }
            return;
        }
        if (!place.get() || placeCooldown > 0) return;
        BlockPos bestBase = bestBase(client, targets);
        if (bestBase == null) return;
        int oldSlot = player.getInventory().getSelectedSlot();
        InteractionHand hand = selectCrystalHand(player);
        if (hand == null) return;
        try {
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(bestBase).add(0, 0.5, 0),
                Direction.UP, bestBase, false);
            ItemStack crystal = player.getItemInHand(hand);
            InteractionResult result = client.gameMode.useItemOn(player, hand, hit);
            if (result.consumesAction()) {
                player.swing(hand, crystal.getInteractAnimation(), false);
                player.itemUsed(hand);
                engaged = true;
            }
            placeCooldown = (int)placeDelay.get();
        } finally {
            if (hand == InteractionHand.MAIN_HAND && restoreSlot.get()
                && player.getInventory().getSelectedSlot() != oldSlot) {
                player.getInventory().setSelectedSlot(oldSlot);
                player.connection.send(new ServerboundSetCarriedItemPacket(oldSlot));
            }
        }
    }

    List<LivingEntity> planningTargets(Minecraft client) {
        return CrystalCombat.targets(client, targetRange.get(), targetMobs.get());
    }

    BlockPos bestBase(Minecraft client, List<LivingEntity> targets) {
        LocalPlayer player = client.player;
        BlockPos origin = player.blockPosition();
        int radius = (int)Math.ceil(placeRange.get());
        double bestScore = Double.NEGATIVE_INFINITY;
        BlockPos best = null;
        Vec3 eyes = player.getEyePosition();
        for (int x = origin.getX() - radius; x <= origin.getX() + radius; x++)
            for (int y = origin.getY() - 2; y <= origin.getY() + 2; y++)
                for (int z = origin.getZ() - radius; z <= origin.getZ() + radius; z++) {
                    BlockPos base = new BlockPos(x, y, z);
                    if (!client.level.getBlockState(base).is(Blocks.OBSIDIAN)
                        && !client.level.getBlockState(base).is(Blocks.BEDROCK)) continue;
                    if (!CrystalCombat.freeCrystalVolume(client, base)) continue;
                    Vec3 hit = Vec3.atCenterOf(base).add(0, 0.5, 0);
                    if (eyes.distanceToSqr(hit) > square(placeRange.get())) continue;
                    if (lineOfSight.get()) {
                        HitResult ray = client.level.clip(new ClipContext(eyes,
                            new Vec3(x + 0.5, y + 0.99, z + 0.5),
                            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
                        if (!(ray instanceof BlockHitResult block) || !block.getBlockPos().equals(base)) continue;
                    }
                    Vec3 explosion = new Vec3(x + 0.5, y + 1.0, z + 0.5);
                    double score = scorePlacement(client, explosion, targets);
                    if (score > bestScore) { bestScore = score; best = base; }
                }
        return best;
    }

    double scorePlacement(Minecraft client, Vec3 explosion, List<LivingEntity> targets) {
        return CrystalCombat.score(client, explosion, targets, minTargetDamage.get(),
            maxSelfDamage.get(), minAdvantage.get(), healthReserve.get());
    }

    double projectedScore(Minecraft client, Vec3 explosion, List<LivingEntity> targets, BlockPos futureBase) {
        return CrystalCombat.projectedScore(client, explosion, targets, minTargetDamage.get(),
            maxSelfDamage.get(), minAdvantage.get(), healthReserve.get(), futureBase);
    }

    private static boolean inAttackRange(LocalPlayer player, EndCrystal crystal) {
        return player.getAttackRangeWith(player.getMainHandItem())
            .isInRange(player, crystal.getBoundingBox(), 3.0);
    }

    private InteractionHand selectCrystalHand(LocalPlayer player) {
        if (useOffhand.get() && player.getOffhandItem().is(Items.END_CRYSTAL)) return InteractionHand.OFF_HAND;
        if (player.getMainHandItem().is(Items.END_CRYSTAL)) return InteractionHand.MAIN_HAND;
        if (!selectHotbar.get()) return null;
        for (int slot = 0; slot < 9; slot++) if (player.getInventory().getItem(slot).is(Items.END_CRYSTAL)) {
            player.getInventory().setSelectedSlot(slot);
            return InteractionHand.MAIN_HAND;
        }
        return null;
    }

    private static double square(double value) { return value * value; }

    @Override protected void onDisable(Minecraft client) {
        observedLevel = null;
        observedPlayer = null;
        placeCooldown = 0;
        breakCooldown = 0;
        engaged = false;
    }
}
