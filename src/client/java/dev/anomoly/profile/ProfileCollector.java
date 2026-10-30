package dev.anomoly.profile;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.anomoly.AnomolyClient;
import dev.anomoly.Module;
import dev.anomoly.modules.PluginInspector;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/** Passive observations only. This class never sends packets to the server. */
public final class ProfileCollector {
    private static final Gson GSON = new Gson();
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC);
    private static final int MOVEMENT_SAMPLE_TICKS = Math.max(1, Integer.getInteger("anomoly.profile.sampleIntervalTicks", 20));
    private boolean enabled = true;
    private BufferedWriter writer;
    private Path path;
    private String server = "not connected";
    private String brand = "unknown";
    private int corrections;
    private int postJoinCorrections;
    private long ticks;
    private Vec3 lastPosition;
    private final Set<String> observedChannels = new HashSet<>();
    private final Set<String> observedCommands = new HashSet<>();

    public boolean enabled() { return enabled; }
    public String server() { return server; }
    public String brand() { return brand; }
    public int corrections() { return corrections; }
    public int postJoinCorrections() { return postJoinCorrections; }
    public Set<String> observedChannels() { return Set.copyOf(observedChannels); }
    public Set<String> observedCommands() { return Set.copyOf(observedCommands); }
    public String path() { return path == null ? "No active session" : path.toAbsolutePath().toString(); }

    public List<AnticheatClassifier.Candidate> candidates(Minecraft client) {
        if (client.getConnection() == null) return List.of();
        Set<String> commands = new HashSet<>(observedCommands);
        commands.addAll(PluginInspector.visibleCommands(client));
        return AnticheatClassifier.classify(commands, observedChannels);
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        if (!enabled) leave("recording disabled");
        else if (Minecraft.getInstance().getConnection() != null) join(Minecraft.getInstance());
    }

    public void join(Minecraft client) {
        if (!enabled) return;
        leave("new session");
        server = client.getCurrentServer() == null ? "singleplayer" : client.getCurrentServer().ip;
        brand = "unknown";
        corrections = 0;
        postJoinCorrections = 0;
        ticks = 0;
        lastPosition = null;
        observedChannels.clear();
        observedCommands.clear();
        String fileName = FILE_TIME.format(Instant.now()) + "-" + server.replaceAll("[^a-zA-Z0-9._-]", "_") + ".jsonl";
        path = FabricLoader.getInstance().getConfigDir().resolve("anomoly").resolve("profiles").resolve(fileName);
        try {
            Files.createDirectories(path.getParent());
            writer = Files.newBufferedWriter(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            JsonObject event = event("join");
            event.addProperty("server", server);
            event.addProperty("minecraft", "26.3");
            record(event);
        } catch (IOException e) {
            writer = null;
            AnomolyClient.LOGGER.warn("Could not create profile log {}", path, e);
        }
    }

    public void tick(Minecraft client) {
        if (!enabled || writer == null || client.player == null) return;
        ticks++;
        LocalPlayer player = client.player;
        lastPosition = player.position();
        String currentBrand = client.getConnection() == null ? null : client.getConnection().serverBrand();
        if (currentBrand != null && !currentBrand.equals(brand)) {
            brand = currentBrand;
            JsonObject event = event("brand");
            event.addProperty("value", brand);
            record(event);
        }
        if (ticks % MOVEMENT_SAMPLE_TICKS == 0) {
            JsonObject event = event("movement_sample");
            addPlayer(event, player);
            record(event);
        }
        if (ticks % 20 == 0) {
            for (String command : PluginInspector.visibleCommands(client)) {
                if (!observedCommands.add(command)) continue;
                JsonObject discovered = event("command_literal");
                discovered.addProperty("name", command);
                if (command.contains(":")) discovered.addProperty("namespace", command.substring(0, command.indexOf(':')));
                record(discovered);
            }
        }
    }

    public void correction(int teleportId, LocalPlayer player) {
        if (!enabled || writer == null || player == null) return;
        corrections++;
        if (ticks >= 100) postJoinCorrections++;
        JsonObject event = event("position_correction");
        event.addProperty("teleportId", teleportId);
        if (lastPosition != null) event.addProperty("distanceFromLastTick", Math.sqrt(lastPosition.distanceToSqr(player.position())));
        addPlayer(event, player);
        record(event);
    }

    public void channel(String channel) {
        if (!enabled || writer == null || !observedChannels.add(channel)) return;
        JsonObject event = event("custom_payload_channel");
        event.addProperty("id", channel);
        record(event);
    }

    public void leave(String reason) {
        if (writer != null) {
            JsonObject event = event("leave");
            event.addProperty("reason", reason);
            record(event);
            try { writer.close(); } catch (IOException e) { AnomolyClient.LOGGER.warn("Could not close profile log", e); }
            writer = null;
        }
        server = "not connected";
        brand = "unknown";
        lastPosition = null;
        observedChannels.clear();
        observedCommands.clear();
    }

    private JsonObject event(String type) {
        JsonObject event = new JsonObject();
        event.addProperty("time", Instant.now().toString());
        event.addProperty("tick", ticks);
        event.addProperty("type", type);
        return event;
    }

    private void addPlayer(JsonObject event, LocalPlayer player) {
        event.addProperty("x", player.getX());
        event.addProperty("y", player.getY());
        event.addProperty("z", player.getZ());
        event.addProperty("yaw", player.getYRot());
        event.addProperty("pitch", player.getXRot());
        event.addProperty("velocityX", player.getDeltaMovement().x);
        event.addProperty("velocityY", player.getDeltaMovement().y);
        event.addProperty("velocityZ", player.getDeltaMovement().z);
        event.addProperty("onGround", player.onGround());
        event.addProperty("sprinting", player.isSprinting());
        Minecraft client = Minecraft.getInstance();
        if (client.getConnection() != null && client.getConnection().getPlayerInfo(player.getUUID()) != null) {
            event.addProperty("latencyMs", client.getConnection().getPlayerInfo(player.getUUID()).getLatency());
        }
        StringBuilder active = new StringBuilder();
        for (Module module : AnomolyClient.MODULES.all()) if (module.enabled()) {
            if (!active.isEmpty()) active.append(',');
            active.append(module.id());
        }
        event.addProperty("modules", active.toString());
    }

    private void record(JsonObject event) {
        if (writer == null) return;
        try {
            writer.write(GSON.toJson(event));
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            AnomolyClient.LOGGER.warn("Could not write profile log {}", path, e);
            try { writer.close(); } catch (IOException ignored) { }
            writer = null;
        }
    }
}
