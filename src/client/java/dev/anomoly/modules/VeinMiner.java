package dev.anomoly.modules;

import dev.anomoly.AnomolyClient;
import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.FloatSetting;
import dev.anomoly.setting.StringSetting;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;

/** Continues a manually started ore or log break through connected blocks. */
public final class VeinMiner extends Module {
    private final FloatSetting range = add(new FloatSetting("range", "Maximum mining reach", 4.5F, 1F, 6F));
    private final FloatSetting maxBlocks = add(new FloatSetting("max_blocks", "Maximum vein size", 32F, 2F, 128F));
    private final FloatSetting searchRadius = add(new FloatSetting("search_radius", "Vein search radius", 8F, 2F, 16F));
    private final StringSetting blockIds = add(new StringSetting("block_ids", "Extra block IDs (comma separated)", ""));
    private final BooleanSetting anyBlock = add(new BooleanSetting("any_block", "Allow every block type", false));
    private final BooleanSetting diagonal = add(new BooleanSetting("diagonal", "Connect diagonal blocks", false));
    private final BooleanSetting preserveFloor = add(new BooleanSetting("preserve_floor", "Keep blocks under your feet", true));
    private final BooleanSetting lineOfSight = add(new BooleanSetting("line_of_sight", "Require line of sight", false));
    private final BooleanSetting autoTool = add(new BooleanSetting("auto_tool", "Choose a suitable hotbar tool", true));
    private final BooleanSetting restoreSlot = add(new BooleanSetting("restore_slot", "Restore previous hotbar slot", true));
    private final ArrayDeque<BlockPos> vein = new ArrayDeque<>();
    private ClientLevel observedLevel;
    private LocalPlayer observedPlayer;
    private Block blockType;
    private BlockPos miningPos;
    private Direction miningFace;
    private int originalSlot = -1;
    private int toolSlot = -1;
    private int miningTicks;
    private int idleTicks;
    private boolean selfMining;

    public VeinMiner() {
        super("vein_miner", "Vein Miner", "Mine connected ore or log blocks after you break one.", Category.WORLD);
    }

    public void onManualStart(Minecraft client, BlockPos pos) {
        if (!enabled() || selfMining || !vein.isEmpty() || client.player == null || client.level == null
            || client.gui.screen() != null || !client.options.keyAttack.isDown()
            || AnomolyClient.MODULES.get(Nuker.class).enabled()
            || AnomolyClient.MODULES.get(NukerLegit.class).enabled()
            || !(client.hitResult instanceof BlockHitResult hit)
            || !hit.getBlockPos().equals(pos)) return;
        BlockState state = client.level.getBlockState(pos);
        if (state.isAir() || state.getDestroySpeed(client.level, pos) < 0F
            || !allowed(state.getBlock())) return;
        reset(client);
        observedLevel = client.level;
        observedPlayer = client.player;
        blockType = state.getBlock();
        buildVein(client, pos);
    }

    private boolean allowed(Block block) {
        if (anyBlock.get()) return true;
        String id = BuiltInRegistries.BLOCK.getKey(block).toString();
        String path = id.substring(id.indexOf(':') + 1);
        if (path.endsWith("_ore") || path.equals("ancient_debris")
            || path.endsWith("_log") || path.endsWith("_stem")) return true;
        for (String listed : blockIds.get().split(","))
            if (id.equals(listed.trim())) return true;
        return false;
    }

    private void buildVein(Minecraft client, BlockPos seed) {
        ArrayDeque<BlockPos> pending = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        pending.add(seed);
        seen.add(seed);
        double radiusSq = searchRadius.get() * searchRadius.get();
        while (!pending.isEmpty() && vein.size() < (int)maxBlocks.get()) {
            BlockPos pos = pending.removeFirst();
            vein.addLast(pos);
            for (int dx = -1; dx <= 1; dx++)
                for (int dy = -1; dy <= 1; dy++)
                    for (int dz = -1; dz <= 1; dz++) {
                        int nonzero = (dx != 0 ? 1 : 0) + (dy != 0 ? 1 : 0) + (dz != 0 ? 1 : 0);
                        if (nonzero == 0 || !diagonal.get() && nonzero != 1) continue;
                        BlockPos next = pos.offset(dx, dy, dz);
                        if (seed.distSqr(next) > radiusSq || !seen.add(next)
                            || client.level.getBlockState(next).getBlock() != blockType) continue;
                        pending.addLast(next);
                    }
        }
    }

    @Override public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (observedPlayer != player || observedLevel != client.level) {
            reset(client);
            observedPlayer = player;
            observedLevel = client.level;
            return;
        }
        if (vein.isEmpty()) return;
        if (client.gameMode == null || client.gui.screen() != null || !player.isAlive()
            || player.isSpectator() || player.isHandsBusy() || player.isUsingItem()
            || client.options.keyAttack.isDown() || client.options.keyUse.isDown()
            || AnomolyClient.MODULES.get(Nuker.class).enabled()
            || AnomolyClient.MODULES.get(NukerLegit.class).enabled()) {
            finishCurrent(client);
            return;
        }
        vein.removeIf(pos -> client.level.getBlockState(pos).getBlock() != blockType);
        if (vein.isEmpty()) { reset(client); return; }
        if (miningPos != null && (client.level.getBlockState(miningPos).getBlock() != blockType
            || player.getEyePosition().distanceToSqr(Vec3.atCenterOf(miningPos)) > reach(player) * reach(player)
            || toolSlot >= 0 && player.getInventory().getSelectedSlot() != toolSlot)) {
            finishCurrent(client);
        }
        if (miningPos == null) {
            Candidate next = nearest(client, player);
            if (next == null) {
                if (++idleTicks > 600) reset(client);
                return;
            }
            idleTicks = 0;
            miningPos = next.pos();
            miningFace = next.face();
            miningTicks = 0;
            originalSlot = player.getInventory().getSelectedSlot();
            toolSlot = autoTool.get() ? selectTool(player, client.level.getBlockState(miningPos)) : originalSlot;
            player.getInventory().setSelectedSlot(toolSlot);
        }
        selfMining = true;
        try {
            if (miningTicks == 0) client.gameMode.startDestroyBlock(miningPos, miningFace);
            if (!client.gameMode.continueDestroyBlock(miningPos, miningFace)) {
                finishCurrent(client);
                return;
            }
        } finally {
            selfMining = false;
        }
        if (miningTicks++ % 4 == 0)
            player.swing(InteractionHand.MAIN_HAND, player.getMainHandItem().getInteractAnimation(), false);
        if (client.level.getBlockState(miningPos).getBlock() != blockType) {
            vein.remove(miningPos);
            finishCurrent(client);
        }
    }

    private Candidate nearest(Minecraft client, LocalPlayer player) {
        Vec3 eyes = player.getEyePosition();
        double maxDistance = reach(player) * reach(player);
        Candidate best = null;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (BlockPos pos : vein) {
            if (preserveFloor.get() && underPlayer(player, pos)) continue;
            Vec3 center = Vec3.atCenterOf(pos);
            double distance = eyes.distanceToSqr(center);
            if (distance > maxDistance || distance >= bestDistance) continue;
            HitResult ray = client.level.clip(new ClipContext(eyes, center,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            BlockHitResult seen = ray instanceof BlockHitResult block && block.getBlockPos().equals(pos)
                ? block : null;
            if (lineOfSight.get() && seen == null) continue;
            best = new Candidate(pos, seen == null ? nearestFace(eyes, center) : seen.getDirection());
            bestDistance = distance;
        }
        return best;
    }

    private double reach(LocalPlayer player) {
        return Math.min(range.get(), player.blockInteractionRange());
    }

    private static boolean underPlayer(LocalPlayer player, BlockPos pos) {
        return pos.getY() == player.blockPosition().getY() - 1
            && new AABB(pos).intersects(player.getBoundingBox().move(0, -1, 0));
    }

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

    private void finishCurrent(Minecraft client) {
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

    private void reset(Minecraft client) {
        finishCurrent(client);
        vein.clear();
        blockType = null;
        idleTicks = 0;
    }

    @Override protected void onDisable(Minecraft client) {
        reset(client);
        observedLevel = null;
        observedPlayer = null;
    }

    private record Candidate(BlockPos pos, Direction face) { }
}
