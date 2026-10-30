package dev.anomoly.modules;

import dev.anomoly.Module;
import java.util.StringJoiner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

public final class NameCollector extends Module {
    public NameCollector() {
        super("name_collector", "NameCollector", "One-shot: copy the names of every player on the tab list to the clipboard, then switch off.", Category.EXPLOIT);
    }

    @Override protected void onEnable(Minecraft client) {
        if (client.player == null) return;
        StringJoiner names = new StringJoiner("\n");
        for (PlayerInfo info : client.player.connection.getOnlinePlayers()) names.add(info.getProfile().name());
        client.keyboardHandler.setClipboard(names.toString());
        client.player.sendSystemMessage(Component.literal("[Anomoly] Copied " + client.player.connection.getOnlinePlayers().size() + " player names."));
        setEnabled(false);
    }
}
