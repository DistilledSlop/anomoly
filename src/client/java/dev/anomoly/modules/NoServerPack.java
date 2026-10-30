package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;

public final class NoServerPack extends Module {
    private final BooleanSetting pretend = add(new BooleanSetting("pretend_loaded", "Report the pack as loaded so required packs do not kick you", true));

    public NoServerPack() {
        super("no_server_pack", "No SRP", "Never download server resource packs; answer the server without loading them (also covers AntiServerRP).", Category.MISC);
    }

    /** Sends the replacement replies; the caller cancels the normal download. */
    public void answer(net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl listener, ClientboundResourcePackPushPacket packet) {
        if (pretend.get()) {
            listener.send(new ServerboundResourcePackPacket(packet.id(), ServerboundResourcePackPacket.Action.ACCEPTED));
            listener.send(new ServerboundResourcePackPacket(packet.id(), ServerboundResourcePackPacket.Action.DOWNLOADED));
            listener.send(new ServerboundResourcePackPacket(packet.id(), ServerboundResourcePackPacket.Action.SUCCESSFULLY_LOADED));
        } else {
            listener.send(new ServerboundResourcePackPacket(packet.id(), ServerboundResourcePackPacket.Action.DECLINED));
        }
    }
}
