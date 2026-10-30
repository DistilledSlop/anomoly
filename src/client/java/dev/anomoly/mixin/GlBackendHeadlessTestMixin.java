package dev.anomoly.mixin;

import com.mojang.renderpearl.backend.opengl.GlBackend;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Allows software GLX test displays without an sRGB visual when explicitly requested. */
@Mixin(GlBackend.class)
public abstract class GlBackendHeadlessTestMixin {
    @ModifyArg(method = "createWindow", at = @At(value = "INVOKE",
        target = "Lorg/lwjgl/sdl/SDLVideo;SDL_GL_SetAttribute(II)Z", ordinal = 4), index = 1)
    private int anomoly$headlessSrgbOverride(int requested) {
        return "1".equals(System.getenv("ANOMOLY_TEST_NO_SRGB")) ? 0 : requested;
    }
}
