package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.EntityHitResult;

public final class FeedAura extends Module {
    private final FloatSetting range = add(new FloatSetting("range", "Range", 4.0F, 2.0F, 6.0F));
    private final FloatSetting interval = add(new FloatSetting("interval_ticks", "Ticks between feeds", 10, 2, 60));
    private int cooldown;

    public FeedAura() {
        super("feed_aura", "FeedAura", "Feed any nearby animal that can breed using matching food from your hotbar.", Category.COMBAT);
    }

    @Override public void tick(Minecraft client) {
        var player = client.player;
        if (client.gameMode == null || client.gui.screen() != null || --cooldown > 0) return;
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof Animal animal) || animal.getAge() != 0 || !animal.canFallInLove()
                || player.distanceToSqr(animal) > range.get() * range.get()) continue;
            for (int slot = 0; slot < 9; slot++) {
                if (!animal.isFood(player.getInventory().getItem(slot))) continue;
                int previous = player.getInventory().getSelectedSlot();
                player.getInventory().setSelectedSlot(slot);
                player.connection.send(new ServerboundSetCarriedItemPacket(slot));
                client.gameMode.interact(player, animal, new EntityHitResult(animal, animal.getBoundingBox().getCenter()), InteractionHand.MAIN_HAND);
                player.getInventory().setSelectedSlot(previous);
                player.connection.send(new ServerboundSetCarriedItemPacket(previous));
                cooldown = (int)interval.get();
                return;
            }
        }
    }
}
