package pro.sketchware.activities.editor.block;

import static org.junit.Assert.*;
import static pro.sketchware.util.GsonUtils.getGson;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockEditorMutationTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private static HashMap<String, Object> block(String name, String code) {
        return new HashMap<>(Map.of("name", name, "code", code, "palette", "9", "unknown", "keep"));
    }
    private Path source(List<HashMap<String, Object>> blocks) throws Exception {
        Path path = temporary.newFile().toPath(); write(path, blocks); return path;
    }
    private static void write(Path path, List<HashMap<String, Object>> blocks) throws Exception {
        Files.write(path, getGson().toJson(blocks).getBytes(StandardCharsets.UTF_8));
    }
    private static BlockEditorMutation edit(HashMap<String, Object> original) {
        return new BlockEditorMutation("owner", "edit", 0, original, new HashMap<>(Map.of("code", "USER_EDIT", "name", original.get("name"))));
    }
    @Test public void savingOneBlockPreservesOtherChangesAndUnknownFields() throws Exception {
        var original = block("A", "OLD"); var other = block("B", "EXTERNAL"); Path path = source(List.of(original, other));
        var result = edit(original).prepare(path);
        assertEquals("USER_EDIT", result.get(0).get("code")); assertEquals("keep", result.get(0).get("unknown"));
        assertEquals(other, result.get(1)); assertEquals("OLD", original.get("code"));
    }
    @Test public void unchangedSourceReusesReadOnlyBaseAndChangedSourceRebases() throws Exception {
        var original = block("A", "OLD"); Path path = source(List.of(original, block("B", "FIRST")));
        var loaded = BlockEditorMutation.read(path);
        var mutation = new BlockEditorMutation("owner", "edit", 0, loaded.blocks().get(0),
                new HashMap<>(Map.of("code", "EDIT", "name", "A")), loaded.blocks(), loaded.digest());
        var first = mutation.prepare(path); assertSame(loaded.blocks().get(1), first.get(1));
        assertNotSame(loaded.blocks().get(0), first.get(0)); assertEquals("OLD", loaded.blocks().get(0).get("code"));
        write(path, List.of(original, block("B", "LATER")));
        var retry = mutation.prepare(path); assertEquals("LATER", retry.get(1).get("code"));
        assertNotSame(loaded.blocks().get(1), retry.get(1));
    }
    @Test public void movedUnchangedAnchorKeepsItsIdentity() throws Exception {
        var original = block("A", "OLD"); Path path = source(List.of(block("B", "OTHER"), original));
        var result = edit(original).prepare(path);
        assertEquals("OTHER", result.get(0).get("code")); assertEquals("USER_EDIT", result.get(1).get("code"));
    }
    @Test public void changedTargetAndAmbiguousMovedTargetsAreRejected() throws Exception {
        var original = block("A", "OLD"); Path path = source(List.of(block("A", "EXTERNAL")));
        assertThrows(BlockEditorMutation.Conflict.class, () -> edit(original).prepare(path));
        write(path, List.of(block("B", "OTHER"), original, original));
        assertThrows(BlockEditorMutation.Conflict.class, () -> edit(original).prepare(path));
    }
    @Test public void insertTracksAnchorAndNewNameConflictsAreRejected() throws Exception {
        var original = block("A", "OLD"); var added = block("NEW", "NEW"); Path path = source(List.of(block("B", "OTHER"), original));
        var insert = new BlockEditorMutation("owner", "insert", 0, original, added);
        var result = insert.prepare(path); assertEquals(List.of("B", "NEW", "A"), result.stream().map(x -> x.get("name")).toList());
        write(path, List.of(added, original)); assertThrows(BlockEditorMutation.Conflict.class, () -> insert.prepare(path));
    }
    @Test public void retryRebasesUnrelatedChangesAndLastCheckRejectsInterveningWrites() throws Exception {
        var original = block("A", "OLD"); Path path = source(List.of(original, block("B", "FIRST"))); var mutation = edit(original);
        mutation.prepare(path); write(path, List.of(original, block("B", "LATER")));
        assertThrows(BlockEditorMutation.Conflict.class, () -> mutation.verifyUnchanged(path));
        var retry = mutation.prepare(path); mutation.verifyUnchanged(path); assertEquals("LATER", retry.get(1).get("code"));
    }
    @Test public void nullRowsAndExistingDuplicateNamesArePreserved() throws Exception {
        var original = block("A", "OLD"); ArrayList<HashMap<String, Object>> input = new ArrayList<>();
        input.add(original); input.add(null); input.add(block("A", "DUPLICATE")); Path path = source(input);
        var result = edit(original).prepare(path); assertNull(result.get(1)); assertEquals("DUPLICATE", result.get(2).get("code"));
        assertEquals(List.of("A", "A"), BlockEditorMutation.read(path).names());
    }
    @Test public void malformedAndNullSourcesNeverBecomeAnEmptyWritableLibrary() throws Exception {
        Path path = source(List.of(block("A", "OLD")));
        for (String invalid : List.of("[broken", "null", "")) {
            Files.write(path, invalid.getBytes(StandardCharsets.UTF_8));
            var add = new BlockEditorMutation("owner", "add", 9, null, block("NEW", "NEW"));
            assertThrows(com.google.gson.JsonParseException.class, () -> add.prepare(path));
            assertEquals(invalid, new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
        }
    }

    @Test public void paletteChangesRejectBothPreparationAndFinalCommitCheck() throws Exception {
        var original = block("A", "OLD"); Path path = source(List.of(original));
        Path palette = source(List.of(new HashMap<>(Map.of("name", "Palette", "color", "#1976D2"))));
        var mutation = edit(original); mutation.guardPalette(palette, BlockEditorMutation.read(palette).digest());
        mutation.prepare(path);
        write(palette, List.of(new HashMap<>(Map.of("name", "Other", "color", "#1976D2"))));
        assertThrows(BlockEditorMutation.Conflict.class, () -> mutation.verifyUnchanged(path));
        assertThrows(BlockEditorMutation.Conflict.class, () -> mutation.prepare(path));
    }
}
