package dev.anomoly.modules;

import dev.anomoly.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;

public final class ItemTags extends Module {
    public ItemTags() {
        super("item_tags", "ItemTags", "Label dropped items with their name and stack size.", Category.RENDER);
    }

    @Override public void tick(Minecraft client) {
        WorldGizmos.draw(client, () -> {
            for (Entity entity : client.level.entitiesForRendering()) {
                if (entity instanceof ItemEntity item && client.player.distanceToSqr(item) < 48 * 48) {
                    var stack = item.getItem();
                    Gizmos.billboardTextOverMob(item, 0, stack.getHoverName().getString() + " x" + stack.getCount(), 0xFFFFFFFF, 0.5F);
                }
            }
        });
    }
}
