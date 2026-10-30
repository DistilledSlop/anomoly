package dev.anomoly.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.List;

/** A persisted setting whose value must be one of a small set of choices. */
public final class ChoiceSetting extends Setting {
    private final List<String> choices;
    private String value;

    public ChoiceSetting(String id, String name, String initial, String... choices) {
        super(id, name);
        this.choices = List.of(choices);
        if (this.choices.isEmpty() || !this.choices.contains(initial))
            throw new IllegalArgumentException("Initial value must be a choice");
        value = initial;
    }

    public String get() { return value; }
    public List<String> choices() { return choices; }
    public void set(String candidate) {
        for (String choice : choices) if (choice.equalsIgnoreCase(candidate)) {
            value = choice;
            return;
        }
    }
    public void next() { value = choices.get((choices.indexOf(value) + 1) % choices.size()); }
    @Override public JsonElement toJson() { return new JsonPrimitive(value); }
    @Override public void fromJson(JsonElement value) { set(value.getAsString()); }
}
