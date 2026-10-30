package dev.anomoly.modules;

import dev.anomoly.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;

public final class EntityOwner extends Module {
    public EntityOwner() {
        super("entity_owner", "Entity Owner", "Label tamed animals with their owner's name (also covers MobOwners).", Category.RENDER);
    }

    @Override public void tick(Minecraft client) {
        WorldGizmos.draw(client, () -> {
            for (Entity entity : client.level.entitiesForRendering()) {
                if (!(entity instanceof OwnableEntity pet) || pet.getOwnerReference() == null) continue;
                PlayerInfo owner = client.getConnection().getPlayerInfo(pet.getOwnerReference().getUUID());
                String name = owner == null ? "owner offline" : owner.getProfile().name();
                Gizmos.billboardTextOverMob(entity, 1, "Owner: " + name, 0xFFAAAAFF, 0.5F);
            }
        });
    }
}
