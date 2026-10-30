package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.StringSetting;

public final class ClientSpoof extends Module {
    private final StringSetting brand = add(new StringSetting("brand", "Reported client brand", "vanilla"));

    public ClientSpoof() {
        super("client_spoof", "ClientSpoof", "Report a plain client brand such as vanilla to servers instead of the modded one (also covers Server Spoof's brand).", Category.MISC);
    }

    public String brand() { return brand.get(); }
}
