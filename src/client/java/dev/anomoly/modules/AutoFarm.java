package dev.anomoly.modules;

import dev.anomoly.AnomolyClient;
import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.FloatSetting;
import dev.anomoly.setting.StringSetting;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Harvests mature crops, replants their seed, and optionally fertilizes growing plants. */
public final class AutoFarm extends Module {
    private final FloatSetting range = add(new FloatSetting("range", "Farm range", 4.5F, 2F, 6F));
    private final FloatSetting verticalRange = add(new FloatSetting("vertical_range", "Vertical scan range", 3F, 1F, 6F));
    private final FloatSetting interval = add(new FloatSetting("interval_ticks", "Ticks between actions", 3F, 1F, 20F));
    private final StringSetting preferredCrop = add(new StringSetting("preferred_crop", "Preferred empty farmland crop ID", "minecraft:wheat"));
    private final BooleanSetting replant = add(new BooleanSetting("replant", "Replant harvested and empty plots", true));
    private final BooleanSetting harvestColumns = add(new BooleanSetting("harvest_columns", "Harvest upper cane, cactus, bamboo, and kelp", true));
    private final BooleanSetting harvestFruit = add(new BooleanSetting("harvest_fruit", "Harvest melons and pumpkins", true));
    private final BooleanSetting harvestBerries = add(new BooleanSetting("harvest_berries", "Pick mature sweet berries", true));
    private final BooleanSetting boneMeal = add(new BooleanSetting("bone_meal", "Fertilize immature crops", false));
    private final BooleanSetting lineOfSight = add(new BooleanSetting("line_of_sight", "Require line of sight", true));
    private final BooleanSetting rotate = add(new BooleanSetting("rotate", "Face farm actions", true));
    private final BooleanSetting autoTool = add(new BooleanSetting("auto_tool", "Choose a suitable tool", true));
    private final BooleanSetting restoreSlot = add(new BooleanSetting("restore_slot", "Restore previous hotbar slot", true));
    private final Map<BlockPos, PlantIntent> plantIntents = new HashMap<>();
    private final Map<BlockPos, Long> retryAfter = new HashMap<>();
    private ClientLevel observedLevel;
    private LocalPlayer observedPlayer;
    private BlockPos miningPos;
    private Direction miningFace;
    private int miningTicks;
    private int originalSlot = -1;
    private int toolSlot = -1;
    private int cooldown;

    public AutoFarm() {
        super("auto_farm", "Auto Farm", "Harvest and replant crops, pick berries, and optionally use bone meal.", Category.WORLD);
    }

    @Override public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (observedPlayer != player || observedLevel != client.level) {
            stopMining(client);
            observedPlayer = player;
            observedLevel = client.level;
            plantIntents.clear();
            retryAfter.clear();
            cooldown = 20;
            return;
        }
        if (client.gameMode == null || client.gui.screen() != null || !player.isAlive()
            || player.isSpectator() || player.isHandsBusy() || player.isUsingItem()
            || client.options.keyAttack.isDown() || client.options.keyUse.isDown()
            || AnomolyClient.MODULES.get(AutoTunnel.class).enabled()
            || AnomolyClient.MODULES.get(Nuker.class).enabled()
            || AnomolyClient.MODULES.get(NukerLegit.class).enabled()
            || AnomolyClient.MODULES.get(VeinMiner.class).enabled()) {
            stopMining(client);
            return;
        }
        long now = client.level.getGameTime();
        retryAfter.entrySet().removeIf(entry -> entry.getValue() <= now);
        plantIntents.entrySet().removeIf(entry -> {
            BlockState current = client.level.getBlockState(entry.getKey());
            return !current.isAir() && !(seedFor(current) == entry.getValue().item() && mature(current));
        });
        if (miningPos != null) {
            if (client.level.getBlockState(miningPos).isAir()) {
                stopMining(client);
                cooldown = (int)interval.get();
            } else if (player.getEyePosition().distanceToSqr(Vec3.atCenterOf(miningPos))
                > effectiveRange(player) * effectiveRange(player)) stopMining(client);
            else { mine(client); return; }
        }
        if (cooldown > 0) { cooldown--; return; }
        Action action = findAction(client);
        if (action == null || faceTarget(player, action.hit().getLocation())) return;
        retryAfter.put(action.pos(), now + Math.max(8, (int)interval.get() * 2));
        if (action.kind() == Kind.BREAK) {
            miningPos = action.pos();
            miningFace = action.hit().getDirection();
            miningTicks = 0;
            originalSlot = player.getInventory().getSelectedSlot();
            toolSlot = autoTool.get() ? selectTool(player, client.level.getBlockState(miningPos)) : originalSlot;
            player.getInventory().setSelectedSlot(toolSlot);
            if (action.seed() != null && replant.get())
                plantIntents.put(action.pos(), new PlantIntent(action.seed(), action.cocoaSupport()));
            mine(client);
            return;
        }
        Item item = action.item();
        int oldSlot = player.getInventory().getSelectedSlot();
        InteractionHand hand = selectItem(player, item);
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

    private Action findAction(Minecraft client) {
        LocalPlayer player = client.player;
        BlockPos origin = player.blockPosition();
        int horizontal = (int)Math.ceil(effectiveRange(player));
        int vertical = (int)verticalRange.get();
        Action best = null;
        for (int x = origin.getX() - horizontal; x <= origin.getX() + horizontal; x++)
            for (int y = origin.getY() - vertical; y <= origin.getY() + vertical; y++)
                for (int z = origin.getZ() - horizontal; z <= origin.getZ() + horizontal; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (retryAfter.containsKey(pos)) continue;
                    Action action = classify(client, pos);
                    if (action == null || !visible(client, action.hit())) continue;
                    if (best == null || action.kind().priority < best.kind().priority
                        || action.kind() == best.kind() && action.distance() < best.distance()) best = action;
                }
        return best;
    }

    private Action classify(Minecraft client, BlockPos pos) {
        BlockState state = client.level.getBlockState(pos);
        LocalPlayer player = client.player;
        if (state.isAir()) return plantingAction(client, pos);
        if (state.is(Blocks.SWEET_BERRY_BUSH) && harvestBerries.get()
            && state.getValue(SweetBerryBushBlock.AGE) >= 2)
            return action(player, Kind.PICK, pos, pos, null, null, null);
        Item seed = seedFor(state);
        if (seed != null && mature(state))
            return action(player, Kind.BREAK, pos, pos, null, seed,
                state.is(Blocks.COCOA) ? state.getValue(CocoaBlock.FACING) : null);
        if (harvestFruit.get() && (state.is(Blocks.MELON) || state.is(Blocks.PUMPKIN)))
            return action(player, Kind.BREAK, pos, pos, null, null, null);
        if (harvestColumns.get() && upperColumn(client, pos, state))
            return action(player, Kind.BREAK, pos, pos, null, null, null);
        if (boneMeal.get() && immature(state) && hasItem(player, Items.BONE_MEAL))
            return action(player, Kind.FERTILIZE, pos, pos, Items.BONE_MEAL, null, null);
        return null;
    }

    private Action plantingAction(Minecraft client, BlockPos pos) {
        if (!replant.get()) return null;
        LocalPlayer player = client.player;
        PlantIntent intent = plantIntents.get(pos);
        if (intent != null && intent.cocoaSupport() != null) {
            BlockPos support = pos.relative(intent.cocoaSupport());
            if (client.level.getBlockState(support).is(BlockTags.SUPPORTS_COCOA)
                && hasItem(player, intent.item()))
                return action(player, Kind.PLANT, pos, support, intent.item(), null, intent.cocoaSupport());
            return null;
        }
        BlockState soil = client.level.getBlockState(pos.below());
        if (soil.is(Blocks.SOUL_SAND)) {
            if (hasItem(player, Items.NETHER_WART))
                return action(player, Kind.PLANT, pos, pos.below(), Items.NETHER_WART, null, null);
            return null;
        }
        if (!soil.is(Blocks.FARMLAND)) return null;
        Item desired = intent != null ? intent.item() : preferredSeed();
        if (!hasItem(player, desired) && intent == null) desired = firstSeed(player);
        if (!hasItem(player, desired)) return null;
        return action(player, Kind.PLANT, pos, pos.below(), desired, null, null);
    }

    private Action action(LocalPlayer player, Kind kind, BlockPos pos, BlockPos clicked,
                          Item item, Item seed, Direction cocoaSupport) {
        Direction face;
        Vec3 hit;
        if (kind == Kind.PLANT && cocoaSupport != null) {
            face = cocoaSupport.getOpposite();
            hit = Vec3.atCenterOf(clicked).add(face.getStepX() * 0.5,
                face.getStepY() * 0.5, face.getStepZ() * 0.5);
        } else if (kind == Kind.PLANT) {
            face = Direction.UP;
            hit = new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        } else {
            hit = Vec3.atCenterOf(pos);
            face = nearestFace(player.getEyePosition(), hit);
        }
        double distance = player.getEyePosition().distanceToSqr(hit);
        if (distance > effectiveRange(player) * effectiveRange(player)) return null;
        return new Action(kind, pos, new BlockHitResult(hit, face, clicked, false),
            item, seed, cocoaSupport, distance);
    }

    private boolean visible(Minecraft client, BlockHitResult hit) {
        if (!lineOfSight.get()) return true;
        HitResult ray = client.level.clip(new ClipContext(client.player.getEyePosition(), hit.getLocation(),
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, client.player));
        return ray.getType() == HitResult.Type.MISS
            || ray instanceof BlockHitResult block && block.getBlockPos().equals(hit.getBlockPos());
    }

    private static Item seedFor(BlockState state) {
        if (state.is(Blocks.WHEAT)) return Items.WHEAT_SEEDS;
        if (state.is(Blocks.CARROTS)) return Items.CARROT;
        if (state.is(Blocks.POTATOES)) return Items.POTATO;
        if (state.is(Blocks.BEETROOTS)) return Items.BEETROOT_SEEDS;
        if (state.is(Blocks.NETHER_WART)) return Items.NETHER_WART;
        if (state.is(Blocks.COCOA)) return Items.COCOA_BEANS;
        return null;
    }

    private static boolean mature(BlockState state) {
        if (state.getBlock() instanceof CropBlock crop) return crop.isMaxAge(state);
        if (state.is(Blocks.NETHER_WART)) return state.getValue(NetherWartBlock.AGE) == NetherWartBlock.MAX_AGE;
        if (state.is(Blocks.COCOA)) return state.getValue(CocoaBlock.AGE) == CocoaBlock.MAX_AGE;
        return false;
    }

    private static boolean immature(BlockState state) {
        if (seedFor(state) == null && !state.is(Blocks.SWEET_BERRY_BUSH)) return false;
        if (state.getBlock() instanceof CropBlock crop) return !crop.isMaxAge(state);
        if (state.is(Blocks.COCOA)) return state.getValue(CocoaBlock.AGE) < CocoaBlock.MAX_AGE;
        if (state.is(Blocks.SWEET_BERRY_BUSH)) return state.getValue(SweetBerryBushBlock.AGE) < 3;
        return false;
    }

    private static boolean upperColumn(Minecraft client, BlockPos pos, BlockState state) {
        BlockState below = client.level.getBlockState(pos.below());
        if (state.is(Blocks.SUGAR_CANE)) return below.is(Blocks.SUGAR_CANE);
        if (state.is(Blocks.CACTUS)) return below.is(Blocks.CACTUS);
        if (state.is(Blocks.BAMBOO)) return below.is(Blocks.BAMBOO) || below.is(Blocks.BAMBOO_SAPLING);
        if (state.is(Blocks.KELP) || state.is(Blocks.KELP_PLANT))
            return below.is(Blocks.KELP) || below.is(Blocks.KELP_PLANT);
        return false;
    }

    private Item preferredSeed() {
        return switch (preferredCrop.get().trim()) {
            case "minecraft:carrots", "minecraft:carrot", "carrot", "carrots" -> Items.CARROT;
            case "minecraft:potatoes", "minecraft:potato", "potato", "potatoes" -> Items.POTATO;
            case "minecraft:beetroots", "minecraft:beetroot", "beetroot", "beetroots" -> Items.BEETROOT_SEEDS;
            default -> Items.WHEAT_SEEDS;
        };
    }

    private Item firstSeed(LocalPlayer player) {
        for (Item seed : new Item[] {Items.WHEAT_SEEDS, Items.CARROT, Items.POTATO, Items.BEETROOT_SEEDS})
            if (hasItem(player, seed)) return seed;
        return Items.WHEAT_SEEDS;
    }

    private boolean hasItem(LocalPlayer player, Item item) {
        if (item == null) return false;
        if (player.getMainHandItem().is(item) || player.getOffhandItem().is(item)) return true;
        for (int slot = 0; slot < 9; slot++)
            if (player.getInventory().getItem(slot).is(item)) return true;
        return false;
    }

    private InteractionHand selectItem(LocalPlayer player, Item item) {
        if (item == null || player.getMainHandItem().is(item)) return InteractionHand.MAIN_HAND;
        if (player.getOffhandItem().is(item)) return InteractionHand.OFF_HAND;
        for (int slot = 0; slot < 9; slot++)
            if (player.getInventory().getItem(slot).is(item)) {
                player.getInventory().setSelectedSlot(slot);
                return InteractionHand.MAIN_HAND;
            }
        return null;
    }

    private void mine(Minecraft client) {
        if (miningPos == null) return;
        if (miningTicks == 0 && !client.gameMode.startDestroyBlock(miningPos, miningFace)) {
            stopMining(client);
            return;
        }
        if (!client.level.getBlockState(miningPos).isAir())
            client.gameMode.continueDestroyBlock(miningPos, miningFace);
        if (miningTicks++ % 4 == 0)
            client.player.swing(InteractionHand.MAIN_HAND,
                client.player.getMainHandItem().getInteractAnimation(), false);
        if (client.level.getBlockState(miningPos).isAir() || miningTicks > 100) {
            stopMining(client);
            cooldown = (int)interval.get();
        }
    }

    private void stopMining(Minecraft client) {
        if (miningPos != null && client.gameMode != null && client.player != null)
            client.gameMode.stopDestroyBlock();
        if (originalSlot >= 0 && restoreSlot.get() && observedPlayer == client.player
            && client.player != null && client.player.getInventory().getSelectedSlot() == toolSlot) {
            client.player.getInventory().setSelectedSlot(originalSlot);
            client.player.connection.send(new ServerboundSetCarriedItemPacket(originalSlot));
        }
        miningPos = null;
        miningFace = null;
        miningTicks = 0;
        originalSlot = -1;
        toolSlot = -1;
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

    private boolean faceTarget(LocalPlayer player, Vec3 hit) {
        if (!rotate.get()) return false;
        Vec3 direction = hit.subtract(player.getEyePosition());
        float yaw = (float)(Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0);
        float pitch = (float)-Math.toDegrees(Math.atan2(direction.y,
            Math.hypot(direction.x, direction.z)));
        boolean changed = Math.abs(Mth.wrapDegrees(yaw - player.getYRot())) > 0.5F
            || Math.abs(pitch - player.getXRot()) > 0.5F;
        player.setYRot(yaw);
        player.setYHeadRot(yaw);
        player.setXRot(Mth.clamp(pitch, -90F, 90F));
        return changed;
    }

    private double effectiveRange(LocalPlayer player) { return Math.min(range.get(), player.blockInteractionRange()); }

    private static Direction nearestFace(Vec3 eyes, Vec3 center) {
        Vec3 d = eyes.subtract(center);
        double ax = Math.abs(d.x), ay = Math.abs(d.y), az = Math.abs(d.z);
        if (ay >= ax && ay >= az) return d.y >= 0 ? Direction.UP : Direction.DOWN;
        if (ax >= az) return d.x >= 0 ? Direction.EAST : Direction.WEST;
        return d.z >= 0 ? Direction.SOUTH : Direction.NORTH;
    }

    @Override protected void onDisable(Minecraft client) {
        stopMining(client);
        observedPlayer = null;
        observedLevel = null;
        plantIntents.clear();
        retryAfter.clear();
        cooldown = 0;
    }

    private enum Kind {
        PLANT(0), BREAK(1), PICK(1), FERTILIZE(3);
        final int priority;
        Kind(int priority) { this.priority = priority; }
    }
    private record PlantIntent(Item item, Direction cocoaSupport) { }
    private record Action(Kind kind, BlockPos pos, BlockHitResult hit, Item item,
                          Item seed, Direction cocoaSupport, double distance) { }
}
