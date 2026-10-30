package dev.anomoly.modules;

import dev.anomoly.Module;

public final class NoServerSlot extends Module {
    public NoServerSlot() {
        super("no_server_slot", "NoServerSlot", "Ignore server requests to change your selected hotbar slot (also covers NoSlotSet).", Category.PLAYER);
    }
}
