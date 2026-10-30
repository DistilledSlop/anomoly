package dev.anomoly.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.anomoly.AnomolyClient;
import dev.anomoly.clock.ClientClockIdentity;
import dev.anomoly.modules.TimeChanger;
import dev.anomoly.modules.WeatherChanger;
import net.minecraft.client.ClientClockManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientClockManager.ClientClockInstance.class)
public abstract class ClientClockInstanceMixin implements ClientClockIdentity {
    @Unique private boolean anomoly$overworld;

    @Override public void anomoly$setOverworld(boolean overworld) {
        anomoly$overworld = overworld;
    }

    @ModifyReturnValue(method = "totalTicks", at = @At("RETURN"))
    private long anomoly$fixedTime(long original) {
        if (!anomoly$overworld) return original;
        long selected = anomoly$selectedTicks();
        return selected >= 0 ? selected : original;
    }

    @ModifyReturnValue(method = "partialTick", at = @At("RETURN"))
    private float anomoly$fixedPartialTick(float original) {
        return anomoly$overworld && anomoly$selectedTicks() >= 0 ? 0.0F : original;
    }

    @Unique private static long anomoly$selectedTicks() {
        WeatherChanger weather = AnomolyClient.MODULES.get(WeatherChanger.class);
        if (weather.overridesTime()) return weather.fixedTicks();
        TimeChanger time = AnomolyClient.MODULES.get(TimeChanger.class);
        return time.enabled() ? time.fixedTicks() : -1L;
    }
}
