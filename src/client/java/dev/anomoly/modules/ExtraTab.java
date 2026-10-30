package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public final class ExtraTab extends Module {
    private final BooleanSetting health = add(new BooleanSetting("health", "Show health of loaded players", true));
    private final BooleanSetting gamemode = add(new BooleanSetting("gamemode", "Show game mode", false));

    public ExtraTab() {
        super("extra_tab", "ExtraTab", "Append health and game mode to names on the player list (also covers BetterTab).", Category.RENDER);
    }

    public Component decorate(Component name, PlayerInfo info) {
        if (!enabled()) return name;
        Component result = name;
        Minecraft client = Minecraft.getInstance();
        if (health.get() && client.level != null) {
            Player player = client.level.getPlayerByUUID(info.getProfile().id());
            if (player != null) result = Component.empty().append(result).append(Component.literal(" " + Math.round(player.getHealth()) + " HP"));
        }
        if (gamemode.get()) result = Component.empty().append(result).append(Component.literal(" [" + info.getGameMode().getShortDisplayName().getString() + "]"));
        return result;
    }
}
