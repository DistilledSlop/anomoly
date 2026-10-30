package dev.anomoly.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class FloatSetting extends Setting {
    private final float min;
    private final float max;
    private float value;

    public FloatSetting(String id, String name, float initial, float min, float max) {
        super(id, name);
        if (min > max) throw new IllegalArgumentException("min > max");
        this.min = min;
        this.max = max;
        set(initial);
    }

    public float get() { return value; }
    public float min() { return min; }
    public float max() { return max; }
    public void set(float value) { this.value = Math.max(min, Math.min(max, value)); }
    @Override public JsonElement toJson() { return new JsonPrimitive(value); }
    @Override public void fromJson(JsonElement value) { set(value.getAsFloat()); }
}
