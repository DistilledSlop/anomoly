package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;

public final class ViewModel extends Module {
    private final FloatSetting main = add(new FloatSetting("main_height", "Main hand height", 0.7F, 0.0F, 1.0F));
    private final FloatSetting off = add(new FloatSetting("off_height", "Offhand height", 0.5F, 0.0F, 1.0F));

    public ViewModel() {
        super("view_model", "ViewModel", "Lower how high the held item and offhand sit on screen (also covers SmallShield and Hand View).", Category.RENDER);
    }

    public void apply(FirstPersonHandsAndItemsRenderState state) {
        if (!enabled()) return;
        state.mainHandHeight *= main.get();
        state.oldMainHandHeight *= main.get();
        state.offHandHeight *= off.get();
        state.oldOffHandHeight *= off.get();
    }
}
