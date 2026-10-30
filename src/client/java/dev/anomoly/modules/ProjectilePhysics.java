package dev.anomoly.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.ExperienceBottleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.item.ThrowablePotionItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.WindChargeItem;
import net.minecraft.world.item.component.ChargedProjectiles;

/** Flight constants audited from Minecraft 26.3 item and projectile sources. */
final class ProjectilePhysics {
    record Profile(float speed, double gravity, float drag, boolean gravityBeforeMove, boolean dragBeforeMove,
                   boolean inheritsShooterMotion, float aimPitchOffset, double spawnBelowEye) { }

    private ProjectilePhysics() { }

    static Profile held(Minecraft client, boolean bowsOnly) {
        if (client.player == null) return null;
        if (client.player.isUsingItem()) return forStack(client, client.player.getUseItem(), bowsOnly);
        Profile main = forStack(client, client.player.getMainHandItem(), bowsOnly);
        return main != null ? main : forStack(client, client.player.getOffhandItem(), bowsOnly);
    }

    static Profile forStack(Minecraft client, ItemStack stack, boolean bowsOnly) {
        if (stack.isEmpty()) return null;
        Item item = stack.getItem();
        if (item instanceof BowItem) {
            float power = client.player.isUsingItem() && client.player.getUseItem() == stack
                ? BowItem.getPowerForTime(client.player.getTicksUsingItem()) : 1F;
            return power < 0.1F ? null : new Profile(power * 3F, 0.05, 0.99F, false, false, true, 0F, 0.1);
        }
        if (item instanceof CrossbowItem) {
            ChargedProjectiles charged = stack.get(DataComponents.CHARGED_PROJECTILES);
            if (charged != null && charged.contains(Items.FIREWORK_ROCKET))
                return new Profile(1.6F, 0.0, 1F, false, false, false, 0F, 0.15);
            return new Profile(3.15F, 0.05, 0.99F, false, false, false, 0F, 0.1);
        }
        if (bowsOnly) return null;
        if (item instanceof TridentItem) return new Profile(2.5F, 0.05, 0.99F, false, false, true, 0F, 0.1);
        if (item instanceof SnowballItem || item instanceof EggItem || item instanceof EnderpearlItem)
            return new Profile(1.5F, 0.03, 0.99F, true, true, true, 0F, 0.1);
        if (item instanceof ExperienceBottleItem) return new Profile(0.7F, 0.07, 0.99F, true, true, true, 20F, 0.1);
        if (item instanceof ThrowablePotionItem) return new Profile(0.5F, 0.05, 0.99F, true, true, true, 20F, 0.1);
        if (item instanceof WindChargeItem) return new Profile(1.5F, 0.0, 1F, true, true, true, 0F, 0.0);
        return null;
    }

    static Profile atFullCharge(Minecraft client, ItemStack stack) {
        if (stack.getItem() instanceof BowItem)
            return new Profile(3F, 0.05, 0.99F, false, false, true, 0F, 0.1);
        return forStack(client, stack, false);
    }
}
