package dev.anomoly.mixin;

import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.SafeWalk;
import dev.anomoly.modules.Scaffold;
import dev.anomoly.modules.AutoTunnel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerSafeWalkMixin {
    @Inject(method = "isStayingOnGroundSurface", at = @At("RETURN"), cancellable = true)
    private void anomoly$safeWalk(CallbackInfoReturnable<Boolean> result) {
        if ((Object)this instanceof LocalPlayer && (AnomolyClient.MODULES.get(SafeWalk.class).enabled()
            || AnomolyClient.MODULES.get(Scaffold.class).protectEdges()
            || AnomolyClient.MODULES.get(AutoTunnel.class).protectEdges())) {
            result.setReturnValue(true);
        }
    }
}
