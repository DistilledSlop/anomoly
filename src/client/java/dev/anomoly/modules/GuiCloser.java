package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.IntSetting;
import dev.anomoly.setting.StringSetting;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

public final class GuiCloser extends Module {
    private final IntSetting delay = add(new IntSetting("delay_ticks", "Close after ticks", 20, 1, 200));
    private final StringSetting title = add(new StringSetting("title", "Only when title contains (blank for any)", ""));
    private int open;

    public GuiCloser() {
        super("gui_closer", "GUICloser", "Automatically close container screens such as chests and shop menus after a short delay.", Category.MISC);
    }

    @Override public void tick(Minecraft client) {
        if (!(client.gui.screen() instanceof AbstractContainerScreen<?> screen) || screen instanceof InventoryScreen) {
            open = 0;
            return;
        }
        boolean matches = title.get().isBlank() || screen.getTitle().getString().toLowerCase(Locale.ROOT).contains(title.get().toLowerCase(Locale.ROOT));
        if (matches && ++open >= delay.get()) {
            client.player.closeContainer();
            open = 0;
        }
    }
}
