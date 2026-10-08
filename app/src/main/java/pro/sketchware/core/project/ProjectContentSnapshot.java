package pro.sketchware.core.project;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonWriter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

/** Fingerprints the content the project managers would save, without writing any files. */
public final class ProjectContentSnapshot {
    private static final int FILE_HASH_BUFFER_SIZE = 8 * 1024;
    private static final int MAX_CACHED_KEY_ORDERS = 256;
    private ProjectContentSnapshot() { }

    public static String capture(String projectId) throws IOException {
        if (!ProjectDataManager.canSaveProject(projectId)) {
            throw new IllegalStateException("Cannot snapshot unreadable project " + projectId);
        }
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
        ProjectDataStore data = ProjectDataManager.getProjectDataManager(projectId);
        append(digest, data::serializeViewData);
        append(digest, data::serializeLogicData);
        append(digest, ProjectDataManager.getFileManager(projectId)::serializeFiles);
        append(digest, ProjectDataManager.getResourceManager(projectId)::serializeResources);
        append(digest, ProjectDataManager.getLibraryManager(projectId)::serializeLibraries);
        ResourceManager resources = ProjectDataManager.getResourceManager(projectId);
        appendFiles(digest, new File(resources.imageDirPath).toPath());
        appendFiles(digest, new File(resources.soundDirPath).toPath());
        appendFiles(digest, new File(resources.fontDirPath).toPath());
        StringBuilder result = new StringBuilder();
        for (byte value : digest.digest()) {
            result.append(Character.forDigit((value & 0xff) >>> 4, 16));
            result.append(Character.forDigit(value & 0xf, 16));
        }
        return result.toString();
    }

    private static void appendFiles(MessageDigest digest, Path root) throws IOException {
        // A replaced image/font can retain exactly the same metadata and filename.
        if (Files.exists(root)) {
            try (Stream<Path> paths = Files.walk(root)) {
                for (Path path : (Iterable<Path>) paths.filter(Files::isRegularFile).sorted()::iterator) {
                    appendBytes(digest, root.relativize(path).toString().getBytes(StandardCharsets.UTF_8));
                    digest.update(ByteBuffer.allocate(Long.BYTES).putLong(Files.size(path)).array());
                    try (InputStream input = Files.newInputStream(path)) {
                        byte[] buffer = new byte[FILE_HASH_BUFFER_SIZE];
                        int count;
                        while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
                    }
                }
            }
        }
        appendBytes(digest, new byte[0]);
    }

    private static void append(MessageDigest digest, Consumer<StringBuilder> serializer) throws IOException {
        StringBuilder buffer = new StringBuilder();
        serializer.accept(buffer);
        // Section and JSON-object ordering is not an edit; list ordering is meaningful.
        Map<String, StringBuilder> sections = new TreeMap<>();
        Map<List<String>, List<String>> keyOrders = new HashMap<>();
        StringBuilder section = new StringBuilder();
        sections.put("", section);
        for (String line : buffer.toString().split("\n")) {
            if (line.startsWith("@")) {
                section = sections.computeIfAbsent(line, ignored -> new StringBuilder());
            } else if (!line.isEmpty()) {
                if (line.startsWith("{")) {
                    StringWriter output = new StringWriter();
                    try (JsonWriter writer = new JsonWriter(output)) {
                        writer.setStrictness(Strictness.LENIENT);
                        writer.setSerializeNulls(true);
                        writeSorted(JsonParser.parseString(line), writer, keyOrders);
                    }
                    section.append(output);
                } else {
                    section.append(line);
                }
                section.append('\n');
            }
        }
        StringBuilder normalized = new StringBuilder();
        sections.forEach((name, content) -> normalized.append(name).append('\n').append(content));
        byte[] bytes = normalized.toString().getBytes(StandardCharsets.UTF_8);
        appendBytes(digest, bytes);
    }

    private static void appendBytes(MessageDigest digest, byte[] bytes) {
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }

    private static void writeSorted(JsonElement element, JsonWriter output,
                                    Map<List<String>, List<String>> keyOrders) throws IOException {
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            List<String> names = new ArrayList<>(object.keySet());
            List<String> sorted = keyOrders.get(names);
            if (sorted == null) {
                sorted = new ArrayList<>(names);
                Collections.sort(sorted);
                // Reuse repeated bean shapes, with a per-serialization memory bound.
                if (keyOrders.size() < MAX_CACHED_KEY_ORDERS) keyOrders.put(names, sorted);
            }
            output.beginObject();
            for (String name : sorted) {
                output.name(name);
                writeSorted(object.get(name), output, keyOrders);
            }
            output.endObject();
        } else if (element.isJsonArray()) {
            output.beginArray();
            for (JsonElement value : element.getAsJsonArray()) writeSorted(value, output, keyOrders);
            output.endArray();
        } else if (element.isJsonNull()) {
            output.nullValue();
        } else {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isBoolean()) output.value(primitive.getAsBoolean());
            // Only numeric tokens already recognized by JsonParser reach this branch.
            // Preserve their spelling without value(Number)'s repeated regex validation.
            else if (primitive.isNumber()) output.jsonValue(primitive.getAsString());
            else output.value(primitive.getAsString());
        }
    }
}
