package dev.anomoly.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class BooleanSetting extends Setting {
    private boolean value;

    public BooleanSetting(String id, String name, boolean initial) {
        super(id, name);
        value = initial;
    }

    public boolean get() { return value; }
    public void set(boolean value) { this.value = value; }
    @Override public JsonElement toJson() { return new JsonPrimitive(value); }
    @Override public void fromJson(JsonElement value) { this.value = value.getAsBoolean(); }
}
