package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;

public final class NoRotate extends Module {
    private final BooleanSetting teleport = add(new BooleanSetting("teleport", "Ignore teleport rotation", true));
    private final BooleanSetting rotationPacket = add(new BooleanSetting("rotation_packet", "Ignore rotation packets", true));

    public NoRotate() {
        super("no_rotate", "No Rotate", "Preserve your camera direction when the server sends position or rotation updates.", Category.PLAYER);
    }

    public boolean ignoreTeleport() { return enabled() && teleport.get(); }
    public boolean ignoreRotationPacket() { return enabled() && rotationPacket.get(); }
}
