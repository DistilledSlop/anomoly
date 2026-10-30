package dev.anomoly.modules;

import dev.anomoly.Module;

public final class AntiReducedDebugInfo extends Module {
    public AntiReducedDebugInfo() {
        super("anti_reduced_debug_info", "Anti Reduced Debug Info", "Show full local F3 details even when a server requests reduced debug info.", Category.EXPLOIT);
    }
}
