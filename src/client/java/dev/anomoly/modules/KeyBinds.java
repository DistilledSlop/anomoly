package dev.anomoly.modules;

import java.util.List;
import net.minecraft.client.Minecraft;

public final class KeyBinds extends TextHud {
    public KeyBinds() {
        super("keybinds", "KeyBinds", "Show which movement, jump, sneak and attack keys are held on the HUD.");
    }

    @Override protected List<String> lines(Minecraft client) {
        var o = client.options;
        return List.of((o.keyUp.isDown() ? "[W]" : " W ") + (o.keyLeft.isDown() ? "[A]" : " A ") + (o.keyDown.isDown() ? "[S]" : " S ")
            + (o.keyRight.isDown() ? "[D]" : " D "), (o.keyJump.isDown() ? "[JUMP]" : " JUMP ") + (o.keyShift.isDown() ? "[SNEAK]" : " SNEAK ")
            + (o.keyAttack.isDown() ? "[LMB]" : " LMB ") + (o.keyUse.isDown() ? "[RMB]" : " RMB "));
    }
}
