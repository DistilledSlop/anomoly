package dev.anomoly.modules;

import dev.anomoly.AnomolyClient;
import dev.anomoly.Module;
import dev.anomoly.mixin.MinecraftAccessor;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.entity.player.Player;

public final class AutoClicker extends Module {
    private final BooleanSetting attack = add(new BooleanSetting("attack", "Repeat attack", true));
    private final FloatSetting attackCps = add(new FloatSetting("attack_cps", "Attack clicks/sec", 8, 1, 20));
    private final BooleanSetting use = add(new BooleanSetting("use", "Repeat use", true));
    private final FloatSetting useCps = add(new FloatSetting("use_cps", "Use clicks/sec", 8, 1, 20));
    private boolean attackHeld;
    private boolean useHeld;
    private boolean usePulse;
    private int attackTicks;
    private int useTicks;

    public AutoClicker() {
        super("auto_clicker", "Auto Clicker", "Repeat attack and use actions while their keys are held.", Category.COMBAT);
    }

    @Override public void tick(Minecraft client) {
        if (client.gui.screen() != null || client.gameMode == null || SoupUse.busy(client)
            || AnomolyClient.MODULES.get(AutoBuff.class).combatPaused(client)
            || AnomolyClient.MODULES.get(SmartEat.class).combatPaused(client)) { reset(); return; }
        if (attack.get() && client.options.keyAttack.isDown() && !(client.hitResult instanceof BlockHitResult)
            && !(client.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof Player player
                && AnomolyClient.MODULES.get(AntiBot.class).reject(client, player))) {
            if (!attackHeld) { attackHeld = true; attackTicks = 0; }
            else if (++attackTicks >= interval(attackCps.get())) {
                ((MinecraftAccessor)client).anomoly$startAttack();
                attackTicks = 0;
            }
        } else { attackHeld = false; attackTicks = 0; }
        if (use.get() && client.options.keyUse.isDown()) {
            if (!useHeld) { useHeld = true; useTicks = 0; }
            else if (++useTicks >= interval(useCps.get())) {
                usePulse = true;
                useTicks = 0;
            }
        } else { useHeld = false; usePulse = false; useTicks = 0; }
    }

    public boolean allowHeldUse() {
        if (SoupUse.busy(Minecraft.getInstance())) return true;
        if (!enabled() || !use.get()) return true;
        boolean allowed = usePulse;
        usePulse = false;
        return allowed;
    }

    private static int interval(float cps) { return Math.max(1, Math.round(20.0F / cps)); }

    private void reset() {
        attackHeld = false; useHeld = false; usePulse = false;
        attackTicks = 0; useTicks = 0;
    }

    @Override protected void onDisable(Minecraft client) { reset(); }
}
