package dev.anomoly.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

public final class MurderMystery extends EntityEsp {
    public MurderMystery() {
        super("murder_mystery", "MurderMystery", "Outline players holding a sword or bow through walls to spot the murderer and detective.");
    }

    @Override protected boolean matches(Entity entity) {
        if (!(entity instanceof Player player) || player == Minecraft.getInstance().player) return false;
        var hand = player.getMainHandItem();
        return hand.is(ItemTags.SWORDS) || hand.is(Items.BOW) || hand.is(Items.CROSSBOW);
    }
}
