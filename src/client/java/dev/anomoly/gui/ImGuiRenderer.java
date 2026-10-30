package dev.anomoly.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.anomoly.AnomolyClient;
import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.ChoiceSetting;
import dev.anomoly.setting.FloatSetting;
import dev.anomoly.setting.Setting;
import dev.anomoly.setting.StringSetting;
import dev.anomoly.modules.PluginInspector;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImGuiStyle;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiCond;
import imgui.gl3.ImGuiImplGl3;
import imgui.type.ImString;
import net.minecraft.client.Minecraft;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.lwjgl.opengl.GL;

public final class ImGuiRenderer {
    private static final ImGuiImplGl3 GL_RENDERER = new ImGuiImplGl3();
    private static boolean initialized;
    private static boolean failed;
    private static long lastFrame;
    private static final ImString SEARCH = new ImString(96);
    private static final Map<Setting, ImString> TEXT_BUFFERS = new HashMap<>();
    private static Module.Category selectedCategory = Module.Category.COMBAT;
    private static boolean profilePage;

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
        Minecraft client = Minecraft.getInstance();
        int screenWidth = client.getWindow().getScreenWidth();
        int screenHeight = client.getWindow().getScreenHeight();
        int windowWidth = Math.max(1, Math.min(900, screenWidth - 24));
        int windowHeight = Math.max(1, Math.min(600, screenHeight - 24));
        ImGui.setNextWindowSize(windowWidth, windowHeight, ImGuiCond.Always);
        ImGui.setNextWindowPos((screenWidth - windowWidth) / 2F, (screenHeight - windowHeight) / 2F, ImGuiCond.Always);
        if (ImGui.begin("ANOMOLY  /  Minecraft 26.3")) {
            ImGui.textColored(0.38F, 0.94F, 0.79F, 1F, "ANOMOLY");
            ImGui.sameLine();
            ImGui.textDisabled("  Right Shift: close   |   End: disable all modules");
            ImGui.separator();
            ImGui.setNextItemWidth(300);
            ImGui.inputTextWithHint("##module-search", "Search modules or descriptions", SEARCH);
            ImGui.sameLine();
            long active = AnomolyClient.MODULES.all().stream().filter(Module::enabled).count();
            ImGui.textDisabled(active + " active / " + AnomolyClient.MODULES.all().size() + " modules");
            ImGui.separator();

            ImGui.beginChild("navigation", 170, -48, true);
            ImGui.textDisabled("CATEGORIES");
            for (Module.Category category : Module.Category.values()) {
                long count = AnomolyClient.MODULES.all().stream().filter(module -> module.category() == category).count();
                if (ImGui.selectable(category.name() + "  " + count, !profilePage && selectedCategory == category)) {
                    selectedCategory = category;
                    profilePage = false;
                }
            }
            ImGui.separator();
            if (ImGui.selectable("SERVER PROFILE", profilePage)) profilePage = true;
            ImGui.endChild();
            ImGui.sameLine();
            ImGui.beginChild("module-content", 0, -48, true);
            if (profilePage) drawProfile();
            else {
                String query = SEARCH.get().trim().toLowerCase(Locale.ROOT);
                ImGui.textColored(0.38F, 0.94F, 0.79F, 1F, query.isEmpty() ? selectedCategory.name() : "SEARCH RESULTS");
                ImGui.separator();
                int matches = 0;
                for (Module module : AnomolyClient.MODULES.all()) {
                    if (query.isEmpty() && module.category() != selectedCategory) continue;
                    if (!query.isEmpty() && !module.name().toLowerCase(Locale.ROOT).contains(query)
                        && !module.description().toLowerCase(Locale.ROOT).contains(query)) continue;
                    matches++;
                    drawModule(module);
                }
                if (matches == 0) ImGui.textDisabled("No modules found.");
            }
            ImGui.endChild();
            ImGui.separator();
            if (ImGui.button("PANIC  /  disable all modules")) AnomolyClient.MODULES.disableAll();
        }
        ImGui.end();
    }

    private static void drawModule(Module module) {
        if (ImGui.checkbox("##enabled-" + module.id(), module.enabled())) module.setEnabled(!module.enabled());
        ImGui.sameLine();
        if (ImGui.collapsingHeader(module.name() + "##details-" + module.id())) {
            ImGui.textWrapped(module.description());
            var key = AnomolyClient.moduleKey(module);
            if (key != null) ImGui.textDisabled("Toggle key: " + key.getTranslatedKeyMessage().getString() + "  (Minecraft Controls)");
            for (Setting setting : module.settings()) {
                if (setting instanceof BooleanSetting toggle) {
                    if (ImGui.checkbox(toggle.name() + "##" + module.id() + setting.id(), toggle.get())) {
                        toggle.set(!toggle.get());
                        AnomolyClient.saveConfig();
                    }
                } else if (setting instanceof FloatSetting slider) {
                    float[] value = {slider.get()};
                    ImGui.setNextItemWidth(240);
                    if (ImGui.sliderFloat(slider.name() + "##" + module.id() + setting.id(), value, slider.min(), slider.max())) {
                        slider.set(value[0]);
                        AnomolyClient.saveConfig();
                    }
                } else if (setting instanceof ChoiceSetting choice) {
                    ImGui.setNextItemWidth(240);
                    if (ImGui.beginCombo(choice.name() + "##" + module.id() + setting.id(), choice.get())) {
                        for (String option : choice.choices())
                            if (ImGui.selectable(option, option.equals(choice.get()))) {
                                choice.set(option);
                                AnomolyClient.saveConfig();
                            }
                        ImGui.endCombo();
                    }
                } else if (setting instanceof StringSetting string) {
                    ImString buffer = TEXT_BUFFERS.computeIfAbsent(setting, unused -> new ImString(string.get(), 256));
                    ImGui.setNextItemWidth(360);
                    if (ImGui.inputTextWithHint(string.name() + "##" + module.id() + setting.id(),
                        "minecraft:item, minecraft:other_item", buffer)) {
                        string.set(buffer.get());
                        AnomolyClient.saveConfig();
                    }
                }
            }
            if (module.id().equals("player_radar") && module.enabled()) drawRadar();
            ImGui.spacing();
        }
    }

    private static void drawProfile() {
        ImGui.textColored(0.38F, 0.94F, 0.79F, 1F, "SERVER PROFILE");
        ImGui.separator();
        boolean recording = AnomolyClient.PROFILE.enabled();
        if (ImGui.checkbox("Record passive server profile", recording)) {
            AnomolyClient.PROFILE.setEnabled(!recording);
            AnomolyClient.saveConfig();
        }
        ImGui.text("Server: " + AnomolyClient.PROFILE.server());
        ImGui.text("Brand: " + AnomolyClient.PROFILE.brand());
        ImGui.text("Position corrections: " + AnomolyClient.PROFILE.corrections());
        ImGui.text("Corrections after 5 seconds: " + AnomolyClient.PROFILE.postJoinCorrections());
        ImGui.textWrapped("Log: " + AnomolyClient.PROFILE.path());
        ImGui.spacing();
        ImGui.textColored(0.38F, 0.94F, 0.79F, 1F, "POSSIBLE ANTICHEATS");
        var candidates = AnomolyClient.PROFILE.candidates(Minecraft.getInstance());
        if (candidates.isEmpty()) ImGui.textDisabled("No recognizable command or payload signature observed.");
        else for (var candidate : candidates) {
            ImGui.text(candidate.name() + "  /  " + candidate.confidence());
            for (String evidence : candidate.evidence()) ImGui.bulletText(evidence);
        }
        ImGui.textDisabled("Hints only: commands can be hidden or spoofed. Corrections do not identify a plugin.");
        ImGui.textDisabled("Enable AntiCheat Detect under Misc for one-time chat notices.");
        ImGui.spacing();
        ImGui.textDisabled("Records normal movement, server brand, corrections and disconnects.");
        ImGui.textDisabled("No active anticheat probes or plugin enumeration packets are sent.");
        PluginInspector plugins = AnomolyClient.MODULES.get(PluginInspector.class);
        ImGui.spacing();
        ImGui.separator();
        ImGui.textColored(0.38F, 0.94F, 0.79F, 1F, "VISIBLE COMMAND NAMESPACES");
        if (!plugins.enabled()) ImGui.textDisabled("Enable Plugins under Exploit to inspect the server command tree.");
        else {
            ImGui.textWrapped(plugins.namespaces().isEmpty() ? "No namespaced commands exposed by this server."
                : String.join(", ", plugins.namespaces()));
            if (ImGui.collapsingHeader("Visible root commands (" + plugins.commands().size() + ")")) {
                for (String command : plugins.commands()) ImGui.text(command);
            }
            ImGui.textDisabled("Only commands sent to this client are visible; namespace is not proof a plugin is installed.");
        }
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
