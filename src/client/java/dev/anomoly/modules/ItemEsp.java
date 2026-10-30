package dev.anomoly.modules;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;

public final class ItemEsp extends EntityEsp {
    public ItemEsp() {
        super("item_esp", "ItemESP", "Outline dropped items through walls.");
    }

    @Override protected boolean matches(Entity entity) { return entity instanceof ItemEntity; }
}
