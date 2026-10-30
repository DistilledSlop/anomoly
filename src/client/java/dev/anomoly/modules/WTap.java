package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** Releases forward movement briefly after a hit, then restores sprint. */
public final class WTap extends Module {
    private final FloatSetting tapTicks = add(new FloatSetting("tap_ticks", "Forward release ticks", 2F, 1F, 10F));
    private final FloatSetting interval = add(new FloatSetting("interval_ticks", "Minimum ticks between taps", 6F, 3F, 30F));
    private final FloatSetting minimumStrength = add(new FloatSetting("minimum_strength", "Minimum attack strength", 0.9F, 0.1F, 1F));
    private final BooleanSetting targetMobs = add(new BooleanSetting("target_mobs", "Also tap after mob hits", false));
    private LocalPlayer observedPlayer;
    private int remaining;
    private int cooldown;

    public WTap() {
        super("w_tap", "W Tap", "Release forward movement briefly after a hit, then resume sprint.", Category.COMBAT);
    }

    public boolean tapping(Minecraft client) {
        return enabled() && observedPlayer == client.player && remaining > 0;
    }

    boolean afterAttack(Minecraft client, Entity target, AttackHooks.Context context) {
        if (!enabled() || client.player == null || client.player.isSpectator()
            || client.gui.screen() != null || remaining > 0 || cooldown > 0
            || !(target instanceof Player) && !targetMobs.get()
            || context.attackStrength() < minimumStrength.get()
            || !client.options.keyUp.isDown()) return false;
        observedPlayer = client.player;
        remaining = (int)tapTicks.get();
        cooldown = (int)interval.get();
        client.options.keyUp.setDown(false);
        client.player.setSprinting(false);
        client.player.connection.send(new ServerboundPlayerCommandPacket(client.player,
            ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
        return true;
    }

    @Override public void tick(Minecraft client) {
        if (remaining > 0 && observedPlayer != client.player) { release(client, false); return; }
        if (cooldown > 0) cooldown--;
        if (tapping(client)) client.options.keyUp.setDown(false);
    }

    public void finishTick(Minecraft client) {
        if (remaining > 0 && --remaining == 0) release(client, true);
    }

    private void release(Minecraft client, boolean restartSprint) {
        remaining = 0;
        boolean samePlayer = observedPlayer != null && observedPlayer == client.player;
        observedPlayer = null;
        if (client.gui.screen() != null) return;
        KeyMapping.setAll();
        if (restartSprint && samePlayer && client.options.keyUp.isDown() && client.player.isAlive()
            && !client.player.isSpectator() && !client.player.isShiftKeyDown()
            && !client.player.isUsingItem()) {
            client.player.setSprinting(true);
            client.player.connection.send(new ServerboundPlayerCommandPacket(client.player,
                ServerboundPlayerCommandPacket.Action.START_SPRINTING));
        }
    }

    @Override protected void onDisable(Minecraft client) {
        release(client, false);
        cooldown = 0;
    }
}
