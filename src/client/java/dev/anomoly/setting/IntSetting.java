package dev.anomoly.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** A persisted whole-number slider. */
public final class IntSetting extends Setting {
    private final int min;
    private final int max;
    private int value;

    public IntSetting(String id, String name, int initial, int min, int max) {
        super(id, name);
        if (min > max) throw new IllegalArgumentException("min > max");
        this.min = min;
        this.max = max;
        set(initial);
    }

    public int get() { return value; }
    public int min() { return min; }
    public int max() { return max; }
    public void set(int value) { this.value = Math.max(min, Math.min(max, value)); }
    @Override public JsonElement toJson() { return new JsonPrimitive(value); }
    @Override public void fromJson(JsonElement value) { set(value.getAsInt()); }
}
