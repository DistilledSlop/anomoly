package dev.anomoly.mixin;

import dev.anomoly.gui.ImGuiRenderer;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftRenderMixin {
    // Render after RenderPearl copies the game frame to the window, before swap/present.
    @Inject(method = "renderFrame(Z)V", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/device/GpuSurface;present()V", remap = false))
    private void anomoly$renderImGui(boolean advanceGameTime, CallbackInfo ci) {
        ImGuiRenderer.render();
    }
}
