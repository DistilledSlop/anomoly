package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.IntSetting;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public final class StashFinder extends Module {
    private final IntSetting threshold = add(new IntSetting("threshold", "Containers per chunk to report", 12, 3, 100));
    private final Set<ChunkPos> reported = new HashSet<>();
    private int ticks;

    public StashFinder() {
        super("stash_finder", "Stash Finder", "Report loaded chunks that hold many chests, barrels or shulker boxes (also covers StashLogger).", Category.WORLD);
    }

    @Override protected void onEnable(Minecraft client) { reported.clear(); ticks = 0; }

    @Override public void tick(Minecraft client) {
        if (ticks++ % 40 != 0) return;
        int radius = client.options.getEffectiveRenderDistance();
        ChunkPos origin = client.player.chunkPosition();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                ChunkPos pos = new ChunkPos(origin.x() + dx, origin.z() + dz);
                if (reported.contains(pos)) continue;
                LevelChunk chunk = client.level.getChunkSource().getChunk(pos.x(), pos.z(), ChunkStatus.FULL, false);
                if (chunk == null) continue;
                int containers = 0;
                for (BlockEntity entity : chunk.getBlockEntities().values()) {
                    if (entity instanceof ChestBlockEntity || entity instanceof BarrelBlockEntity || entity instanceof ShulkerBoxBlockEntity) containers++;
                }
                if (containers >= threshold.get()) {
                    reported.add(pos);
                    client.player.sendSystemMessage(Component.literal("[Anomoly] Stash: " + containers + " containers near "
                        + pos.getMiddleBlockX() + ", " + pos.getMiddleBlockZ()));
                }
            }
        }
    }
}
