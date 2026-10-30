package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ArmorStandItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Supplies a normal block interaction at the aimed air position on right click. */
public final class AirPlace extends Module {
    private final BooleanSetting customRange = add(new BooleanSetting("custom_range", "Use fixed placement range", false));
    private final FloatSetting range = add(new FloatSetting("range", "Fixed placement range", 4.5F, 1F, 6F));
    private final BooleanSetting blocks = add(new BooleanSetting("blocks", "Place blocks", true));
    private final BooleanSetting spawnEggs = add(new BooleanSetting("spawn_eggs", "Use spawn eggs", true));
    private final BooleanSetting armorStands = add(new BooleanSetting("armor_stands", "Place armor stands", true));
    private final BooleanSetting fireworks = add(new BooleanSetting("fireworks", "Launch firework rockets", true));

    public AirPlace() {
        super("air_place", "AirPlace", "Place blocks and supported items at the aimed air position with a normal right click.", Category.WORLD);
    }

    /** Called only when vanilla is about to process a use-key press. */
    public BlockHitResult clickTarget(Minecraft client) {
        if (!enabled() || client.player == null || client.level == null || client.gui.screen() != null
            || client.player.isSpectator() || client.player.isHandsBusy()
            || client.hitResult == null || client.hitResult.getType() != HitResult.Type.MISS
            || !usable(client.player.getMainHandItem()) && !usable(client.player.getOffhandItem())) return null;
        double maxReach = Math.max(1.0, client.player.blockInteractionRange() - 0.25);
        double distance = customRange.get() ? Math.min(range.get(), maxReach) : maxReach;
        HitResult ray = client.player.pick(distance, 1.0F, false);
        if (ray.getType() != HitResult.Type.MISS) return null;
        BlockPos pos = BlockPos.containing(ray.getLocation());
        if (!client.level.getWorldBorder().isWithinBounds(pos)
            || !client.level.getBlockState(pos).canBeReplaced()
            || !client.level.getFluidState(pos).isEmpty()) return null;
        Direction face = client.player.getDirection().getOpposite();
        return new BlockHitResult(Vec3.atCenterOf(pos), face, pos, false);
    }

    private boolean usable(ItemStack stack) {
        Item item = stack.getItem();
        return blocks.get() && item instanceof BlockItem
            || spawnEggs.get() && item instanceof SpawnEggItem
            || armorStands.get() && item instanceof ArmorStandItem
            || fireworks.get() && item instanceof FireworkRocketItem;
    }
}
