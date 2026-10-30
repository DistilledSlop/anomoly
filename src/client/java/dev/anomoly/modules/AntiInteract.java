package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.StringSetting;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.phys.BlockHitResult;

/** Suppresses right-click interactions with selected container blocks. */
public final class AntiInteract extends Module {
    private final StringSetting blockIds = add(new StringSetting("block_ids", "Protected block IDs (comma separated)",
        "minecraft:chest,minecraft:trapped_chest,minecraft:ender_chest,minecraft:barrel,"
            + "minecraft:shulker_box,minecraft:hopper,minecraft:dispenser,minecraft:dropper,"
            + "minecraft:furnace,minecraft:blast_furnace,minecraft:smoker,minecraft:brewing_stand,minecraft:crafter"));
    private final BooleanSetting allowSneakPlacement = add(new BooleanSetting("allow_sneak_placement",
        "Allow placement while sneaking", true));

    public AntiInteract() {
        super("anti_interact", "AntiInteract", "Prevent right-clicking selected containers.", Category.WORLD);
    }

    public boolean blocksClick(Minecraft client, BlockHitResult hit) {
        if (!enabled() || client.player == null || client.level == null || client.player.isSpectator()) return false;
        if (allowSneakPlacement.get() && client.player.isSecondaryUseActive()
            && (!client.player.getMainHandItem().isEmpty() || !client.player.getOffhandItem().isEmpty())) return false;
        var block = client.level.getBlockState(hit.getBlockPos()).getBlock();
        Set<String> selected = Arrays.stream(blockIds.get().split(","))
            .map(String::trim).filter(value -> !value.isEmpty()).collect(Collectors.toSet());
        String id = BuiltInRegistries.BLOCK.getKey(block).toString();
        return selected.contains(id) || block instanceof ShulkerBoxBlock && selected.contains("minecraft:shulker_box");
    }
}
