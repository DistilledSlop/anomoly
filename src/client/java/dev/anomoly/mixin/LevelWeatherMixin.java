package dev.anomoly.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.WeatherChanger;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Level.class)
public abstract class LevelWeatherMixin {
    @ModifyReturnValue(method = "getRainLevel", at = @At("RETURN"))
    private float anomoly$rain(float original) {
        WeatherChanger weather = AnomolyClient.MODULES.get(WeatherChanger.class);
        return (Object)this instanceof ClientLevel && weather.enabled() ? weather.rain() : original;
    }

    @ModifyReturnValue(method = "getThunderLevel", at = @At("RETURN"))
    private float anomoly$thunder(float original) {
        WeatherChanger weather = AnomolyClient.MODULES.get(WeatherChanger.class);
        return (Object)this instanceof ClientLevel && weather.enabled() ? weather.thunder() : original;
    }
}
