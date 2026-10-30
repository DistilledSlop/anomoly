package dev.anomoly.mixin;

import dev.anomoly.clock.ClientClockIdentity;
import net.minecraft.client.ClientClockManager;
import net.minecraft.core.Holder;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientClockManager.class)
public abstract class ClientClockManagerMixin {
    @Inject(method = "getInstance", at = @At("RETURN"))
    private void anomoly$identifyClock(Holder<WorldClock> definition,
        CallbackInfoReturnable<ClientClockManager.ClientClockInstance> ci) {
        ((ClientClockIdentity)ci.getReturnValue()).anomoly$setOverworld(definition.is(WorldClocks.OVERWORLD));
    }
}
