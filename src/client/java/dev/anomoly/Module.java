package dev.anomoly;

import net.minecraft.client.Minecraft;
import java.util.ArrayList;
import java.util.List;
import dev.anomoly.setting.Setting;

public abstract class Module {
    public enum Category { COMBAT, MOVEMENT, PLAYER, WORLD, RENDER }

    private final String id;
    private final String name;
    private final String description;
    private final Category category;
    private boolean enabled;
    private final List<Setting> settings = new ArrayList<>();

    protected Module(String id, String name, String description, Category category) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
    }

    public final String id() { return id; }
    public final String name() { return name; }
    public final String description() { return description; }
    public final Category category() { return category; }
    public final boolean enabled() { return enabled; }
    public final List<Setting> settings() { return List.copyOf(settings); }
    protected final <T extends Setting> T add(T setting) { settings.add(setting); return setting; }

    public final void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        if (enabled) onEnable(Minecraft.getInstance());
        else onDisable(Minecraft.getInstance());
        AnomolyClient.saveConfig();
    }

    protected void onEnable(Minecraft client) { }
    protected void onDisable(Minecraft client) { }
    public void tick(Minecraft client) { }
}
