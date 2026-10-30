package dev.anomoly;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import dev.anomoly.gui.ClickGuiScreen;
import dev.anomoly.gui.FallbackClickGuiScreen;
import dev.anomoly.gui.ImGuiRenderer;
import dev.anomoly.profile.ProfileCollector;
import dev.anomoly.modules.AutoReconnect;
import dev.anomoly.modules.FreeLook;
import dev.anomoly.modules.Offhand;
import dev.anomoly.modules.ElytraSwap;
import dev.anomoly.modules.AutoBuff;
import dev.anomoly.setting.Setting;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AnomolyClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("anomoly");
    public static final ModuleManager MODULES = new ModuleManager();
    public static final ProfileCollector PROFILE = new ProfileCollector();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG = FabricLoader.getInstance().getConfigDir().resolve("anomoly.json");
    private static boolean loading;
    private static int configBatchDepth;
    private static boolean configDirty;
    private static KeyMapping guiKey;
    private static KeyMapping panicKey;
    private static KeyMapping zoomKey;
    private static KeyMapping freeLookKey;
    private static final Map<Module, KeyMapping> MODULE_KEYS = new LinkedHashMap<>();
    private static final Map<String, KeyMapping> OFFHAND_KEYS = new LinkedHashMap<>();

    public static boolean zoomHeld() { return zoomKey != null && zoomKey.isDown(); }
    public static boolean freeLookHeld() { return freeLookKey != null && freeLookKey.isDown(); }
    public static KeyMapping moduleKey(Module module) { return MODULE_KEYS.get(module); }

    @Override public void onInitializeClient() {
        loadConfig();
        // SDL scancodes, not legacy GLFW key codes: right shift and End.
        guiKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.anomoly.gui", 229, KeyMapping.Category.MISC));
        panicKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.anomoly.panic", 77, KeyMapping.Category.MISC));
        zoomKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.anomoly.zoom", 6, KeyMapping.Category.MISC));
        freeLookKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.anomoly.free_look", 11, KeyMapping.Category.MISC));
        KeyMapping.Category moduleCategory = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("anomoly", "modules"));
        for (Module module : MODULES.all()) {
            KeyMapping key = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.anomoly.toggle." + module.id(), 0, moduleCategory));
            MODULE_KEYS.put(module, key);
        }
        for (String mode : new String[] {"cycle", "auto", "totem", "crystal", "gapple", "strength", "block"})
            OFFHAND_KEYS.put(mode, KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.anomoly.offhand." + mode, 0, moduleCategory)));
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            MODULES.tick(client);
            PROFILE.tick(client);
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            MODULES.get(dev.anomoly.modules.AutoBow.class).finishTick(client);
            MODULES.get(dev.anomoly.modules.AutoPearl.class).finishTick(client);
            MODULES.get(dev.anomoly.modules.AutoSoup.class).finishTick(client);
            MODULES.get(dev.anomoly.modules.Soup.class).finishTick(client);
            MODULES.get(dev.anomoly.modules.WTap.class).finishTick(client);
            MODULES.get(AutoBuff.class).finishTick(client);
            MODULES.get(AutoReconnect.class).tickDisconnected(client);
            if (client.gui.screen() instanceof ClickGuiScreen
                && (ImGuiRenderer.failed() || !ImGuiRenderer.supported())) {
                client.gui.setScreen(new FallbackClickGuiScreen());
            }
            boolean panicPressed = false;
            while (panicKey.consumeClick()) { MODULES.disableAll(); panicPressed = true; }
            boolean guiPressed = false;
            while (guiKey.consumeClick()) {
                guiPressed = true;
                if (client.gui.screen() instanceof ClickGuiScreen || client.gui.screen() instanceof FallbackClickGuiScreen) {
                    client.gui.setScreen(null);
                } else client.gui.setScreen(ImGuiRenderer.supported() && !ImGuiRenderer.failed()
                    ? new ClickGuiScreen() : new FallbackClickGuiScreen());
            }
            for (var entry : MODULE_KEYS.entrySet()) {
                while (entry.getValue().consumeClick()) {
                    if (!panicPressed && !guiPressed && client.player != null && client.gui.screen() == null)
                        entry.getKey().setEnabled(!entry.getKey().enabled());
                }
            }
            Offhand offhand = MODULES.get(Offhand.class);
            for (var entry : OFFHAND_KEYS.entrySet()) while (entry.getValue().consumeClick()) {
                if (!panicPressed && !guiPressed && client.player != null && client.gui.screen() == null) {
                    if (entry.getKey().equals("cycle")) offhand.cycleMode();
                    else offhand.selectMode(Character.toUpperCase(entry.getKey().charAt(0))
                        + entry.getKey().substring(1));
                }
            }
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> PROFILE.join(client));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            MODULES.get(FreeLook.class).reset(client);
            MODULES.get(ElytraSwap.class).disconnect();
            MODULES.get(AutoBuff.class).disconnect();
            PROFILE.leave("connection closed");
        });
        LOGGER.info("Anomoly initialized");
    }

    private static void loadConfig() {
        if (!Files.exists(CONFIG)) return;
        loading = true;
        try {
            JsonObject root = GSON.fromJson(Files.readString(CONFIG), JsonObject.class);
            if (root == null) return;
            JsonObject states = root.getAsJsonObject("modules");
            if (states != null) for (Module module : MODULES.all()) {
                if (states.has(module.id())) module.setEnabled(states.get(module.id()).getAsBoolean());
            }
            JsonObject settings = root.getAsJsonObject("settings");
            if (settings != null) for (Module module : MODULES.all()) {
                JsonObject moduleSettings = settings.getAsJsonObject(module.id());
                if (moduleSettings == null) continue;
                for (Setting setting : module.settings()) {
                    if (moduleSettings.has(setting.id())) setting.fromJson(moduleSettings.get(setting.id()));
                }
            }
            if (root.has("profileRecording")) PROFILE.setEnabled(root.get("profileRecording").getAsBoolean());
        } catch (Exception e) {
            LOGGER.warn("Could not load Anomoly config", e);
        } finally {
            loading = false;
        }
    }

    public static void saveConfig() {
        if (loading) return;
        if (configBatchDepth > 0) { configDirty = true; return; }
        JsonObject root = new JsonObject();
        JsonObject states = new JsonObject();
        for (Module module : MODULES.all()) states.addProperty(module.id(), module.enabled());
        root.add("modules", states);
        JsonObject settings = new JsonObject();
        for (Module module : MODULES.all()) {
            JsonObject moduleSettings = new JsonObject();
            for (Setting setting : module.settings()) moduleSettings.add(setting.id(), setting.toJson());
            settings.add(module.id(), moduleSettings);
        }
        root.add("settings", settings);
        root.addProperty("profileRecording", PROFILE.enabled());
        Path temporary = CONFIG.resolveSibling(CONFIG.getFileName() + ".tmp");
        try {
            Files.createDirectories(CONFIG.getParent());
            Files.writeString(temporary, GSON.toJson(root));
            try {
                Files.move(temporary, CONFIG, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, CONFIG, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            LOGGER.warn("Could not save Anomoly config", e);
        }
    }

    public static void batchConfigChanges(Runnable changes) {
        configBatchDepth++;
        try { changes.run(); }
        finally {
            configBatchDepth--;
            if (configBatchDepth == 0 && configDirty) {
                configDirty = false;
                saveConfig();
            }
        }
    }
}
