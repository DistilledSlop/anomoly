package dev.anomoly.modules;

import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.phys.Vec3;

/** Builds an overhead block only when a falling anvil is headed for the player. */
public final class AntiAnvil extends SelfTrap {
    private final FloatSetting scanHeight = add(new FloatSetting("scan_height", "Anvil scan height", 16F, 5F, 32F));
    private final FloatSetting predictTicks = add(new FloatSetting("predict_ticks", "Prediction ticks", 30F, 8F, 60F));
    private final FloatSetting horizontalMargin = add(new FloatSetting("horizontal_margin", "Horizontal threat margin", 0.8F, 0.5F, 1.5F));

    public AntiAnvil() {
        super("anti_anvil", "Anti Anvil", "Build an overhead shield when a falling anvil is headed for you.");
    }

    @Override public void tick(Minecraft client) {
        if (threatened(client)) super.tick(client);
    }

    private boolean threatened(Minecraft client) {
        LocalPlayer player = client.player;
        double roofTop = player.blockPosition().getY() + 3.0;
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof FallingBlockEntity falling) || !falling.isAlive()
                || !falling.getBlockState().is(BlockTags.ANVIL)
                || falling.getY() <= roofTop || falling.getY() - player.getY() > scanHeight.get()) continue;
            Vec3 position = falling.position();
            Vec3 motion = falling.getDeltaMovement();
            for (int tick = 1; tick <= (int)predictTicks.get(); tick++) {
                motion = motion.add(0, falling.isNoGravity() ? 0 : -0.04, 0);
                position = position.add(motion);
                if (position.y <= roofTop) {
                    if (Math.abs(position.x - player.getX()) < horizontalMargin.get()
                        && Math.abs(position.z - player.getZ()) < horizontalMargin.get()) return true;
                    break;
                }
                motion = motion.scale(0.98);
            }
        }
        return false;
    }
}
