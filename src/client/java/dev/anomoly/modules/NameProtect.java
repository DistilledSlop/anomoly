package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.StringSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class NameProtect extends Module implements ChatFilter {
    private final StringSetting alias = add(new StringSetting("alias", "Replacement name", "Player"));

    public NameProtect() {
        super("name_protect", "NameProtect", "Replace your username with an alias in incoming chat lines.", Category.MISC);
    }

    @Override public Component onIncoming(Component message) {
        Minecraft client = Minecraft.getInstance();
        String own = client.player.getName().getString();
        String text = message.getString();
        if (!text.contains(own)) return message;
        return Component.literal(text.replace(own, alias.get()));
    }
}
