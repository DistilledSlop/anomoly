package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.StringSetting;
import net.minecraft.resources.Identifier;

public final class SoundBlocker extends Module {
    private final StringSetting sounds = add(new StringSetting("sounds", "Sound IDs or fragments (comma separated)", "minecraft:entity.generic.explode"));

    public SoundBlocker() {
        super("sound_blocker", "Sound Blocker", "Silence sounds whose ID contains any listed text.", Category.MISC);
    }

    public boolean blocks(Identifier id) {
        if (!enabled()) return false;
        String name = id.toString();
        for (String fragment : sounds.get().split(",")) if (!fragment.isBlank() && name.contains(fragment.trim())) return true;
        return false;
    }
}
