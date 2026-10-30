package dev.anomoly;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import dev.anomoly.gui.ClickGuiScreen;
import dev.anomoly.profile.ProfileCollector;
import dev.anomoly.setting.Setting;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AnomolyClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("anomoly");
    public static final ModuleManager MODULES = new ModuleManager();
    public static final ProfileCollector PROFILE = new ProfileCollector();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG = FabricLoader.getInstance().getConfigDir().resolve("anomoly.json");
    private static boolean loading;
    private static KeyMapping guiKey;
    private static KeyMapping panicKey;

    @Override public void onInitializeClient() {
        loadConfig();
        // SDL scancodes, not legacy GLFW key codes: right shift and End.
        guiKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.anomoly.gui", 229, KeyMapping.Category.MISC));
        panicKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.anomoly.panic", 77, KeyMapping.Category.MISC));
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            MODULES.tick(client);
            PROFILE.tick(client);
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (panicKey.consumeClick()) MODULES.disableAll();
            while (guiKey.consumeClick()) {
                if (client.gui.screen() instanceof ClickGuiScreen) client.gui.setScreen(null);
                else client.gui.setScreen(new ClickGuiScreen());
            }
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> PROFILE.join(client));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> PROFILE.leave("connection closed"));
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
        try {
            Files.createDirectories(CONFIG.getParent());
            Files.writeString(CONFIG, GSON.toJson(root));
        } catch (IOException e) {
            LOGGER.warn("Could not save Anomoly config", e);
        }
    }
}
