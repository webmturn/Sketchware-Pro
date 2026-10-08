package pro.sketchware.core.project;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

/** Fingerprints the content the project managers would save, without writing any files. */
public final class ProjectContentSnapshot {
    private static final int FILE_HASH_BUFFER_SIZE = 8 * 1024;
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

    private static void append(MessageDigest digest, Consumer<StringBuilder> serializer) {
        StringBuilder buffer = new StringBuilder();
        serializer.accept(buffer);
        // Section and JSON-object ordering is not an edit; list ordering is meaningful.
        Map<String, StringBuilder> sections = new TreeMap<>();
        StringBuilder section = new StringBuilder();
        sections.put("", section);
        for (String line : buffer.toString().split("\n")) {
            if (line.startsWith("@")) {
                section = sections.computeIfAbsent(line, ignored -> new StringBuilder());
            } else if (!line.isEmpty()) {
                section.append(line.startsWith("{")
                        ? sortObjectKeys(JsonParser.parseString(line)).toString() : line).append('\n');
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

    private static JsonElement sortObjectKeys(JsonElement element) {
        if (element.isJsonObject()) {
            JsonObject result = new JsonObject();
            Map<String, JsonElement> entries = new TreeMap<>();
            element.getAsJsonObject().entrySet().forEach(entry -> entries.put(entry.getKey(), entry.getValue()));
            entries.forEach((key, value) -> result.add(key, sortObjectKeys(value)));
            return result;
        }
        if (element.isJsonArray()) {
            JsonArray result = new JsonArray();
            element.getAsJsonArray().forEach(value -> result.add(sortObjectKeys(value)));
            return result;
        }
        return element;
    }
}
