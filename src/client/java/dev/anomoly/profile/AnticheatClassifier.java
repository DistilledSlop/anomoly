package dev.anomoly.profile;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/** Interprets only command-tree and payload names already sent by the server. */
public final class AnticheatClassifier {
    private static final String RESOURCE = "/assets/anomoly/anticheat_signatures.json";
    private static final List<Signature> SIGNATURES = readSignatures();

    private AnticheatClassifier() { }

    public record Candidate(String name, String confidence, List<String> evidence, String source) { }
    private record Signature(String name, Set<String> roots, Set<String> namespaces, String source) { }

    public static List<Candidate> classify(Collection<String> commandNames, Collection<String> channelIds) {
        Set<String> commands = normalized(commandNames);
        Set<String> channels = normalized(channelIds);
        List<Candidate> result = new ArrayList<>();
        for (Signature signature : SIGNATURES) {
            List<String> evidence = new ArrayList<>();
            boolean namespaced = false;
            for (String command : commands) {
                if (signature.roots.contains(command)) evidence.add("visible command /" + command);
                int colon = command.indexOf(':');
                if (colon > 0 && signature.namespaces.contains(command.substring(0, colon))) {
                    evidence.add("visible command /" + command);
                    namespaced = true;
                }
            }
            for (String channel : channels) {
                int colon = channel.indexOf(':');
                if (colon > 0 && signature.namespaces.contains(channel.substring(0, colon))) {
                    evidence.add("observed payload " + channel);
                    namespaced = true;
                }
            }
            if (evidence.isEmpty()) continue;
            result.add(new Candidate(signature.name, namespaced ? "plausible" : "weak",
                List.copyOf(evidence), signature.source));
        }
        result.sort(Comparator.comparing((Candidate candidate) -> candidate.confidence.equals("plausible") ? 0 : 1)
            .thenComparing(Candidate::name));
        return List.copyOf(result);
    }

    private static Set<String> normalized(Collection<String> values) {
        Set<String> result = new TreeSet<>();
        for (String value : values) {
            if (value == null) continue;
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            if (normalized.startsWith("/")) normalized = normalized.substring(1);
            if (!normalized.isEmpty()) result.add(normalized);
        }
        return result;
    }

    private static List<Signature> readSignatures() {
        try (InputStream stream = AnticheatClassifier.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) throw new IOException("Missing " + RESOURCE);
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            if (root.get("schema").getAsInt() != 1) throw new IOException("Unsupported signature schema");
            List<Signature> result = new ArrayList<>();
            for (var element : root.getAsJsonArray("signatures")) {
                JsonObject object = element.getAsJsonObject();
                result.add(new Signature(object.get("name").getAsString(),
                    strings(object.getAsJsonArray("roots")), strings(object.getAsJsonArray("namespaces")),
                    object.get("source").getAsString()));
            }
            return List.copyOf(result);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static Set<String> strings(JsonArray array) {
        Set<String> result = new TreeSet<>();
        for (var element : array) result.add(element.getAsString().toLowerCase(Locale.ROOT));
        return Set.copyOf(result);
    }
}
