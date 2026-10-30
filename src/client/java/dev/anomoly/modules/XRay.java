package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.StringSetting;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;

public final class XRay extends Module {
    private final StringSetting blocks = add(new StringSetting("blocks", "Visible block IDs (comma separated)",
        "minecraft:coal_ore,minecraft:iron_ore,minecraft:copper_ore,minecraft:gold_ore,minecraft:redstone_ore,minecraft:lapis_ore,"
            + "minecraft:diamond_ore,minecraft:emerald_ore,minecraft:deepslate_coal_ore,minecraft:deepslate_iron_ore,minecraft:deepslate_copper_ore,"
            + "minecraft:deepslate_gold_ore,minecraft:deepslate_redstone_ore,minecraft:deepslate_lapis_ore,minecraft:deepslate_diamond_ore,"
            + "minecraft:deepslate_emerald_ore,minecraft:nether_gold_ore,minecraft:nether_quartz_ore,minecraft:ancient_debris,"
            + "minecraft:chest,minecraft:spawner,minecraft:lava,minecraft:water"));
    private final Set<String> visible = new HashSet<>();
    private String parsedFrom = "";

    public XRay() {
        super("xray", "XRay", "Hide most terrain and keep only selected blocks visible so ores show through the ground.", Category.RENDER);
    }

    /** Whether a block face should be drawn; consulted from the chunk mesher while XRay is on. */
    public boolean shows(BlockState state) {
        if (!parsedFrom.equals(blocks.get())) {
            parsedFrom = blocks.get();
            visible.clear();
            for (String id : parsedFrom.split(",")) visible.add(id.trim().toLowerCase(Locale.ROOT));
        }
        return visible.contains(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
    }

    @Override protected void onEnable(Minecraft client) { rebuild(client); }

    @Override protected void onDisable(Minecraft client) { rebuild(client); }

    private static void rebuild(Minecraft client) {
        if (client.level == null) return;
        client.levelRenderer.invalidateCompiledGeometry(client.level, client.options, client.gameRenderer.mainCamera(), client.getBlockColors());
    }
}
