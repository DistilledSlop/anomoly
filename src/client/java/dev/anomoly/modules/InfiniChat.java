package dev.anomoly.modules;

import dev.anomoly.Module;

public final class InfiniChat extends Module {
    public InfiniChat() {
        super("infini_chat", "InfiniChat", "Lift the chat box's 256-character typing limit; servers still reject over-long chat messages, but long commands go through.", Category.MISC);
    }
}
