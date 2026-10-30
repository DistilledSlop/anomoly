package dev.anomoly.modules;

import dev.anomoly.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;

public final class Esp extends EntityEsp {
    private final BooleanSetting living = add(new BooleanSetting("living", "Players and mobs", true));
    private final BooleanSetting items = add(new BooleanSetting("items", "Items", false));
    private final BooleanSetting other = add(new BooleanSetting("other", "Other entities", false));

    public Esp() {
        super("esp", "ESP", "Outline selected kinds of entities through walls (also covers Wall Hack).");
    }

    @Override protected boolean matches(Entity entity) {
        if (entity == Minecraft.getInstance().player) return false;
        if (entity instanceof LivingEntity) return living.get();
        if (entity instanceof ItemEntity) return items.get();
        return other.get() && !(entity instanceof Player);
    }
}
