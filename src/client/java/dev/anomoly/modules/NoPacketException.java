package dev.anomoly.modules;

import dev.anomoly.Module;

/** Mirrors the empty upstream registration: the source snapshot implements no behavior. */
public final class NoPacketException extends Module {
    public NoPacketException() {
        super("no_packet_exception", "NoPacketException", "Placeholder matching the empty upstream module; it does nothing.", Category.MISC);
    }
}
