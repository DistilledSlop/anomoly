package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.StringSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class AutoEZ extends Module implements HitEvents.Listener {
    private final StringSetting message = add(new StringSetting("message", "Message ({player} is replaced)", "EZ"));
    private Player victim;
    private int age;

    public AutoEZ() {
        super("auto_ez", "AutoEZ", "Send a chat message after a player you recently hit dies.", Category.MISC);
    }

    @Override public void onHit(Entity target) {
        if (target instanceof Player player) {
            victim = player;
            age = 0;
        }
    }

    @Override public void tick(Minecraft client) {
        if (victim == null) return;
        if (victim.isDeadOrDying()) {
            client.player.connection.sendChat(message.get().replace("{player}", victim.getName().getString()));
            victim = null;
        } else if (++age > 100) victim = null;
    }
}
