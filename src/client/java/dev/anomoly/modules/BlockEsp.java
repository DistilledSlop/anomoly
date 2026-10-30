package dev.anomoly.modules;

import dev.anomoly.setting.StringSetting;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;

public final class BlockEsp extends BlockEspBase {
    private final StringSetting blocks = add(new StringSetting("blocks", "Block IDs (comma separated)", "minecraft:diamond_ore,minecraft:deepslate_diamond_ore,minecraft:ancient_debris"));
    private String parsedFrom = "";
    private final Set<String> ids = new HashSet<>();

    public BlockEsp() {
        super("block_esp", "BlockESP", "Outline selected block types through walls (also covers Search).", Category.RENDER, 0xFF55FFFF);
    }

    @Override protected boolean matches(Minecraft client, BlockPos pos, BlockState state) {
        if (!parsedFrom.equals(blocks.get())) {
            parsedFrom = blocks.get();
            ids.clear();
            for (String id : parsedFrom.split(",")) ids.add(id.trim().toLowerCase(java.util.Locale.ROOT));
        }
        return ids.contains(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
    }
}
