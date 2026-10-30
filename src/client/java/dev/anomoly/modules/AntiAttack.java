package dev.anomoly.modules;

import dev.anomoly.Module;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;

public final class AntiAttack extends Module {
    public AntiAttack() {
        super("anti_attack", "AntiAttack", "Refuse to attack your own tamed animals, even by accident.", Category.PLAYER);
    }

    public boolean spares(Player player, Entity target) {
        return enabled() && target instanceof OwnableEntity pet && pet.getOwnerReference() != null
            && player.getUUID().equals(pet.getOwnerReference().getUUID());
    }
}
