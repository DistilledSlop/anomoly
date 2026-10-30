package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;

public final class AutoLeave extends Module {
    private final FloatSetting health = add(new FloatSetting("health", "Leave at health", 6, 1, 20));
    private final BooleanSetting absorption = add(new BooleanSetting("absorption", "Count absorption hearts", true));
    private boolean triggered;
    private ClientPacketListener connection;

    public AutoLeave() {
        super("auto_leave", "Auto Leave", "Disconnect from a multiplayer server when effective health reaches the threshold.", Category.COMBAT);
    }

    @Override public void tick(Minecraft client) {
        if (client.getConnection() != connection) {
            connection = client.getConnection();
            triggered = false;
        }
        if (triggered || client.getCurrentServer() == null || client.isLocalServer()) return;
        float effectiveHealth = client.player.getHealth()
            + (absorption.get() ? client.player.getAbsorptionAmount() : 0);
        if (effectiveHealth <= 0 || effectiveHealth > health.get()) return;
        triggered = true;
        client.disconnectFromWorld(Component.literal("Auto Leave: health reached " + effectiveHealth));
    }

    @Override protected void onDisable(Minecraft client) {
        triggered = false;
        connection = null;
    }
}
