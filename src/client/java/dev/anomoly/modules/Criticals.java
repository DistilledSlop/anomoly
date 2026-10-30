package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.FloatSetting;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPunchPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.AABB;

/** Delays a melee attack until an ordinary jump is descending on the server. */
public final class Criticals extends Module {
    private final FloatSetting minimumStrength = add(new FloatSetting("minimum_strength", "Minimum attack strength", 0.95F, 0.5F, 1F));
    private final FloatSetting fallingTicks = add(new FloatSetting("falling_ticks", "Falling ticks before attack", 2F, 1F, 4F));
    private final FloatSetting maxWait = add(new FloatSetting("max_wait_ticks", "Maximum wait ticks", 18F, 8F, 30F));
    private final BooleanSetting playersOnly = add(new BooleanSetting("players_only", "Only players", false));
    private final BooleanSetting lineOfSight = add(new BooleanSetting("line_of_sight", "Require clear line of sight", true));
    private ClientLevel observedLevel;
    private LocalPlayer observedPlayer;
    private int targetId = -1;
    private UUID targetUuid;
    private int selectedSlot;
    private Item heldItem;
    private int age;
    private int descendingTicks;
    private boolean performingAttack;

    public Criticals() {
        super("criticals", "Criticals", "Jump and time melee attacks for vanilla critical hits.", Category.COMBAT);
    }

    public boolean pending(Minecraft client) {
        return enabled() && targetId >= 0 && observedPlayer == client.player && observedLevel == client.level;
    }

    public boolean suppressesSprintPrime(Minecraft client) {
        return enabled() && (performingAttack || client.player != null && client.player.fallDistance > 0F
            && !client.player.onGround() && !client.player.isInWater() && !client.player.onClimbable());
    }

    /** Returns true when this module took ownership of the attempted attack. */
    public boolean intercept(Player attacker, Entity entity) {
        if (!enabled() || performingAttack) return false;
        Minecraft client = Minecraft.getInstance();
        if (attacker != client.player || client.level == null || client.gameMode == null
            || client.gui.screen() != null || !client.player.isAlive() || client.player.isSpectator()
            || !(entity instanceof LivingEntity target) || !target.isAlive()) return false;
        if (pending(client)) return true;
        if (playersOnly.get() && !(target instanceof Player)
            || target instanceof Player other && !attacker.canHarmPlayer(other)
            || attacker.getAttackStrengthScale(0F) < minimumStrength.get()
            || attacker.isInWater() || attacker.onClimbable() || attacker.isMobilityRestricted()
            || attacker.isPassenger() || client.player.isHandsBusy()
            || lineOfSight.get() && !attacker.hasLineOfSight(target)
            || !inRange(attacker, target)) return false;
        if (attacker.onGround()) {
            AABB headroom = attacker.getBoundingBox().move(0, 1.1, 0);
            if (!client.level.noCollision(attacker, headroom)) return false;
            if (attacker.isSprinting()) attacker.setSprinting(false);
            client.player.connection.send(new ServerboundPlayerCommandPacket(attacker,
                ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
            attacker.jumpFromGround();
        } else if (attacker.fallDistance > 0F && !attacker.isSprinting()) {
            // Vanilla can already make this hit critical; preserve its timing.
            return false;
        }
        observedLevel = client.level;
        observedPlayer = client.player;
        targetId = target.getId();
        targetUuid = target.getUUID();
        selectedSlot = client.player.getInventory().getSelectedSlot();
        heldItem = client.player.getMainHandItem().getItem();
        age = 0;
        descendingTicks = 0;
        return true;
    }

    @Override public void tick(Minecraft client) {
        if (targetId < 0) return;
        if (!pending(client) || client.gameMode == null || client.gui.screen() != null
            || !client.player.isAlive() || client.player.isSpectator()
            || client.player.getInventory().getSelectedSlot() != selectedSlot
            || client.player.getMainHandItem().getItem() != heldItem || ++age > (int)maxWait.get()) {
            clear();
            return;
        }
        Entity entity = client.level.getEntity(targetId);
        if (!(entity instanceof LivingEntity target) || !target.isAlive()
            || !target.getUUID().equals(targetUuid) || !inRange(client.player, target)
            || lineOfSight.get() && !client.player.hasLineOfSight(target)) {
            clear();
            return;
        }
        if (client.player.onGround() && age > 2) { clear(); return; }
        if (client.player.fallDistance > 0F && !client.player.onGround()
            && client.player.getDeltaMovement().y < -0.02) descendingTicks++;
        else descendingTicks = 0;
        if (descendingTicks < (int)fallingTicks.get()) return;
        client.player.setSprinting(false);
        client.player.connection.send(new ServerboundPlayerCommandPacket(client.player,
            ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
        performingAttack = true;
        try {
            client.gameMode.attack(client.player, target);
            client.player.swing(InteractionHand.MAIN_HAND, client.player.getMainHandItem().getAttackAnimation(), false);
            client.player.connection.send(ServerboundPunchPacket.INSTANCE);
        }
        finally {
            performingAttack = false;
            clear();
        }
    }

    private static boolean inRange(Player attacker, Entity target) {
        return attacker.getAttackRangeWith(attacker.getMainHandItem())
            .isInRange(attacker, target.getBoundingBox(), 3.0);
    }

    private void clear() {
        observedLevel = null;
        observedPlayer = null;
        targetId = -1;
        targetUuid = null;
        heldItem = null;
        age = 0;
        descendingTicks = 0;
    }

    @Override protected void onDisable(Minecraft client) { clear(); }
}
