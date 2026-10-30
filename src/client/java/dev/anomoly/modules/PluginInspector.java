package dev.anomoly.modules;

import dev.anomoly.Module;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

/** Inventories namespaced commands already supplied by the server. */
public final class PluginInspector extends Module {
    private ClientPacketListener connection;
    private final Set<String> commands = new TreeSet<>();
    private final Set<String> namespaces = new TreeSet<>();

    public PluginInspector() {
        super("plugins", "Plugins", "Show plugin namespaces exposed by the server command tree without sending probes.", Category.EXPLOIT);
    }

    @Override public void tick(Minecraft client) {
        if (client.getConnection() != connection) {
            connection = client.getConnection();
            commands.clear();
            namespaces.clear();
        }
        if (connection == null) return;
        for (String literal : visibleCommands(client)) {
            if (commands.add(literal) && literal.contains(":")) namespaces.add(literal.substring(0, literal.indexOf(':')));
        }
    }

    public List<String> namespaces() { return List.copyOf(namespaces); }
    public List<String> commands() { return List.copyOf(commands); }

    public static Set<String> visibleCommands(Minecraft client) {
        Set<String> visible = new TreeSet<>();
        if (client.getConnection() == null || client.getConnection().getCommands() == null) return visible;
        for (var node : client.getConnection().getCommands().getRoot().getChildren()) {
            String name = node.getName();
            if (!name.isBlank()) visible.add(name);
        }
        return visible;
    }
}
