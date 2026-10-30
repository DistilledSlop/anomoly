package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.gui.ClickGuiScreen;
import dev.anomoly.gui.FallbackClickGuiScreen;
import dev.anomoly.gui.ImGuiRenderer;
import net.minecraft.client.Minecraft;

public final class ClickGui extends Module {
    public ClickGui() {
        super("click_gui", "ClickGUI", "One-shot: open the Anomoly module browser, then switch off (the same screen Right Shift opens).", Category.MISC);
    }

    @Override protected void onEnable(Minecraft client) {
        client.execute(() -> client.gui.setScreen(ImGuiRenderer.supported() && !ImGuiRenderer.failed()
            ? new ClickGuiScreen() : new FallbackClickGuiScreen()));
        setEnabled(false);
    }
}
