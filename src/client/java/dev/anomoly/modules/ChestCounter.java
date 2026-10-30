package dev.anomoly.modules;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public final class ChestCounter extends TextHud {
    private int ticks;
    private int chests, barrels, shulkers;

    public ChestCounter() {
        super("chest_counter", "ChestCounter", "Show how many chests, barrels and shulker boxes are in the loaded chunks around you.");
    }

    @Override public void tick(Minecraft client) {
        if (ticks++ % 40 != 0) return;
        chests = barrels = shulkers = 0;
        ChunkPos origin = client.player.chunkPosition();
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                LevelChunk chunk = client.level.getChunkSource().getChunk(origin.x() + dx, origin.z() + dz, ChunkStatus.FULL, false);
                if (chunk == null) continue;
                for (BlockEntity entity : chunk.getBlockEntities().values()) {
                    if (entity instanceof ChestBlockEntity) chests++;
                    else if (entity instanceof BarrelBlockEntity) barrels++;
                    else if (entity instanceof ShulkerBoxBlockEntity) shulkers++;
                }
            }
        }
    }

    @Override protected List<String> lines(Minecraft client) {
        return List.of("Chests " + chests + "  Barrels " + barrels + "  Shulkers " + shulkers);
    }
}
