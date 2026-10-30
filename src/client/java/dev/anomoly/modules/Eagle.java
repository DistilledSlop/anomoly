package dev.anomoly.modules;

import dev.anomoly.AnomolyClient;
import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.FloatSetting;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.AABB;

/** Uses ordinary sneak input when a player approaches an unsupported edge. */
public final class Eagle extends Module {
    private final FloatSetting edgeMin = add(new FloatSetting("edge_distance_min", "Minimum edge distance", 0.4F, 0.01F, 1.3F));
    private final FloatSetting edgeMax = add(new FloatSetting("edge_distance_max", "Maximum edge distance", 0.6F, 0.01F, 1.3F));
    private final BooleanSetting onGround = add(new BooleanSetting("on_ground", "Only while on ground", true));
    private final BooleanSetting holdingBlocks = add(new BooleanSetting("holding_blocks", "Only while holding blocks", false));
    private final BooleanSetting forward = add(new BooleanSetting("forward", "Require forward input", false));
    private final BooleanSetting backward = add(new BooleanSetting("backward", "Require backward input", false));
    private final BooleanSetting left = add(new BooleanSetting("left", "Require left input", false));
    private final BooleanSetting right = add(new BooleanSetting("right", "Require right input", false));
    private final FloatSetting pitchMin = add(new FloatSetting("pitch_min", "Minimum look pitch", -90F, -90F, 90F));
    private final FloatSetting pitchMax = add(new FloatSetting("pitch_max", "Maximum look pitch", 90F, -90F, 90F));
    private boolean lastEdge;
    private float edgeDistance;

    public Eagle() {
        super("eagle", "Eagle", "Sneak near unsupported edges for manual bridging.", Category.PLAYER);
    }

    public void apply(KeyboardInput input) {
        if (!enabled()) return;
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (AnomolyClient.MODULES.get(AutoSneak.class).enabled()
            || player == null || client.level == null || client.gui.screen() != null
            || !player.isAlive() || player.isSpectator() || player.isPassenger()
            || onGround.get() && !player.onGround()
            || holdingBlocks.get() && !(player.getMainHandItem().getItem() instanceof BlockItem)
                && !(player.getOffhandItem().getItem() instanceof BlockItem)
            || forward.get() && !client.options.keyUp.isDown()
            || backward.get() && !client.options.keyDown.isDown()
            || left.get() && !client.options.keyLeft.isDown()
            || right.get() && !client.options.keyRight.isDown()
            || player.getXRot() < pitchMin.get() || player.getXRot() > pitchMax.get()) {
            lastEdge = false;
            return;
        }
        if (!lastEdge) {
            float low = Math.min(edgeMin.get(), edgeMax.get());
            float high = Math.max(edgeMin.get(), edgeMax.get());
            edgeDistance = low + (high - low) * ThreadLocalRandom.current().nextFloat();
        }
        boolean edge = nearEdge(client, player, edgeDistance);
        if (edge) {
            Input current = input.keyPresses;
            input.keyPresses = new Input(current.forward(), current.backward(), current.left(), current.right(),
                current.jump(), true, current.sprint());
        }
        lastEdge = edge;
    }

    private static boolean nearEdge(Minecraft client, LocalPlayer player, float distance) {
        AABB box = player.getBoundingBox();
        int y = Mth.floor(box.minY - 0.05);
        for (int x = Mth.floor(box.minX + 0.001); x <= Mth.floor(box.maxX - 0.001); x++)
            for (int z = Mth.floor(box.minZ + 0.001); z <= Mth.floor(box.maxZ - 0.001); z++)
                if (nearEdgeOfBlock(client, player, new BlockPos(x, y, z), distance)) return true;
        return false;
    }

    private static boolean nearEdgeOfBlock(Minecraft client, LocalPlayer player, BlockPos support, float distance) {
        if (client.level.getBlockState(support).getCollisionShape(client.level, support).isEmpty()) return false;
        double west = player.getX() - support.getX();
        double east = support.getX() + 1.0 - player.getX();
        double north = player.getZ() - support.getZ();
        double south = support.getZ() + 1.0 - player.getZ();
        for (Direction side : new Direction[] {Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH}) {
            double edge = switch (side) {
                case WEST -> west;
                case EAST -> east;
                case NORTH -> north;
                case SOUTH -> south;
                default -> Double.POSITIVE_INFINITY;
            };
            if (edge > distance) continue;
            BlockPos beside = support.relative(side);
            if (client.level.getBlockState(beside).getCollisionShape(client.level, beside).isEmpty()) return true;
        }
        return false;
    }

    @Override protected void onDisable(Minecraft client) {
        lastEdge = false;
    }
}
