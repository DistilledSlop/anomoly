package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/** Conservative player-entity filters shared by automatic combat modules. */
public final class AntiBot extends Module {
    private final BooleanSetting requirePlayerInfo = add(new BooleanSetting("require_player_info", "Require player-list entry", true));
    private final FloatSetting minimumAge = add(new FloatSetting("minimum_age", "Minimum entity age (ticks)", 0F, 0F, 100F));
    private final BooleanSetting ignoreInvisible = add(new BooleanSetting("ignore_invisible", "Ignore invisible players", false));

    public AntiBot() {
        super("anti_bot", "Anti Bot", "Keep automatic combat modules from targeting suspicious player entities.", Category.COMBAT);
    }

    public boolean reject(Minecraft client, Player player) {
        if (!enabled()) return false;
        if (requirePlayerInfo.get() && client.getConnection() != null
            && client.getConnection().getPlayerInfo(player.getUUID()) == null) return true;
        if (player.tickCount < minimumAge.get()) return true;
        return ignoreInvisible.get() && player.isInvisible();
    }
}
