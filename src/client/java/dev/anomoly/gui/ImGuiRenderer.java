package dev.anomoly.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.anomoly.AnomolyClient;
import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.FloatSetting;
import dev.anomoly.setting.Setting;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImGuiStyle;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiCond;
import imgui.gl3.ImGuiImplGl3;
import net.minecraft.client.Minecraft;
import java.util.Comparator;
import java.util.Locale;
import org.lwjgl.opengl.GL;

public final class ImGuiRenderer {
    private static final ImGuiImplGl3 GL_RENDERER = new ImGuiImplGl3();
    private static boolean initialized;
    private static boolean failed;
    private static long lastFrame;

    private ImGuiRenderer() { }

    public static boolean initialized() { return initialized; }
    public static boolean failed() { return failed; }

    public static boolean supported() {
        return RenderSystem.getDevice().getDeviceInfo().backendName().toLowerCase(java.util.Locale.ROOT).contains("opengl");
    }

    public static void render() {
        Minecraft client = Minecraft.getInstance();
        if (!(client.gui.screen() instanceof ClickGuiScreen) || !supported() || failed) return;
        try {
            if (!initialized) initialize();
            ImGuiIO io = ImGui.getIO();
            int width = client.getWindow().getScreenWidth();
            int height = client.getWindow().getScreenHeight();
            io.setDisplaySize(width, height);
            io.setDisplayFramebufferScale(
                (float) client.getWindow().getWidth() / width,
                (float) client.getWindow().getHeight() / height
            );
            io.addMousePosEvent((float) client.mouseHandler.xpos(), (float) client.mouseHandler.ypos());
            long now = System.nanoTime();
            io.setDeltaTime(lastFrame == 0 ? 1F / 60F : Math.max(0.001F, Math.min(0.1F, (now - lastFrame) / 1_000_000_000F)));
            lastFrame = now;
            GL_RENDERER.newFrame();
            ImGui.newFrame();
            drawUi();
            ImGui.render();
            GL_RENDERER.renderDrawData(ImGui.getDrawData());
        } catch (Throwable t) {
            failed = true;
            AnomolyClient.LOGGER.error("ImGui render failed; disabling the ClickGUI renderer", t);
        }
    }

    private static void initialize() {
        GL.getCapabilities();
        ImGui.createContext();
        ImGuiIO io = ImGui.getIO();
        io.setIniFilename(null);
        ImGui.styleColorsDark();
        ImGuiStyle style = ImGui.getStyle();
        style.setWindowRounding(12);
        style.setFrameRounding(6);
        style.setGrabRounding(6);
        style.setTabRounding(6);
        style.setColor(ImGuiCol.WindowBg, 0.055F, 0.075F, 0.105F, 0.97F);
        style.setColor(ImGuiCol.FrameBg, 0.11F, 0.16F, 0.21F, 1F);
        style.setColor(ImGuiCol.Header, 0.12F, 0.29F, 0.34F, 1F);
        style.setColor(ImGuiCol.HeaderHovered, 0.14F, 0.48F, 0.49F, 1F);
        style.setColor(ImGuiCol.CheckMark, 0.36F, 0.93F, 0.78F, 1F);
        style.setColor(ImGuiCol.Button, 0.10F, 0.36F, 0.40F, 1F);
        style.setColor(ImGuiCol.ButtonHovered, 0.14F, 0.55F, 0.58F, 1F);
        style.setColor(ImGuiCol.TabSelected, 0.10F, 0.45F, 0.45F, 1F);
        if (!GL_RENDERER.init("#version 150")) throw new IllegalStateException("Could not initialize the ImGui OpenGL renderer");
        initialized = true;
    }

    private static void drawUi() {
        ImGui.setNextWindowSize(760, 515, ImGuiCond.Once);
        if (ImGui.begin("ANOMOLY  /  Minecraft 26.3")) {
            ImGui.textColored(0.38F, 0.94F, 0.79F, 1F, "ANOMOLY");
            ImGui.sameLine();
            ImGui.textDisabled("  Right Shift: close   |   End: disable all modules");
            ImGui.separator();
            if (ImGui.beginTabBar("anomoly-categories")) {
                for (Module.Category category : Module.Category.values()) {
                    if (ImGui.beginTabItem(category.name())) {
                        for (Module module : AnomolyClient.MODULES.all()) {
                            if (module.category() != category) continue;
                            if (ImGui.checkbox(module.name() + "##" + module.id(), module.enabled())) {
                                module.setEnabled(!module.enabled());
                            }
                            ImGui.sameLine();
                            ImGui.textDisabled(module.description());
                            for (Setting setting : module.settings()) {
                                if (setting instanceof BooleanSetting toggle) {
                                    if (ImGui.checkbox("    " + toggle.name() + "##" + module.id() + setting.id(), toggle.get())) {
                                        toggle.set(!toggle.get());
                                        AnomolyClient.saveConfig();
                                    }
                                } else if (setting instanceof FloatSetting slider) {
                                    float[] value = {slider.get()};
                                    if (ImGui.sliderFloat("    " + slider.name() + "##" + module.id() + setting.id(), value, slider.min(), slider.max())) {
                                        slider.set(value[0]);
                                        AnomolyClient.saveConfig();
                                    }
                                }
                            }
                            if (module.id().equals("player_radar") && module.enabled()) drawRadar();
                            ImGui.spacing();
                        }
                        ImGui.endTabItem();
                    }
                }
                if (ImGui.beginTabItem("PROFILE")) {
                    boolean recording = AnomolyClient.PROFILE.enabled();
                    if (ImGui.checkbox("Record passive server profile", recording)) {
                        AnomolyClient.PROFILE.setEnabled(!recording);
                        AnomolyClient.saveConfig();
                    }
                    ImGui.text("Server: " + AnomolyClient.PROFILE.server());
                    ImGui.text("Brand: " + AnomolyClient.PROFILE.brand());
                    ImGui.text("Position corrections: " + AnomolyClient.PROFILE.corrections());
                    ImGui.textWrapped("Log: " + AnomolyClient.PROFILE.path());
                    ImGui.spacing();
                    ImGui.textDisabled("Records normal movement, server brand, corrections and disconnects.");
                    ImGui.textDisabled("No active anticheat probes or plugin enumeration packets are sent.");
                    ImGui.endTabItem();
                }
                ImGui.endTabBar();
            }
            ImGui.separator();
            if (ImGui.button("PANIC  /  disable all modules")) AnomolyClient.MODULES.disableAll();
        }
        ImGui.end();
    }

    private static void drawRadar() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return;
        client.level.players().stream()
            .filter(player -> player != client.player)
            .sorted(Comparator.comparingDouble(client.player::distanceToSqr))
            .limit(16)
            .forEach(player -> ImGui.text(String.format(Locale.ROOT, "    %s  %.1fm  (%.0f, %.0f, %.0f)",
                player.getName().getString(), client.player.distanceTo(player), player.getX(), player.getY(), player.getZ())));
    }
}
