package dev.anomoly.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class StringSetting extends Setting {
    private String value;

    public StringSetting(String id, String name, String initial) {
        super(id, name);
        value = initial;
    }

    public String get() { return value; }
    public void set(String value) { this.value = value == null ? "" : value; }
    @Override public JsonElement toJson() { return new JsonPrimitive(value); }
    @Override public void fromJson(JsonElement value) { set(value.getAsString()); }
}
