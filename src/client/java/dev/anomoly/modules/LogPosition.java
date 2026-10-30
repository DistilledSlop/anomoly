package dev.anomoly.modules;

import dev.anomoly.AnomolyClient;
import dev.anomoly.Module;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class LogPosition extends Module {
    private boolean wasDead;
    private Object lastDimension;

    public LogPosition() {
        super("log_position", "Log Position", "Log your coordinates to chat and the client log when you die or change dimension.", Category.MISC);
    }

    @Override protected void onEnable(Minecraft client) { wasDead = false; lastDimension = null; }

    @Override public void tick(Minecraft client) {
        var player = client.player;
        boolean dead = player.isDeadOrDying();
        Object dimension = client.level.dimension();
        String reason = dead && !wasDead ? "death" : lastDimension != null && !dimension.equals(lastDimension) ? "dimension change" : null;
        if (reason != null) {
            String text = String.format(Locale.ROOT, "Position at %s in %s: %.1f %.1f %.1f", reason, client.level.dimension().identifier(),
                player.getX(), player.getY(), player.getZ());
            player.sendSystemMessage(Component.literal("[Anomoly] " + text));
            AnomolyClient.LOGGER.info(text);
        }
        wasDead = dead;
        lastDimension = dimension;
    }
}
