package dev.anomoly.setting;

import com.google.gson.JsonElement;

public abstract class Setting {
    private final String id;
    private final String name;

    protected Setting(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String id() { return id; }
    public String name() { return name; }
    public abstract JsonElement toJson();
    public abstract void fromJson(JsonElement value);
}
