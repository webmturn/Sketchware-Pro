package pro.sketchware.activities.resourceseditor;

import static org.junit.Assert.*;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.gson.*;
import org.junit.*;
import org.junit.runner.RunWith;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import pro.sketchware.activities.resourceseditor.components.fragments.ArraysEditor;
import pro.sketchware.activities.resourceseditor.components.models.*;
import pro.sketchware.core.resources.ColorModel;

@RunWith(AndroidJUnit4.class)
public class ResourceEditorModelSegmentsTest {
    private File root;
    private String session;
    private ResourceEditorDraftController.Gate gate;
    private ResourceEditorDraftStore store;
    private String failure;
    private long[] revisions;
    private boolean[] dirty;
    private final ResourceEditorSnapshotCache cache = new ResourceEditorSnapshotCache();

    @Before public void setup() throws Exception {
        root = new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getNoBackupFilesDir(),
                "model-segments-test-" + UUID.randomUUID());
        session = UUID.randomUUID().toString();
        gate = new ResourceEditorDraftController.Gate(); gate.owner = 1;
        store = create(null); store.open(false);
        revisions = new long[6]; dirty = new boolean[6];
    }
    @After public void cleanup() throws Exception {
        ResourceXmlWriter.testTimings = null;
        assertTrue(root.getCanonicalPath().startsWith(InstrumentationRegistry.getInstrumentation()
                .getTargetContext().getNoBackupFilesDir().getCanonicalPath() + File.separator));
        remove(root);
    }
    private static void remove(File file) throws IOException {
        File[] files = file.listFiles(); if (files != null) for (File child : files) remove(child);
        if (file.exists() && !file.delete()) throw new IOException("Unable to delete test material");
    }
    private ResourceEditorDraftStore create(String[] paths) throws IOException {
        return new ResourceEditorDraftStore(root, session, "segment-project", "-fr", gate, gate.owner,
                point -> { if (point.equals(failure)) throw new IOException("Injected " + point); }, paths);
    }
    private ArrayList<StyleModel> styles(int rows) {
        var result = new ArrayList<StyleModel>();
        for (int i = 0; i < rows; i++) result.add(new StyleModel("S" + String.format(Locale.ROOT, "%05d", i),
                "Parent", new LinkedHashMap<>(Map.of("attr", "A".repeat(80)))));
        return result;
    }
    private void change(List<StyleModel> live, int row, String value) {
        live.get(row).getAttributes().put("attr", value); cache.invalidate(live.get(row));
    }
    private void commit(long revision, ResourceEditorDraftState.Snapshot snapshot) throws IOException {
        revisions[2] = revision; dirty[2] = true;
        store.commit(revisions, dirty, Map.of(2, snapshot));
    }
    private ResourceEditorDraftState.Snapshot restore() throws IOException { return create(null).open(true)[2]; }
    private static String value(ResourceEditorDraftState.Snapshot snapshot, int row) {
        return ((StyleModel) snapshot.entries.get(row)).getAttribute("attr");
    }
    private JsonObject manifest() throws IOException {
        return JsonParser.parseString(read(new File(store.directory, "manifest.json"))).getAsJsonObject();
    }
    private JsonArray blocks() throws IOException { return manifest().getAsJsonArray("pages").get(2).getAsJsonObject().getAsJsonArray("blocks"); }
    private File segment(JsonObject block) { return new File(new File(store.directory, "pages"), "2-" + block.get("object").getAsString() + ".seg"); }
    private static String read(File file) throws IOException { return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8); }
    private static String sha(byte[] bytes) throws Exception {
        var out = new StringBuilder(); for (byte b : MessageDigest.getInstance("SHA-256").digest(bytes)) out.append(String.format(Locale.ROOT, "%02x", b & 255));
        return out.toString();
    }
    private void writeManifest(JsonObject data) throws IOException {
        Files.write(new File(store.directory, "manifest.json").toPath(), data.toString().getBytes(StandardCharsets.UTF_8));
    }
    private void rejectedRestore() throws Exception {
        var before = new TreeSet<>(List.of(Objects.requireNonNull(new File(store.directory, "pages").list())));
        byte[] manifest = Files.readAllBytes(new File(store.directory, "manifest.json").toPath());
        try { restore(); fail("Damaged reference must fail restore"); } catch (IOException expected) { }
        assertArrayEquals(manifest, Files.readAllBytes(new File(store.directory, "manifest.json").toPath()));
        assertEquals(before, new TreeSet<>(List.of(Objects.requireNonNull(new File(store.directory, "pages").list()))));
    }

    @Test public void firstCapturePacksAllBlocksWithOnlyOnePayloadSync() throws Exception {
        var live = styles(1024); var syncs = new AtomicInteger();
        ResourceXmlWriter.testTimings = (phase, nanos) -> { if (phase.equals("payload.syncNs")) syncs.incrementAndGet(); };
        commit(1, cache.capture(live, Map.of(700, "row note", 1024, "tail")));
        assertEquals(1, syncs.get()); assertEquals(4, blocks().size());
        assertEquals(1, Objects.requireNonNull(new File(store.directory, "pages").list()).length);
        var restored = restore(); assertEquals(1024, restored.entries.size());
        assertEquals("row note", restored.notes.get(700)); assertEquals("tail", restored.notes.get(1024));
    }

    @Test public void pointChangeReusesThreeBlocksAndWritesOnlyOne() throws Exception {
        var live = styles(1024); commit(1, cache.capture(live, Map.of())); var before = blocks().deepCopy();
        store = new ResourceEditorDraftStore(root, session, "segment-project", "-fr", gate, gate.owner, p -> {});
        // A new reader deliberately has no frozen capture baseline, so prime that owner's cache once.
        store.open(true); commit(2, cache.capture(live, Map.of())); before = blocks().deepCopy();
        change(live, 700, "B".repeat(80));
        commit(3, cache.capture(live, Map.of())); var after = blocks();
        for (int i = 0; i < 4; i++) {
            assertEquals(i == 2, !before.get(i).equals(after.get(i)));
        }
        assertEquals(1, after.asList().stream().filter(b -> b.getAsJsonObject().get("revision").getAsLong() == 3).count());
        assertEquals("B".repeat(80), value(restore(), 700));
        assertEquals("A".repeat(80), value(restore(), 1023));
    }

    @Test public void notesOnlyAndEmptyDirtyPageRoundTrip() throws Exception {
        var live = styles(512); commit(1, cache.capture(live, Map.of(300, "remove", 512, "tail A")));
        var before = blocks().deepCopy();
        commit(2, cache.capture(live, Map.of(512, "tail B")));
        assertEquals(before.get(0), blocks().get(0)); assertNotEquals(before.get(1), blocks().get(1));
        assertFalse(restore().notes.containsKey(300)); assertEquals("tail B", restore().notes.get(512));
        live.clear(); commit(3, cache.capture(live, Map.of(0, "empty tail")));
        assertEquals(1, blocks().size()); assertTrue(restore().entries.isEmpty()); assertEquals("empty tail", restore().notes.get(0));
    }

    @Test public void boundaryAppendDoesNotReviveAnOldTailAsRowComment() throws Exception {
        var live = styles(256); commit(1, cache.capture(live, Map.of(256, "tail")));
        var first = blocks().get(0).deepCopy(); live.add(new StyleModel("append", ""));
        commit(2, cache.capture(live, Map.of(256, "row note", 257, "tail")));
        assertEquals(first, blocks().get(0)); var result = restore();
        assertEquals("row note", result.notes.get(256)); assertEquals("tail", result.notes.get(257));
        live.subList(256, live.size()).clear();
        commit(3, cache.capture(live, Map.of(256, "new tail")));
        assertEquals(1, blocks().size()); assertEquals("new tail", restore().notes.get(256));
    }

    @Test public void deletionShiftAndDuplicateNamesPreserveValuesAndNotes() throws Exception {
        var live = styles(1024); live.forEach(row -> row.setStyleName("same"));
        commit(1, cache.capture(live, Map.of(700, "note", 1024, "tail")));
        change(live, 700, "B"); commit(2, cache.capture(live, Map.of(700, "note", 1024, "tail")));
        live.remove(10); commit(3, cache.capture(live, Map.of(699, "note", 1023, "tail")));
        var result = restore(); assertEquals(1023, result.entries.size()); assertEquals("B", value(result, 699));
        assertEquals("note", result.notes.get(699)); assertEquals("tail", result.notes.get(1023));
    }

    @Test public void everyWriteFailurePreservesOldManifestAndRetryUsesLatestContent() throws Exception {
        var live = styles(1024); commit(1, cache.capture(live, Map.of()));
        byte[] before = Files.readAllBytes(new File(store.directory, "manifest.json").toPath());
        for (String point : List.of("before_page", "before_encode", "after_encode", "after_write", "after_page", "before_manifest")) {
            change(live, 700, point); failure = point;
            try { commit(2, cache.capture(live, Map.of(1024, point))); fail("Expected write failure"); } catch (IOException expected) { }
            assertArrayEquals(before, Files.readAllBytes(new File(store.directory, "manifest.json").toPath()));
            failure = null; assertEquals("A".repeat(80), value(restore(), 700));
        }
        change(live, 700, "latest C"); commit(3, cache.capture(live, Map.of(1024, "tail C")));
        assertEquals("latest C", value(restore(), 700)); assertEquals("tail C", restore().notes.get(1024));
    }

    @Test public void twoDirtyPagesStillCommitAsOneAtomicGroup() throws Exception {
        var live = styles(512); var array = new ArrayModel("items", ArraysEditor.ARRAYS_TYPES.STRING,
                new LinkedHashMap<>(Map.of("item1", "A")));
        var arrays = new ResourceEditorSnapshotCache(); revisions[2] = revisions[4] = 1; dirty[2] = dirty[4] = true;
        store.commit(revisions, dirty, Map.of(2, cache.capture(live, Map.of()), 4, arrays.capture(List.of(array), Map.of())));
        change(live, 300, "B"); array.addAttribute("item1", "B"); arrays.invalidate(array);
        revisions[2] = revisions[4] = 2; failure = "before_manifest";
        try { store.commit(revisions, dirty, Map.of(2, cache.capture(live, Map.of()), 4, arrays.capture(List.of(array), Map.of()))); fail(); } catch (IOException expected) { }
        failure = null; var restored = create(null).open(true);
        assertEquals("A".repeat(80), value(restored[2], 300)); assertEquals("A", ((ArrayModel)restored[4].entries.get(0)).getAttribute("item1"));
        store.commit(revisions, dirty, Map.of(2, cache.capture(live, Map.of()), 4, arrays.capture(List.of(array), Map.of())));
        restored = create(null).open(true); assertEquals("B", value(restored[2], 300)); assertEquals("B", ((ArrayModel)restored[4].entries.get(0)).getAttribute("item1"));
    }

    @Test public void lateOwnerCannotPublishCapturedBlocks() throws Exception {
        var live = styles(512); commit(1, cache.capture(live, Map.of()));
        byte[] before = Files.readAllBytes(new File(store.directory, "manifest.json").toPath());
        var obsolete = new ResourceEditorDraftStore(root, session, "segment-project", "-fr", gate, gate.owner,
                point -> { if (point.equals("before_manifest")) gate.owner++; });
        obsolete.open(true); change(live, 300, "late"); revisions[2] = 2;
        try { obsolete.commit(revisions, dirty, Map.of(2, cache.capture(live, Map.of()))); fail(); } catch (InterruptedIOException expected) { }
        assertArrayEquals(before, Files.readAllBytes(new File(store.directory, "manifest.json").toPath()));
        assertEquals("A".repeat(80), value(restore(), 300));
    }

    @Test public void corruptOrTruncatedSegmentsRetainAllRecoveryMaterial() throws Exception {
        var live = styles(512); commit(1, cache.capture(live, Map.of()));
        File file = segment(blocks().get(1).getAsJsonObject()); byte[] original = Files.readAllBytes(file.toPath());
        byte[] damaged = original.clone(); damaged[(int)blocks().get(1).getAsJsonObject().get("offset").getAsLong() + 5] ^= 1;
        Files.write(file.toPath(), damaged); rejectedRestore();
        Files.write(file.toPath(), Arrays.copyOf(original, original.length - 1)); rejectedRestore();
    }

    @Test public void invalidRangesRevisionsAndSwappedBlockIdentityAreRejected() throws Exception {
        var live = styles(512); commit(1, cache.capture(live, Map.of())); var original = manifest();
        for (String field : List.of("offset", "length", "revision")) {
            JsonObject changed = original.deepCopy(); var block = changed.getAsJsonArray("pages").get(2).getAsJsonObject().getAsJsonArray("blocks").get(0).getAsJsonObject();
            block.addProperty(field, Long.MAX_VALUE); writeManifest(changed); rejectedRestore();
        }
        var changed = original.deepCopy(); var entries = changed.getAsJsonArray("pages").get(2).getAsJsonObject().getAsJsonArray("blocks");
        JsonElement first = entries.get(0); entries.set(0, entries.get(1)); entries.set(1, first);
        writeManifest(changed); rejectedRestore();
        changed = original.deepCopy(); changed.getAsJsonArray("pages").get(2).getAsJsonObject().addProperty("rows", 513);
        writeManifest(changed); rejectedRestore();
    }

    @Test public void missingBlockHeadersAreRejectedEvenWithAValidChecksum() throws Exception {
        commit(1, cache.capture(styles(10), Map.of()));
        var originalManifest = manifest(); var originalRef = blocks().get(0).getAsJsonObject();
        File file = segment(originalRef); var originalData = JsonParser.parseString(read(file)).getAsJsonObject();
        for (String field : List.of("schema", "session", "project", "variant", "page", "revision", "block")) {
            var data = originalData.deepCopy(); data.remove(field);
            byte[] bytes = data.toString().getBytes(StandardCharsets.UTF_8); Files.write(file.toPath(), bytes);
            var changedManifest = originalManifest.deepCopy();
            var ref = changedManifest.getAsJsonArray("pages").get(2).getAsJsonObject().getAsJsonArray("blocks").get(0).getAsJsonObject();
            ref.addProperty("length", bytes.length); ref.addProperty("checksum", sha(bytes));
            writeManifest(changedManifest); rejectedRestore();
        }
    }

    @Test public void oldJsonPagesUpgradeOnlyWhenLiveContentIsProtected() throws Exception {
        var live = styles(512); String object = UUID.randomUUID().toString();
        var data = new JsonObject(); data.addProperty("schema", 1); data.addProperty("session", session); data.addProperty("project", "segment-project"); data.addProperty("variant", "-fr"); data.addProperty("page", 2); data.addProperty("revision", 7);
        data.add("entries", new Gson().toJsonTree(live)); data.add("notes", new Gson().toJsonTree(Map.of(512, "old tail")));
        byte[] bytes = data.toString().getBytes(StandardCharsets.UTF_8); File pages = new File(store.directory, "pages"); assertTrue(pages.mkdirs());
        Files.write(new File(pages, "2-" + object + ".json").toPath(), bytes);
        var manifest = new JsonObject(); manifest.addProperty("schema", 1); manifest.addProperty("session", session); manifest.addProperty("project", "segment-project"); manifest.addProperty("variant", "-fr"); manifest.addProperty("generation", 1);
        var refs = new JsonArray(); for (int i = 0; i < 6; i++) { var ref = new JsonObject(); ref.addProperty("revision", i == 2 ? 7 : 0); ref.addProperty("dirty", i == 2); if (i == 2) {ref.addProperty("object", object); ref.addProperty("checksum", sha(bytes));} refs.add(ref); }
        manifest.add("pages", refs); writeManifest(manifest); var restored = store.open(true);
        assertEquals("old tail", restored[2].notes.get(512)); assertTrue(new File(pages, "2-" + object + ".json").exists());
        change(live, 300, "upgraded B"); commit(8, cache.capture(live, Map.of(512, "old tail")));
        assertEquals(ResourceEditorModelBlocks.FORMAT, manifest().getAsJsonArray("pages").get(2).getAsJsonObject().get("format").getAsString());
        assertFalse(new File(pages, "2-" + object + ".json").exists()); assertEquals("upgraded B", value(restore(), 300));
    }

    @Test public void xmlIntentDisablesReuseUntilFullNewSnapshotCommits() throws Exception {
        var live = styles(1024); commit(1, cache.capture(live, Map.of())); var before = blocks().deepCopy();
        store.writeXmlCommitIntent(Map.of(2, "a".repeat(64)));
        change(live, 700, "XML B"); failure = "before_manifest";
        try { commit(2, cache.capture(live, Map.of())); fail(); } catch (IOException expected) { }
        failure = null; change(live, 10, "later C"); commit(3, cache.capture(live, Map.of()));
        String packed = blocks().get(0).getAsJsonObject().get("object").getAsString();
        for (int i = 0; i < 4; i++) {assertNotEquals(before.get(i), blocks().get(i)); assertEquals(packed, blocks().get(i).getAsJsonObject().get("object").getAsString());}
        assertEquals("XML B", value(restore(), 700)); assertEquals("later C", value(restore(), 10));
    }

    @Test public void restoredXmlBAndLaterCDoNotReuseAnyOldABlock() throws Exception {
        var live = styles(1024); commit(1, cache.capture(live, Map.of()));
        File xml = new File(root, "xml-B.xml"); byte[] bytes = "XML B fingerprint".getBytes(StandardCharsets.UTF_8); Files.write(xml.toPath(), bytes);
        store.writeXmlCommitIntent(Map.of(2, sha(bytes)));
        String[] paths = new String[6]; paths[2] = xml.getAbsolutePath(); store = create(paths);
        assertNull(store.open(true)[2]); change(live, 700, "saved XML B"); change(live, 10, "later C");
        commit(2, cache.capture(live, Map.of()));
        assertEquals("saved XML B", value(restore(), 700)); assertEquals("later C", value(restore(), 10));
        assertEquals("A".repeat(80), value(restore(), 1023));
    }

    @Test public void migrationDoesNotRetainRestorationModelsAsAFrozenCapture() throws Exception {
        var drafts = new android.os.Bundle();
        drafts.putBundle("2", ResourceEditorDraftState.capture(styles(512), Map.of(512, "tail")));
        var migrated = store.importLegacy(drafts);
        var field = ResourceEditorDraftStore.class.getDeclaredField("protectedModels"); field.setAccessible(true);
        assertNull(((ResourceEditorDraftState.Snapshot[])field.get(store))[2]);
        var material = migrated.pages.get(2);
        ((StyleModel)material.entries.get(300)).addAttribute("attr", "edited after migration");
        commit(2, cache.capture(material.entries, material.notes));
        assertEquals("edited after migration", value(restore(), 300));
    }

    @Test public void invalidLegacyNotePositionsAreIsolatedInsteadOfSilentlyDropped() throws Exception {
        var drafts = new android.os.Bundle();
        drafts.putBundle("2", ResourceEditorDraftState.capture(styles(1), Map.of(2, "outside page")));
        drafts.putBundle("3", ResourceEditorDraftState.capture(styles(1), Map.of(0, "valid note")));
        var migrated = store.importLegacy(drafts);
        assertTrue(migrated.corrupt.containsKey("2")); assertNull(migrated.pages.get(2));
        assertEquals("valid note", migrated.pages.get(3).notes.get(0));
    }

    @Test public void compactionFailureKeepsOldCommitAndSuccessfulRetryBoundsWaste() throws Exception {
        var live = styles(2048); commit(1, cache.capture(live, Map.of()));
        for (int b : new int[]{0,1,2,3}) change(live, b * 256, "B".repeat(80));
        commit(2, cache.capture(live, Map.of()));
        for (int b : new int[]{0,1,4,5}) change(live, b * 256, "C".repeat(80));
        commit(3, cache.capture(live, Map.of()));
        byte[] before = Files.readAllBytes(new File(store.directory, "manifest.json").toPath());
        for (int b : new int[]{0,2,4,6}) change(live, b * 256, "D".repeat(80));
        for (String point : List.of("before_compaction", "after_compaction")) {
            failure = point;
            try {commit(4, cache.capture(live, Map.of())); fail("Expected fragmentation compaction failure");} catch (IOException expected) { }
            assertArrayEquals(before, Files.readAllBytes(new File(store.directory, "manifest.json").toPath()));
            failure = null; assertEquals("C".repeat(80), value(restore(), 0));
        }
        commit(4, cache.capture(live, Map.of()));
        assertEquals(1, Objects.requireNonNull(new File(store.directory, "pages").list()).length);
        for (int b : new int[]{0,2,4,6}) assertEquals("D".repeat(80), value(restore(), b * 256));
    }
}
