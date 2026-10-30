package dev.anomoly.modules;

import dev.anomoly.AnomolyClient;
import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.FloatSetting;
import dev.anomoly.setting.StringSetting;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Continuously mines nearby blocks using the client's ordinary mining state machine. */
public class Nuker extends Module {
    private final boolean legit;
    private final FloatSetting range;
    private final FloatSetting verticalRange = add(new FloatSetting("vertical_range", "Vertical scan range", 2F, 0F, 6F));
    private final FloatSetting fieldOfView;
    private final FloatSetting delay;
    private final StringSetting blockIds = add(new StringSetting("block_ids", "Block IDs (comma separated)", ""));
    private final BooleanSetting whitelistOnly = add(new BooleanSetting("whitelist_only", "Mine only listed IDs", false));
    private final BooleanSetting exposedOnly = add(new BooleanSetting("exposed_only", "Mine exposed blocks only", true));
    private final BooleanSetting lineOfSight;
    private final BooleanSetting preserveFloor = add(new BooleanSetting("preserve_floor", "Keep blocks under your feet", true));
    private final BooleanSetting protectBlockEntities = add(new BooleanSetting("protect_block_entities", "Skip containers and other block entities", true));
    private final BooleanSetting autoTool = add(new BooleanSetting("auto_tool", "Choose a suitable hotbar tool", true));
    private final BooleanSetting restoreSlot = add(new BooleanSetting("restore_slot", "Restore previous hotbar slot", true));
    private ClientLevel observedLevel;
    private LocalPlayer observedPlayer;
    private BlockPos target;
    private Direction face;
    private Vec3 hitPoint;
    private int originalSlot = -1;
    private int toolSlot = -1;
    private int cooldown;
    private int miningTicks;

    public Nuker() {
        this("nuker", "Nuker", false);
    }

    protected Nuker(String id, String name, boolean legit) {
        super(id, name, legit
            ? "Mine nearby visible blocks one at a time after turning to face them."
            : "Mine nearby blocks one at a time without manual clicking.", Category.WORLD);
        this.legit = legit;
        range = add(new FloatSetting("range", "Mining reach", legit ? 4.25F : 5F, 1F, 6F));
        fieldOfView = add(new FloatSetting("fov", "Field of view", legit ? 90F : 360F, 20F, 360F));
        delay = add(new FloatSetting("delay_ticks", "Ticks between blocks", legit ? 4F : 0F, 0F, 20F));
        lineOfSight = legit ? null : add(new BooleanSetting("line_of_sight", "Require line of sight", false));
    }

    @Override public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (observedPlayer != player || observedLevel != client.level) {
            stop(client);
            observedPlayer = player;
            observedLevel = client.level;
            cooldown = 20;
            return;
        }
        if (client.gameMode == null || client.gui.screen() != null || !player.isAlive()
            || player.isSpectator() || player.isHandsBusy() || player.isUsingItem()
            || client.options.keyAttack.isDown() || client.options.keyUse.isDown()
            || (!legit && AnomolyClient.MODULES.get(NukerLegit.class).enabled())) {
            stop(client);
            return;
        }
        if (target != null && (client.level.getBlockState(target).isAir()
            || player.getEyePosition().distanceToSqr(Vec3.atCenterOf(target)) > effectiveRange(player) * effectiveRange(player)
            || (toolSlot >= 0 && player.getInventory().getSelectedSlot() != toolSlot))) {
            stop(client);
            cooldown = (int)delay.get();
        }
        if (cooldown > 0) { cooldown--; return; }
        if (target == null) {
            Target selected = findTarget(client);
            if (selected == null) return;
            target = selected.pos();
            face = selected.face();
            hitPoint = selected.hitPoint();
            miningTicks = 0;
            originalSlot = player.getInventory().getSelectedSlot();
            if (autoTool.get()) toolSlot = selectTool(player, client.level.getBlockState(target));
            else toolSlot = originalSlot;
            player.getInventory().setSelectedSlot(toolSlot);
        }
        if (legit) {
            turnToward(player, hitPoint);
            if (!(client.hitResult instanceof BlockHitResult hit)
                || !hit.getBlockPos().equals(target) || hit.getDirection() != face) return;
        }
        if (miningTicks == 0) client.gameMode.startDestroyBlock(target, face);
        boolean mining = client.gameMode.continueDestroyBlock(target, face);
        if (!mining) { stop(client); return; }
        if (miningTicks++ % 4 == 0)
            player.swing(InteractionHand.MAIN_HAND, player.getMainHandItem().getInteractAnimation(), false);
        if (client.level.getBlockState(target).isAir()) {
            stop(client);
            cooldown = (int)delay.get();
        }
    }

    private Target findTarget(Minecraft client) {
        LocalPlayer player = client.player;
        Vec3 eyes = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double reach = effectiveRange(player);
        double minDot = Math.cos(Math.toRadians(fieldOfView.get() / 2F));
        Set<String> listed = new HashSet<>();
        Arrays.stream(blockIds.get().split(",")).map(String::trim).filter(s -> !s.isEmpty())
            .forEach(listed::add);
        BlockPos origin = player.blockPosition();
        int horizontal = (int)Math.ceil(reach);
        int vertical = (int)verticalRange.get();
        Target best = null;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (int x = origin.getX() - horizontal; x <= origin.getX() + horizontal; x++)
            for (int y = origin.getY() - vertical; y <= origin.getY() + vertical; y++)
                for (int z = origin.getZ() - horizontal; z <= origin.getZ() + horizontal; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = client.level.getBlockState(pos);
                    if (state.isAir() || !state.getFluidState().isEmpty()
                        || state.getDestroySpeed(client.level, pos) < 0F
                        || protectBlockEntities.get() && client.level.getBlockEntity(pos) != null
                        || preserveFloor.get() && underPlayer(player, pos)
                        || exposedOnly.get() && !exposed(client.level, pos)) continue;
                    String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
                    if (whitelistOnly.get() != listed.contains(id)) continue;
                    Vec3 center = Vec3.atCenterOf(pos);
                    Vec3 toward = center.subtract(eyes);
                    double distance = toward.lengthSqr();
                    if (distance > reach * reach || distance >= bestDistance) continue;
                    if (toward.normalize().dot(look) < minDot) continue;
                    HitResult ray = client.level.clip(new ClipContext(eyes, center,
                        ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
                    BlockHitResult seen = ray instanceof BlockHitResult block && block.getBlockPos().equals(pos)
                        ? block : null;
                    if ((legit || lineOfSight.get()) && seen == null) continue;
                    Direction side = seen != null ? seen.getDirection() : nearestFace(eyes, center);
                    Vec3 hitPoint = seen != null ? seen.getLocation()
                        : center.add(side.getStepX() * 0.5, side.getStepY() * 0.5, side.getStepZ() * 0.5);
                    best = new Target(pos, side, hitPoint);
                    bestDistance = distance;
                }
        return best;
    }

    private static boolean underPlayer(LocalPlayer player, BlockPos pos) {
        return pos.getY() == player.blockPosition().getY() - 1
            && new AABB(pos).intersects(player.getBoundingBox().move(0, -1, 0));
    }

    private double effectiveRange(LocalPlayer player) {
        return Math.min(range.get(), player.blockInteractionRange());
    }

    private static boolean exposed(ClientLevel level, BlockPos pos) {
        for (Direction side : Direction.values())
            if (level.getBlockState(pos.relative(side)).isAir()) return true;
        return false;
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

    private static void turnToward(LocalPlayer player, Vec3 hit) {
        Vec3 direction = hit.subtract(player.getEyePosition());
        float yaw = (float)(Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0);
        float pitch = (float)-Math.toDegrees(Math.atan2(direction.y,
            Math.hypot(direction.x, direction.z)));
        player.setYRot(player.getYRot() + Mth.wrapDegrees(yaw - player.getYRot()));
        player.setYHeadRot(player.getYRot());
        player.setXRot(Mth.clamp(pitch, -90F, 90F));
    }

    private void stop(Minecraft client) {
        if (target != null && client.gameMode != null && client.player != null)
            client.gameMode.stopDestroyBlock();
        if (originalSlot >= 0 && restoreSlot.get() && observedPlayer == client.player
            && client.player != null && client.player.getInventory().getSelectedSlot() == toolSlot) {
            client.player.getInventory().setSelectedSlot(originalSlot);
            client.player.connection.send(new ServerboundSetCarriedItemPacket(originalSlot));
        }
        target = null;
        face = null;
        hitPoint = null;
        toolSlot = -1;
        originalSlot = -1;
        miningTicks = 0;
    }

    @Override protected void onDisable(Minecraft client) {
        stop(client);
        observedPlayer = null;
        observedLevel = null;
        cooldown = 0;
    }

    private record Target(BlockPos pos, Direction face, Vec3 hitPoint) { }
}
