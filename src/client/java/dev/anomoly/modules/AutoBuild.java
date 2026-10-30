package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.ChoiceSetting;
import dev.anomoly.setting.FloatSetting;
import dev.anomoly.setting.StringSetting;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Builds a fixed platform or a complete, lit nether portal frame. */
public final class AutoBuild extends Module {
    private static final Direction[] SUPPORTS = {
        Direction.DOWN, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP
    };
    private final ChoiceSetting mode = add(new ChoiceSetting("mode", "Structure", "Portal", "Portal", "Platform"));
    private final FloatSetting platformRadius = add(new FloatSetting("platform_radius", "Platform radius", 2F, 1F, 6F));
    private final BooleanSetting disableOnYChange = add(new BooleanSetting("disable_on_y_change", "Stop platform on height change", true));
    private final ChoiceSetting filterMode = add(new ChoiceSetting("filter_mode", "Platform block filter", "Whitelist", "Whitelist", "Blacklist"));
    private final StringSetting blockIds = add(new StringSetting("block_ids", "Platform block IDs", "minecraft:stone,minecraft:cobblestone,minecraft:obsidian"));
    private final BooleanSetting useOffhand = add(new BooleanSetting("use_offhand", "Use offhand materials", true));
    private final BooleanSetting restoreSlot = add(new BooleanSetting("restore_slot", "Restore selected hotbar slot", true));
    private final BooleanSetting lineOfSight = add(new BooleanSetting("line_of_sight", "Require visible support face", false));
    private final FloatSetting placeRange = add(new FloatSetting("place_range", "Placement reach", 4.5F, 2F, 6F));
    private final FloatSetting interval = add(new FloatSetting("interval_ticks", "Ticks between actions", 4F, 1F, 20F));
    private final FloatSetting igniteTimeout = add(new FloatSetting("ignite_timeout_ticks", "Portal ignition timeout", 60F, 20F, 200F));
    private ClientLevel observedLevel;
    private LocalPlayer observedPlayer;
    private String observedMode;
    private BlockPos anchor;
    private Direction forward;
    private Direction width;
    private int startupDelay;
    private int cooldown;
    private int settledTicks;
    private int stalledTicks;
    private int igniteTicks;
    private boolean stallNotice;

    public AutoBuild() {
        super("auto_build", "AutoBuild", "Automatically build a platform or a lit nether portal, then turn off.", Category.WORLD);
    }

    @Override public void tick(Minecraft client) {
        if (client.gameMode == null || client.gui.screen() != null || !client.player.isAlive()
            || client.player.isSpectator() || client.player.isHandsBusy() || client.player.isUsingItem()
            || client.options.keyUse.isDown() || client.options.keyAttack.isDown()) return;
        if (observedPlayer != client.player || observedLevel != client.level || !mode.get().equals(observedMode)) {
            observedPlayer = client.player;
            observedLevel = client.level;
            observedMode = mode.get();
            anchor = null;
            startupDelay = 20;
            cooldown = 0;
            settledTicks = 0;
            stalledTicks = 0;
            igniteTicks = 0;
            stallNotice = false;
            return;
        }
        if (startupDelay > 0) { startupDelay--; return; }
        if (anchor == null) {
            if (mode.get().equals("Portal")) {
                if (!beginPortal(client)) return;
            } else {
                anchor = client.player.blockPosition().below();
            }
        }
        if (mode.get().equals("Platform") && disableOnYChange.get()
            && Math.abs(client.player.getY() - (anchor.getY() + 1.0)) > 0.4) {
            stop(client, "Stopped because your height changed", false);
            return;
        }
        if (cooldown > 0) { cooldown--; return; }
        if (mode.get().equals("Portal")) buildPortal(client);
        else buildPlatform(client);
    }

    private void buildPlatform(Minecraft client) {
        int radius = (int) platformRadius.get();
        List<BlockPos> missing = new ArrayList<>();
        for (int x = -radius; x <= radius; x++)
            for (int z = -radius; z <= radius; z++) {
                BlockPos pos = anchor.offset(x, 0, z);
                if (client.level.getBlockState(pos).canBeReplaced()) missing.add(pos);
            }
        if (missing.isEmpty()) {
            if (++settledTicks >= 10) stop(client, "Platform complete", true);
            return;
        }
        settledTicks = 0;
        if (!hasMaterial(client.player)) {
            stop(client, "No allowed platform blocks in hotbar or offhand", false);
            return;
        }
        missing.sort(Comparator.comparingInt((BlockPos pos) ->
            Math.abs(pos.getX() - anchor.getX()) + Math.abs(pos.getZ() - anchor.getZ())));
        for (BlockPos target : missing) {
            BlockHitResult hit = support(client, target);
            if (hit == null) continue;
            if (place(client, hit)) {
                cooldown = (int) interval.get();
                stalledTicks = 0;
                return;
            }
        }
        waitForReach(client);
    }

    private boolean beginPortal(Minecraft client) {
        LocalPlayer player = client.player;
        forward = player.getDirection();
        width = forward.getClockWise();
        BlockPos base = player.blockPosition();
        for (int distance = 2; distance <= 3; distance++)
            for (int lateral : new int[] {0, -1, 1, -2, 2}) {
                BlockPos candidate = base.relative(forward, distance).relative(width, lateral);
                if (!validPortalSite(client, candidate)) continue;
                anchor = candidate;
                int missing = 0;
                for (BlockPos pos : portalFrame()) if (!client.level.getBlockState(pos).is(Blocks.OBSIDIAN)) missing++;
                if (countItem(player, Items.OBSIDIAN) < missing || countItem(player, Items.FLINT_AND_STEEL) < 1) {
                    stop(client, "Need " + missing + " obsidian and flint and steel in hotbar or offhand", false);
                    return false;
                }
                return true;
            }
        stop(client, "No clear portal site with solid ground nearby", false);
        return false;
    }

    private boolean validPortalSite(Minecraft client, BlockPos candidate) {
        for (int u = -1; u <= 2; u++) {
            BlockPos floor = candidate.relative(width, u).below();
            if (!client.level.getBlockState(floor).isFaceSturdy(client.level, floor, Direction.UP)) return false;
            for (int y = 0; y <= 4; y++) {
                BlockPos pos = candidate.relative(width, u).above(y);
                BlockState state = client.level.getBlockState(pos);
                boolean frame = u == -1 || u == 2 || y == 0 || y == 4;
                if (frame ? !state.is(Blocks.OBSIDIAN) && !state.canBeReplaced()
                    : !state.isAir() && !state.is(Blocks.FIRE) && !state.is(Blocks.NETHER_PORTAL)) return false;
            }
        }
        return true;
    }

    private List<BlockPos> portalFrame() {
        List<BlockPos> frame = new ArrayList<>(14);
        for (int y = 0; y <= 4; y++)
            for (int u = -1; u <= 2; u++)
                if (u == -1 || u == 2 || y == 0 || y == 4)
                    frame.add(anchor.relative(width, u).above(y));
        return frame;
    }

    private void buildPortal(Minecraft client) {
        if (client.level.getBlockState(anchor.above()).is(Blocks.NETHER_PORTAL)) {
            if (++settledTicks >= 10) stop(client, "Portal complete and lit", true);
            return;
        }
        settledTicks = 0;
        for (BlockPos target : portalFrame()) {
            if (client.level.getBlockState(target).is(Blocks.OBSIDIAN)) continue;
            if (!client.level.getBlockState(target).canBeReplaced()) {
                stop(client, "Portal frame site became obstructed", false);
                return;
            }
            if (countItem(client.player, Items.OBSIDIAN) == 0) {
                stop(client, "Ran out of obsidian", false);
                return;
            }
            BlockHitResult hit = support(client, target);
            if (hit == null) continue;
            if (placeItem(client, hit, Items.OBSIDIAN)) {
                cooldown = (int) interval.get();
                stalledTicks = 0;
                return;
            }
        }
        for (BlockPos target : portalFrame())
            if (!client.level.getBlockState(target).is(Blocks.OBSIDIAN)) {
                waitForReach(client);
                return;
            }
        if (++igniteTicks > (int) igniteTimeout.get()) {
            stop(client, "Portal could not be lit in time", false);
            return;
        }
        BlockPos inner = anchor.above();
        if (!client.level.getBlockState(inner).isAir()) return;
        BlockPos base = anchor;
        Vec3 hitPos = Vec3.atCenterOf(base).add(0, 0.5, 0);
        if (client.player.getEyePosition().distanceToSqr(hitPos) > effectiveRange(client.player) * effectiveRange(client.player)) return;
        if (placeItem(client, new BlockHitResult(hitPos, Direction.UP, base, false), Items.FLINT_AND_STEEL))
            cooldown = (int) interval.get();
    }

    private BlockHitResult support(Minecraft client, BlockPos target) {
        if (!client.level.getBlockState(target).canBeReplaced()
            || !client.level.getFluidState(target).isEmpty()) return null;
        for (Direction direction : SUPPORTS) {
            BlockPos block = target.relative(direction);
            Direction face = direction.getOpposite();
            if (!client.level.getBlockState(block).isFaceSturdy(client.level, block, face)) continue;
            Vec3 hit = Vec3.atCenterOf(block).add(face.getStepX() * 0.5,
                face.getStepY() * 0.5, face.getStepZ() * 0.5);
            if (client.player.getEyePosition().distanceToSqr(hit) > effectiveRange(client.player) * effectiveRange(client.player)) continue;
            if (lineOfSight.get()) {
                Vec3 inside = hit.add(-face.getStepX() * 0.01, -face.getStepY() * 0.01, -face.getStepZ() * 0.01);
                HitResult ray = client.level.clip(new ClipContext(client.player.getEyePosition(), inside,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, client.player));
                if (!(ray instanceof BlockHitResult seen)
                    || !seen.getBlockPos().equals(block) || seen.getDirection() != face) continue;
            }
            return new BlockHitResult(hit, face, block, false);
        }
        return null;
    }

    private double effectiveRange(LocalPlayer player) { return Math.min(placeRange.get(), player.blockInteractionRange()); }

    private boolean place(Minecraft client, BlockHitResult hit) {
        int oldSlot = client.player.getInventory().getSelectedSlot();
        InteractionHand hand = selectMaterial(client.player);
        return interact(client, hit, hand, oldSlot);
    }

    private boolean placeItem(Minecraft client, BlockHitResult hit, Item item) {
        int oldSlot = client.player.getInventory().getSelectedSlot();
        InteractionHand hand = selectItem(client.player, item);
        return interact(client, hit, hand, oldSlot);
    }

    private boolean interact(Minecraft client, BlockHitResult hit, InteractionHand hand, int oldSlot) {
        if (hand == null) return false;
        try {
            ItemStack stack = client.player.getItemInHand(hand);
            InteractionResult result = client.gameMode.useItemOn(client.player, hand, hit);
            if (result.consumesAction()) {
                client.player.swing(hand, stack.getInteractAnimation(), false);
                client.player.itemUsed(hand);
                return true;
            }
            return false;
        } finally {
            if (hand == InteractionHand.MAIN_HAND && restoreSlot.get()
                && client.player.getInventory().getSelectedSlot() != oldSlot) {
                client.player.getInventory().setSelectedSlot(oldSlot);
                client.player.connection.send(new ServerboundSetCarriedItemPacket(oldSlot));
            }
        }
    }

    private InteractionHand selectMaterial(LocalPlayer player) {
        if (useOffhand.get() && usablePlatform(player, player.getOffhandItem())) return InteractionHand.OFF_HAND;
        if (usablePlatform(player, player.getMainHandItem())) return InteractionHand.MAIN_HAND;
        for (int slot = 0; slot < 9; slot++)
            if (usablePlatform(player, player.getInventory().getItem(slot))) {
                player.getInventory().setSelectedSlot(slot);
                return InteractionHand.MAIN_HAND;
            }
        return null;
    }

    private boolean hasMaterial(LocalPlayer player) {
        if (useOffhand.get() && usablePlatform(player, player.getOffhandItem())) return true;
        for (int slot = 0; slot < 9; slot++)
            if (usablePlatform(player, player.getInventory().getItem(slot))) return true;
        return false;
    }

    private boolean usablePlatform(LocalPlayer player, ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) return false;
        var block = blockItem.getBlock();
        if (block instanceof FallingBlock || block instanceof EntityBlock
            || !block.defaultBlockState().isCollisionShapeFullBlock(player.level(), BlockPos.ZERO)) return false;
        Set<String> ids = StorageMenu.ids(blockIds.get());
        boolean listed = ids.contains(BuiltInRegistries.BLOCK.getKey(block).toString());
        return filterMode.get().equals("Whitelist") == listed;
    }

    private InteractionHand selectItem(LocalPlayer player, Item item) {
        if (useOffhand.get() && player.getOffhandItem().is(item)) return InteractionHand.OFF_HAND;
        if (player.getMainHandItem().is(item)) return InteractionHand.MAIN_HAND;
        for (int slot = 0; slot < 9; slot++)
            if (player.getInventory().getItem(slot).is(item)) {
                player.getInventory().setSelectedSlot(slot);
                return InteractionHand.MAIN_HAND;
            }
        return null;
    }

    private int countItem(LocalPlayer player, Item item) {
        int count = useOffhand.get() && player.getOffhandItem().is(item) ? player.getOffhandItem().getCount() : 0;
        for (int slot = 0; slot < 9; slot++)
            if (player.getInventory().getItem(slot).is(item)) count += player.getInventory().getItem(slot).getCount();
        return count;
    }

    private void waitForReach(Minecraft client) {
        if (++stalledTicks >= 40 && !stallNotice) {
            client.gui.hud.getChat().addClientSystemMessage(
                Component.literal("[Anomoly] AutoBuild: Move closer to the remaining blocks.").withStyle(ChatFormatting.YELLOW));
            stallNotice = true;
        }
    }

    private void stop(Minecraft client, String message, boolean success) {
        client.gui.hud.getChat().addClientSystemMessage(Component.literal("[Anomoly] AutoBuild: " + message + ".")
            .withStyle(success ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
        setEnabled(false);
    }

    @Override protected void onEnable(Minecraft client) { reset(); }
    @Override protected void onDisable(Minecraft client) { reset(); }
    private void reset() {
        observedPlayer = null;
        observedLevel = null;
        observedMode = null;
        anchor = null;
        forward = null;
        width = null;
        startupDelay = 0;
        cooldown = 0;
        settledTicks = 0;
        stalledTicks = 0;
        igniteTicks = 0;
        stallNotice = false;
    }
}
