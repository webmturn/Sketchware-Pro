package pro.sketchware.activities.editor.block;

import static pro.sketchware.util.GsonUtils.getGson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.NoSuchFileException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

/** Replays one editor change against the latest library, preserving unrelated edits. */
final class BlockEditorMutation {
    static final class Conflict extends IOException { }
    record Loaded(ArrayList<HashMap<String, Object>> blocks, ArrayList<String> names, byte[] digest) { }
    final String ownerToken;
    private final String mode;
    private final int position;
    private final HashMap<String, Object> original;
    private final HashMap<String, Object> changes;
    private byte[] expectedDigest;
    private ArrayList<HashMap<String, Object>> baseSnapshot;
    private byte[] baseDigest;
    private boolean expectedMissing;
    BlockEditorSaveState receipt;
    private Path palettePath;
    private byte[] paletteDigest;

    BlockEditorMutation(String ownerToken, String mode, int position, HashMap<String, Object> original,
                        HashMap<String, Object> changes) {
        this.ownerToken = ownerToken; this.mode = mode; this.position = position;
        this.original = original == null ? null : new HashMap<>(original);
        this.changes = new HashMap<>(changes);
    }

    BlockEditorMutation(String ownerToken, String mode, int position, HashMap<String, Object> original,
                        HashMap<String, Object> changes, ArrayList<HashMap<String, Object>> base, byte[] digest) {
        this(ownerToken, mode, position, original, changes);
        // The editor never mutates loaded records; its form changes live in separate view values.
        baseSnapshot = new ArrayList<>(base);
        baseDigest = digest.clone();
    }

    static Loaded read(Path path) throws IOException { return parse(Files.readAllBytes(path)); }

    static Loaded read(Path path, boolean allowMissing) throws IOException {
        try { return read(path); }
        catch (NoSuchFileException missing) {
            if (!allowMissing) throw missing;
            return parse("[]".getBytes(StandardCharsets.UTF_8));
        }
    }

    static byte[] recordDigest(HashMap<String, Object> block) {
        return digest(getGson().toJson(block).getBytes(StandardCharsets.UTF_8));
    }

    static int findAnchor(Loaded loaded, byte[] expected, String name, int preferred) {
        var blocks = loaded.blocks();
        if (preferred >= 0 && preferred < blocks.size() && Arrays.equals(expected, recordDigest(blocks.get(preferred)))) return preferred;
        int target = -1;
        for (int i = 0; i < blocks.size(); i++) {
            var block = blocks.get(i);
            if (block != null && (name == null || name.equals(block.get("name"))) && Arrays.equals(expected, recordDigest(block))) {
                if (target != -1) return -1;
                target = i;
            }
        }
        return target;
    }

    private static Loaded parse(byte[] source) {
        ArrayList<HashMap<String, Object>> blocks = getGson().fromJson(new String(source, StandardCharsets.UTF_8),
                new TypeToken<ArrayList<HashMap<String, Object>>>() { }.getType());
        if (blocks == null) throw new JsonParseException("The block library is empty");
        ArrayList<String> names = new ArrayList<>();
        for (var block : blocks) if (block != null && block.get("name") instanceof String name) names.add(name);
        return new Loaded(blocks, names, digest(source));
    }

    ArrayList<HashMap<String, Object>> prepare(Path path) throws IOException {
        verifyPalette();
        byte[] source;
        expectedMissing = false;
        try { source = Files.readAllBytes(path); }
        catch (NoSuchFileException missing) {
            if (!"add".equals(mode) || baseSnapshot == null || !baseSnapshot.isEmpty()) throw missing;
            source = "[]".getBytes(StandardCharsets.UTF_8); expectedMissing = true;
        }
        byte[] currentDigest = digest(source);
        ArrayList<HashMap<String, Object>> blocks = baseSnapshot != null && Arrays.equals(baseDigest, currentDigest)
                ? new ArrayList<>(baseSnapshot) : parse(source).blocks();
        int target = position;
        if (!"add".equals(mode)) {
            if (target < 0 || target >= blocks.size() || !Objects.equals(blocks.get(target), original)) {
                target = -1;
                for (int i = 0; i < blocks.size(); i++) if (Objects.equals(blocks.get(i), original)) {
                    if (target != -1) throw new Conflict();
                    target = i;
                }
                if (target == -1) throw new Conflict();
            }
        }
        boolean sameName = "edit".equals(mode) && Objects.equals(changes.get("name"), original.get("name"));
        if (!sameName) for (int i = 0; i < blocks.size(); i++) {
            var block = blocks.get(i);
            if (block != null && !("edit".equals(mode) && i == target)
                    && Objects.equals(block.get("name"), changes.get("name"))) throw new Conflict();
        }
        if ("edit".equals(mode)) {
            HashMap<String, Object> updated = new HashMap<>(blocks.get(target));
            updated.putAll(changes); blocks.set(target, updated);
        } else if ("insert".equals(mode)) blocks.add(target, new HashMap<>(changes));
        else blocks.add(new HashMap<>(changes));
        expectedDigest = currentDigest;
        return blocks;
    }

    void verifyUnchanged(Path path) throws IOException {
        verifyPalette();
        if (expectedMissing) {
            if (Files.exists(path)) throw new Conflict();
            return;
        }
        if (!Arrays.equals(expectedDigest, digest(Files.readAllBytes(path)))) throw new Conflict();
    }

    void guardPalette(Path path, byte[] digest) {
        palettePath = path; paletteDigest = digest.clone();
    }

    private void verifyPalette() throws IOException {
        if (palettePath != null && !Arrays.equals(paletteDigest, digest(Files.readAllBytes(palettePath)))) throw new Conflict();
    }

    private static byte[] digest(byte[] bytes) {
        try { return MessageDigest.getInstance("SHA-256").digest(bytes); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
}
