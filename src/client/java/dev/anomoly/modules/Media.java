package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.StringSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

public final class Media extends Module implements ChatFilter {
    private final StringSetting alias = add(new StringSetting("alias", "Name shown for other players", "Player"));

    public Media() {
        super("media", "Media", "Replace other players' names in incoming chat with an alias, for recording without showing names.", Category.MISC);
    }

    @Override public Component onIncoming(Component message) {
        Minecraft client = Minecraft.getInstance();
        String text = message.getString();
        String result = text;
        for (PlayerInfo info : client.player.connection.getOnlinePlayers()) {
            String name = info.getProfile().name();
            if (!name.equals(client.player.getName().getString()) && result.contains(name)) result = result.replace(name, alias.get());
        }
        return result.equals(text) ? message : Component.literal(result);
    }
}
