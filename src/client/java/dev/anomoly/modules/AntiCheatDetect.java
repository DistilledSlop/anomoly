package dev.anomoly.modules;

import dev.anomoly.AnomolyClient;
import dev.anomoly.Module;
import dev.anomoly.profile.AnticheatClassifier;
import dev.anomoly.setting.BooleanSetting;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;

/** Announces passive, source-backed anticheat candidates once per session. */
public final class AntiCheatDetect extends Module {
    private final BooleanSetting notify = add(new BooleanSetting("notify_in_chat", "Notify in chat", true));
    private final Set<String> announced = new HashSet<>();
    private ClientPacketListener connection;
    private int ticks;

    public AntiCheatDetect() {
        super("anticheat_detect", "AntiCheat Detect",
            "Show possible anticheats from commands and payloads the server already sends.", Category.MISC);
    }

    @Override public void tick(Minecraft client) {
        if (client.getConnection() != connection) {
            connection = client.getConnection();
            announced.clear();
            ticks = 0;
        }
        if (connection == null || ++ticks % 20 != 0 || !notify.get()) return;
        for (AnticheatClassifier.Candidate candidate : AnomolyClient.PROFILE.candidates(client)) {
            if (!announced.add(candidate.name())) continue;
            String evidence = candidate.evidence().getFirst();
            client.gui.hud.getChat().addClientSystemMessage(Component.literal(
                "[Anomoly] Possible anticheat: " + candidate.name() + " (" + candidate.confidence()
                    + "; " + evidence + ").").withStyle(ChatFormatting.YELLOW));
        }
    }

    @Override protected void onDisable(Minecraft client) {
        announced.clear();
        ticks = 0;
    }
}
